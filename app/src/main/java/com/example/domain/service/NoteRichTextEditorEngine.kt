package com.example.domain.service

import com.example.domain.model.NoteListStyle
import com.example.domain.model.NoteRichDocument
import com.example.domain.model.NoteRichSpan
import com.example.domain.model.NoteScriptStyle
import com.example.domain.model.NoteTextFormat
import com.example.domain.model.NoteTextSize

data class NoteEditorSelection(
    val start: Int = 0,
    val end: Int = 0,
) {
    val min: Int get() = minOf(start, end)
    val max: Int get() = maxOf(start, end)
    val collapsed: Boolean get() = start == end
}

data class NoteEditorState(
    val document: NoteRichDocument = NoteRichDocument(),
    val selection: NoteEditorSelection = NoteEditorSelection(),
    val activeFormat: NoteTextFormat = NoteTextFormat.DEFAULT,
) {
    val activeListStyle: NoteListStyle?
        get() = NoteRichTextEditorEngine.detectActiveListStyle(document.text, selection.min)
}

object NoteRichTextEditorEngine {

    fun initialState(document: NoteRichDocument): NoteEditorState {
        val normalized = document.normalized()
        val cursor = normalized.text.length
        val initialFormat = if (cursor > 0) {
            normalized.formatAt(cursor - 1)
        } else {
            NoteTextFormat.DEFAULT
        }
        return NoteEditorState(
            document = normalized,
            selection = NoteEditorSelection(cursor, cursor),
            activeFormat = initialFormat,
        )
    }

    fun onTextChanged(
        state: NoteEditorState,
        newText: String,
        newSelection: NoteEditorSelection,
    ): NoteEditorState {
        val oldText = state.document.text
        val clampedSelection = NoteEditorSelection(
            start = newSelection.start.coerceIn(0, newText.length),
            end = newSelection.end.coerceIn(0, newText.length),
        )

        if (newText == oldText) {
            if (clampedSelection == state.selection) {
                return state
            }
            val formatAtCursor = formatForSelection(state.document, clampedSelection)
            return state.copy(
                selection = clampedSelection,
                activeFormat = formatAtCursor,
            )
        }

        val diff = computeTextDiff(oldText, newText)
        val insertedText = newText.substring(diff.editStart, diff.newEditEnd)

        if (diff.oldEditEnd == diff.editStart && insertedText == "\n") {
            return handleNewLineInsertion(state, diff.editStart)
        }

        val updatedSpans = applyEditToSpans(
            spans = state.document.spans,
            editStart = diff.editStart,
            oldEditEnd = diff.oldEditEnd,
            newEditEnd = diff.newEditEnd,
            insertFormat = state.activeFormat,
        )
        val updatedDoc = NoteRichDocument(text = newText, spans = updatedSpans).normalized()
        val nextFormat = if (diff.newEditEnd > diff.editStart) {
            state.activeFormat
        } else {
            formatForSelection(updatedDoc, clampedSelection)
        }

        return NoteEditorState(
            document = updatedDoc,
            selection = clampedSelection,
            activeFormat = nextFormat,
        )
    }

    fun toggleBold(state: NoteEditorState): NoteEditorState =
        toggleInlineFlag(state, { it.bold }) { format, enabled -> format.copy(bold = enabled) }

    fun toggleItalic(state: NoteEditorState): NoteEditorState =
        toggleInlineFlag(state, { it.italic }) { format, enabled -> format.copy(italic = enabled) }

    fun toggleUnderline(state: NoteEditorState): NoteEditorState =
        toggleInlineFlag(state, { it.underline }) { format, enabled -> format.copy(underline = enabled) }

    fun toggleStrikethrough(state: NoteEditorState): NoteEditorState =
        toggleInlineFlag(state, { it.strikethrough }) { format, enabled -> format.copy(strikethrough = enabled) }

    fun toggleSuperscript(state: NoteEditorState): NoteEditorState =
        toggleScript(state, NoteScriptStyle.SUPERSCRIPT)

    fun toggleSubscript(state: NoteEditorState): NoteEditorState =
        toggleScript(state, NoteScriptStyle.SUBSCRIPT)

    fun applyTextSize(state: NoteEditorState, size: NoteTextSize): NoteEditorState {
        val text = state.document.text
        if (!state.selection.collapsed) {
            val updatedDoc = transformRange(
                document = state.document,
                rangeStart = state.selection.min,
                rangeEnd = state.selection.max,
            ) { it.withSize(size) }
            return state.copy(
                document = updatedDoc,
                activeFormat = state.activeFormat.withSize(size),
            )
        }

        val (lineStart, lineEnd) = lineBoundsAt(text, state.selection.min)
        val updatedDoc = if (lineEnd > lineStart) {
            transformRange(
                document = state.document,
                rangeStart = lineStart,
                rangeEnd = lineEnd,
            ) { it.withSize(size) }
        } else {
            state.document
        }
        return state.copy(
            document = updatedDoc,
            activeFormat = state.activeFormat.withSize(size),
        )
    }

    fun clearFormatting(state: NoteEditorState): NoteEditorState {
        if (state.selection.collapsed) {
            return state.copy(activeFormat = NoteTextFormat.DEFAULT)
        }
        val updatedDoc = transformRange(
            document = state.document,
            rangeStart = state.selection.min,
            rangeEnd = state.selection.max,
        ) { NoteTextFormat.DEFAULT }
        return state.copy(
            document = updatedDoc,
            activeFormat = NoteTextFormat.DEFAULT,
        )
    }

    fun toggleListStyle(state: NoteEditorState, style: NoteListStyle): NoteEditorState {
        val text = state.document.text
        if (text.isEmpty()) {
            val prefix = NoteListPrefixParser.prefixFor(style, 0)
            return state.copy(
                document = NoteRichDocument(text = prefix, spans = emptyList()),
                selection = NoteEditorSelection(prefix.length, prefix.length),
            )
        }

        val lineRanges = collectLineRanges(text)
        val firstLineIdx = findLineIndexAt(lineRanges, state.selection.min)
        val lastLineIdx = findLineIndexAt(
            lineRanges,
            if (state.selection.collapsed) state.selection.min else (state.selection.max - 1).coerceAtLeast(state.selection.min),
        )

        val targetIndices = firstLineIdx..lastLineIdx
        val allAlreadyMatch = targetIndices.all { idx ->
            val range = lineRanges[idx]
            val lineText = text.substring(range.first, range.last + 1)
            NoteListPrefixParser.detect(lineText)?.style == style
        }

        var currentText = text
        var currentSpans = state.document.spans
        var selStart = state.selection.start
        var selEnd = state.selection.end
        var offsetShift = 0

        for ((itemOffset, lineIdx) in targetIndices.withIndex()) {
            val originalRange = lineRanges[lineIdx]
            val lineStart = originalRange.first + offsetShift
            val lineEnd = originalRange.last + 1 + offsetShift
            val lineText = currentText.substring(lineStart, lineEnd)
            val existing = NoteListPrefixParser.detect(lineText)
            val oldPrefixLen = existing?.rawPrefix?.length ?: 0
            val newPrefix = if (allAlreadyMatch) "" else NoteListPrefixParser.prefixFor(style, itemOffset)

            currentText = currentText.replaceRange(lineStart, lineStart + oldPrefixLen, newPrefix)
            currentSpans = applyEditToSpans(
                spans = currentSpans,
                editStart = lineStart,
                oldEditEnd = lineStart + oldPrefixLen,
                newEditEnd = lineStart + newPrefix.length,
                insertFormat = NoteTextFormat.DEFAULT,
            )

            val delta = newPrefix.length - oldPrefixLen
            selStart = adjustCursorForPrefixChange(selStart, lineStart, oldPrefixLen, newPrefix.length)
            selEnd = adjustCursorForPrefixChange(selEnd, lineStart, oldPrefixLen, newPrefix.length)
            offsetShift += delta
        }

        val normalizedDoc = NoteRichDocument(text = currentText, spans = currentSpans).normalized()
        val finalSelection = NoteEditorSelection(
            start = selStart.coerceIn(0, currentText.length),
            end = selEnd.coerceIn(0, currentText.length),
        )
        return state.copy(
            document = normalizedDoc,
            selection = finalSelection,
        )
    }

    fun toggleChecklistLine(document: NoteRichDocument, lineIndex: Int): NoteRichDocument {
        if (document.text.isEmpty() || lineIndex < 0) return document
        val ranges = collectLineRanges(document.text)
        if (lineIndex >= ranges.size) return document
        val range = ranges[lineIndex]
        val lineStart = range.first
        val lineText = document.text.substring(lineStart, range.last + 1)
        val detected = NoteListPrefixParser.detect(lineText) ?: return document
        if (detected.style != NoteListStyle.CHECKLIST) return document

        val replacement = if (detected.isChecked) {
            NoteListPrefixParser.CHECKBOX_UNCHECKED_PREFIX
        } else {
            NoteListPrefixParser.CHECKBOX_CHECKED_PREFIX
        }
        val updatedText = document.text.replaceRange(
            lineStart,
            lineStart + detected.rawPrefix.length,
            replacement,
        )
        return document.copy(text = updatedText).normalized()
    }

    fun detectActiveListStyle(text: String, cursor: Int): NoteListStyle? {
        if (text.isEmpty()) return null
        val (lineStart, lineEnd) = lineBoundsAt(text, cursor.coerceIn(0, text.length))
        val line = text.substring(lineStart, lineEnd)
        return NoteListPrefixParser.detect(line)?.style
    }

    private fun handleNewLineInsertion(
        state: NoteEditorState,
        insertOffset: Int,
    ): NoteEditorState {
        val oldText = state.document.text
        val lineStart = oldText.lastIndexOf('\n', (insertOffset - 1).coerceAtLeast(0))
            .let { if (insertOffset == 0 || it < 0) 0 else it + 1 }
        val currentLineBeforeCursor = oldText.substring(lineStart, insertOffset)
        val detected = NoteListPrefixParser.detect(currentLineBeforeCursor)
        val nextParagraphFormat = state.activeFormat.forNewParagraph()

        if (detected != null) {
            val contentAfterPrefix = currentLineBeforeCursor.substring(detected.rawPrefix.length)
            if (contentAfterPrefix.isBlank()) {
                val updatedText = oldText.removeRange(lineStart, insertOffset)
                val updatedSpans = applyEditToSpans(
                    spans = state.document.spans,
                    editStart = lineStart,
                    oldEditEnd = insertOffset,
                    newEditEnd = lineStart,
                    insertFormat = NoteTextFormat.DEFAULT,
                )
                return NoteEditorState(
                    document = NoteRichDocument(text = updatedText, spans = updatedSpans).normalized(),
                    selection = NoteEditorSelection(lineStart, lineStart),
                    activeFormat = nextParagraphFormat,
                )
            }

            val nextPrefix = NoteListPrefixParser.nextPrefixAfter(detected)
            val insertion = "\n$nextPrefix"
            val updatedText = oldText.substring(0, insertOffset) + insertion + oldText.substring(insertOffset)
            val updatedSpans = applyEditToSpans(
                spans = state.document.spans,
                editStart = insertOffset,
                oldEditEnd = insertOffset,
                newEditEnd = insertOffset + insertion.length,
                insertFormat = NoteTextFormat.DEFAULT,
            )
            val newCursor = insertOffset + insertion.length
            return NoteEditorState(
                document = NoteRichDocument(text = updatedText, spans = updatedSpans).normalized(),
                selection = NoteEditorSelection(newCursor, newCursor),
                activeFormat = nextParagraphFormat,
            )
        }

        val updatedText = oldText.substring(0, insertOffset) + "\n" + oldText.substring(insertOffset)
        val updatedSpans = applyEditToSpans(
            spans = state.document.spans,
            editStart = insertOffset,
            oldEditEnd = insertOffset,
            newEditEnd = insertOffset + 1,
            insertFormat = NoteTextFormat.DEFAULT,
        )
        val newCursor = insertOffset + 1
        return NoteEditorState(
            document = NoteRichDocument(text = updatedText, spans = updatedSpans).normalized(),
            selection = NoteEditorSelection(newCursor, newCursor),
            activeFormat = nextParagraphFormat,
        )
    }

    private fun toggleInlineFlag(
        state: NoteEditorState,
        getter: (NoteTextFormat) -> Boolean,
        setter: (NoteTextFormat, Boolean) -> NoteTextFormat,
    ): NoteEditorState {
        if (state.selection.collapsed) {
            val toggled = setter(state.activeFormat, !getter(state.activeFormat))
            return state.copy(activeFormat = toggled)
        }
        val rangeStart = state.selection.min
        val rangeEnd = state.selection.max
        val shouldEnable = !isEntireRangeMatching(state.document, rangeStart, rangeEnd, getter)
        val updatedDoc = transformRange(state.document, rangeStart, rangeEnd) { format ->
            setter(format, shouldEnable)
        }
        return state.copy(
            document = updatedDoc,
            activeFormat = setter(state.activeFormat, shouldEnable),
        )
    }

    private fun toggleScript(
        state: NoteEditorState,
        targetScript: NoteScriptStyle,
    ): NoteEditorState {
        if (state.selection.collapsed) {
            val toggled = if (state.activeFormat.script == targetScript) {
                NoteScriptStyle.NORMAL
            } else {
                targetScript
            }
            return state.copy(activeFormat = state.activeFormat.copy(script = toggled))
        }
        val rangeStart = state.selection.min
        val rangeEnd = state.selection.max
        val alreadyAllTarget = isEntireRangeMatching(state.document, rangeStart, rangeEnd) {
            it.script == targetScript
        }
        val nextScript = if (alreadyAllTarget) NoteScriptStyle.NORMAL else targetScript
        val updatedDoc = transformRange(state.document, rangeStart, rangeEnd) { format ->
            format.copy(script = nextScript)
        }
        return state.copy(
            document = updatedDoc,
            activeFormat = state.activeFormat.copy(script = nextScript),
        )
    }

    private fun formatForSelection(
        document: NoteRichDocument,
        selection: NoteEditorSelection,
    ): NoteTextFormat {
        if (document.text.isEmpty()) return NoteTextFormat.DEFAULT
        val probeIndex = if (selection.collapsed) {
            (selection.min - 1).coerceAtLeast(0)
        } else {
            selection.min
        }
        return document.formatAt(probeIndex)
    }

    private fun isEntireRangeMatching(
        document: NoteRichDocument,
        rangeStart: Int,
        rangeEnd: Int,
        predicate: (NoteTextFormat) -> Boolean,
    ): Boolean {
        if (rangeStart >= rangeEnd) return false
        val points = rangeBreakpoints(document.spans, rangeStart, rangeEnd)
        for (i in 0 until points.lastIndex) {
            val segStart = points[i]
            val segEnd = points[i + 1]
            if (segStart >= segEnd) continue
            val format = document.formatAt(segStart)
            if (!predicate(format)) return false
        }
        return true
    }

    private fun transformRange(
        document: NoteRichDocument,
        rangeStart: Int,
        rangeEnd: Int,
        transform: (NoteTextFormat) -> NoteTextFormat,
    ): NoteRichDocument {
        if (rangeStart >= rangeEnd || document.text.isEmpty()) return document
        val preserved = mutableListOf<NoteRichSpan>()
        for (span in document.spans) {
            if (span.start < rangeStart) {
                preserved.add(span.copy(end = minOf(span.end, rangeStart)))
            }
            if (span.end > rangeEnd) {
                preserved.add(span.copy(start = maxOf(span.start, rangeEnd)))
            }
        }
        val points = rangeBreakpoints(document.spans, rangeStart, rangeEnd)
        for (i in 0 until points.lastIndex) {
            val segStart = points[i]
            val segEnd = points[i + 1]
            if (segStart >= segEnd) continue
            val updatedFormat = transform(document.formatAt(segStart))
            if (!updatedFormat.isDefault) {
                preserved.add(NoteRichSpan(segStart, segEnd, updatedFormat))
            }
        }
        return document.copy(spans = preserved).normalized()
    }

    private fun rangeBreakpoints(
        spans: List<NoteRichSpan>,
        rangeStart: Int,
        rangeEnd: Int,
    ): List<Int> {
        val points = sortedSetOf(rangeStart, rangeEnd)
        for (span in spans) {
            if (span.start in (rangeStart + 1) until rangeEnd) points.add(span.start)
            if (span.end in (rangeStart + 1) until rangeEnd) points.add(span.end)
        }
        return points.toList()
    }

    private fun applyEditToSpans(
        spans: List<NoteRichSpan>,
        editStart: Int,
        oldEditEnd: Int,
        newEditEnd: Int,
        insertFormat: NoteTextFormat,
    ): List<NoteRichSpan> {
        val delta = newEditEnd - oldEditEnd
        val updated = mutableListOf<NoteRichSpan>()

        for (span in spans) {
            if (span.start < editStart) {
                val leftEnd = minOf(span.end, editStart)
                if (leftEnd > span.start) {
                    updated.add(span.copy(end = leftEnd))
                }
            }
            if (span.end > oldEditEnd) {
                val rightStart = maxOf(span.start, oldEditEnd) + delta
                val rightEnd = span.end + delta
                if (rightEnd > rightStart) {
                    updated.add(span.copy(start = rightStart, end = rightEnd))
                }
            }
        }

        if (newEditEnd > editStart && !insertFormat.isDefault) {
            updated.add(NoteRichSpan(editStart, newEditEnd, insertFormat))
        }
        return updated
    }

    private fun computeTextDiff(oldText: String, newText: String): TextDiff {
        val minLen = minOf(oldText.length, newText.length)
        var prefix = 0
        while (prefix < minLen && oldText[prefix] == newText[prefix]) {
            prefix++
        }
        var suffix = 0
        val maxSuffix = minLen - prefix
        while (
            suffix < maxSuffix &&
            oldText[oldText.length - 1 - suffix] == newText[newText.length - 1 - suffix]
        ) {
            suffix++
        }
        return TextDiff(
            editStart = prefix,
            oldEditEnd = oldText.length - suffix,
            newEditEnd = newText.length - suffix,
        )
    }

    private fun lineBoundsAt(text: String, cursor: Int): Pair<Int, Int> {
        if (text.isEmpty()) return 0 to 0
        val safeCursor = cursor.coerceIn(0, text.length)
        val lineStart = if (safeCursor == 0) {
            0
        } else {
            text.lastIndexOf('\n', safeCursor - 1).let { if (it < 0) 0 else it + 1 }
        }
        val lineEnd = text.indexOf('\n', safeCursor).let { if (it < 0) text.length else it }
        return lineStart to lineEnd
    }

    private fun collectLineRanges(text: String): List<IntRange> {
        val ranges = mutableListOf<IntRange>()
        var start = 0
        for (i in text.indices) {
            if (text[i] == '\n') {
                ranges.add(start until i)
                start = i + 1
            }
        }
        ranges.add(start until text.length)
        return ranges
    }

    private fun findLineIndexAt(lineRanges: List<IntRange>, offset: Int): Int {
        for (idx in lineRanges.indices) {
            val range = lineRanges[idx]
            val endExclusive = range.last + 1
            if (offset <= endExclusive) return idx
        }
        return lineRanges.lastIndex.coerceAtLeast(0)
    }

    private fun adjustCursorForPrefixChange(
        cursor: Int,
        lineStart: Int,
        oldPrefixLen: Int,
        newPrefixLen: Int,
    ): Int {
        val delta = newPrefixLen - oldPrefixLen
        return when {
            cursor < lineStart -> cursor
            cursor <= lineStart + oldPrefixLen -> lineStart + newPrefixLen
            else -> cursor + delta
        }
    }

    private data class TextDiff(
        val editStart: Int,
        val oldEditEnd: Int,
        val newEditEnd: Int,
    )
}
