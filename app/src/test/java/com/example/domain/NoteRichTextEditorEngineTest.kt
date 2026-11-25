package com.example.domain

import com.example.data.local.NoteMapper
import com.example.domain.model.EventId
import com.example.domain.model.Note
import com.example.domain.model.NoteId
import com.example.domain.model.NoteListStyle
import com.example.domain.model.NoteRichDocument
import com.example.domain.model.NoteRichSpan
import com.example.domain.model.NoteScriptStyle
import com.example.domain.model.NoteTextFormat
import com.example.domain.model.NoteTextSize
import com.example.domain.service.NoteEditorSelection
import com.example.domain.service.NoteEditorState
import com.example.domain.service.NoteRichDocumentCodec
import com.example.domain.service.NoteRichTextEditorEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NoteRichTextEditorEngineTest {

    @Test
    fun inlineFormattingAndScriptTogglesApplyToSelectionAndTyping() {
        var state = NoteEditorState(
            document = NoteRichDocument.plain("Hello World"),
            selection = NoteEditorSelection(0, 5),
        )

        state = NoteRichTextEditorEngine.toggleBold(state)
        state = NoteRichTextEditorEngine.toggleItalic(state)
        state = NoteRichTextEditorEngine.toggleUnderline(state)
        state = NoteRichTextEditorEngine.toggleStrikethrough(state)

        val helloFormat = state.document.formatAt(2)
        assertTrue(helloFormat.bold)
        assertTrue(helloFormat.italic)
        assertTrue(helloFormat.underline)
        assertTrue(helloFormat.strikethrough)
        assertEquals(NoteTextFormat.DEFAULT, state.document.formatAt(7))

        // Toggling superscript then subscript switches mutually exclusively
        state = state.copy(selection = NoteEditorSelection(6, 11))
        state = NoteRichTextEditorEngine.toggleSuperscript(state)
        assertEquals(NoteScriptStyle.SUPERSCRIPT, state.document.formatAt(8).script)

        state = NoteRichTextEditorEngine.toggleSubscript(state)
        assertEquals(NoteScriptStyle.SUBSCRIPT, state.document.formatAt(8).script)

        // Toggling subscript again returns to normal
        state = NoteRichTextEditorEngine.toggleSubscript(state)
        assertEquals(NoteScriptStyle.NORMAL, state.document.formatAt(8).script)
    }

    @Test
    fun textSizeVariantsApplyToCurrentLineAndResetHeadingOnNewLine() {
        var state = NoteEditorState(
            document = NoteRichDocument.plain("Nagłówek"),
            selection = NoteEditorSelection(8, 8),
        )

        state = NoteRichTextEditorEngine.applyTextSize(state, NoteTextSize.HEADING_1)
        assertEquals(NoteTextSize.HEADING_1, state.document.formatAt(0).size)
        assertEquals(NoteTextSize.HEADING_1, state.activeFormat.size)

        // Pressing Enter after a heading resets active size back to BODY
        state = NoteRichTextEditorEngine.onTextChanged(
            state = state,
            newText = "Nagłówek\n",
            newSelection = NoteEditorSelection(9, 9),
        )
        assertEquals(NoteTextSize.BODY, state.activeFormat.size)

        // Typing on the new line uses BODY while keeping H1 on line 1
        state = NoteRichTextEditorEngine.onTextChanged(
            state = state,
            newText = "Nagłówek\nAkapit",
            newSelection = NoteEditorSelection(15, 15),
        )
        assertEquals(NoteTextSize.HEADING_1, state.document.formatAt(2).size)
        assertEquals(NoteTextSize.BODY, state.document.formatAt(11).size)
    }

    @Test
    fun quickListsToggleConvertAndContinueOnEnter() {
        var state = NoteEditorState(
            document = NoteRichDocument.plain("Pierwszy\nDrugi"),
            selection = NoteEditorSelection(0, 14),
        )

        state = NoteRichTextEditorEngine.toggleListStyle(state, NoteListStyle.NUMBERED)
        assertEquals("1. Pierwszy\n2. Drugi", state.document.text)

        // Switching to bullet replaces prefixes cleanly
        state = NoteRichTextEditorEngine.toggleListStyle(state, NoteListStyle.BULLET)
        assertEquals("• Pierwszy\n• Drugi", state.document.text)

        // Pressing Enter at end of bullet line auto-inserts next bullet
        val endOffset = state.document.text.length
        state = state.copy(selection = NoteEditorSelection(endOffset, endOffset))
        state = NoteRichTextEditorEngine.onTextChanged(
            state = state,
            newText = state.document.text + "\n",
            newSelection = NoteEditorSelection(endOffset + 1, endOffset + 1),
        )
        assertEquals("• Pierwszy\n• Drugi\n• ", state.document.text)
        assertEquals(NoteListStyle.BULLET, state.activeListStyle)

        // Pressing Enter on an empty bullet item exits the list
        val emptyBulletEnd = state.document.text.length
        state = NoteRichTextEditorEngine.onTextChanged(
            state = state,
            newText = state.document.text + "\n",
            newSelection = NoteEditorSelection(emptyBulletEnd + 1, emptyBulletEnd + 1),
        )
        assertEquals("• Pierwszy\n• Drugi\n", state.document.text)
        assertNull(state.activeListStyle)
    }

    @Test
    fun codecAndMapperRoundTripRichSpansAndAttachedEvents() {
        val original = Note(
            id = NoteId(7L),
            title = "Notatka bogata",
            content = "Tytuł\nH2O oraz x2",
            richSpans = listOf(
                NoteRichSpan(0, 5, NoteTextFormat(bold = true, size = NoteTextSize.HEADING_1)),
                NoteRichSpan(7, 8, NoteTextFormat(script = NoteScriptStyle.SUBSCRIPT)),
                NoteRichSpan(16, 17, NoteTextFormat(italic = true, script = NoteScriptStyle.SUPERSCRIPT)),
            ),
            attachedEventIds = listOf(EventId(3L), EventId(9L)),
            createdAtMillis = 123456L,
        )

        val encodedSpans = NoteRichDocumentCodec.encodeSpans(original.richSpans)
        val decodedSpans = NoteRichDocumentCodec.decodeSpans(encodedSpans)
        assertEquals(original.richSpans, decodedSpans)

        val mapped = NoteMapper.toDomain(NoteMapper.toEntity(original))
        assertEquals(original, mapped)

        val trimmed = NoteRichDocument(
            text = "  abc  ",
            spans = listOf(NoteRichSpan(2, 5, NoteTextFormat(bold = true))),
        ).trimmed()
        assertEquals("abc", trimmed.text)
        assertEquals(listOf(NoteRichSpan(0, 3, NoteTextFormat(bold = true))), trimmed.spans)
        assertFalse(trimmed.isBlank)
    }

    @Test
    fun toggledInlineAndScriptFormatsPersistAcrossImeSelectionSyncAndApplyToTypedText() {
        var state = NoteEditorState(
            document = NoteRichDocument.plain("H"),
            selection = NoteEditorSelection(1, 1),
        )

        // Toggle subscript and bold at collapsed cursor
        state = NoteRichTextEditorEngine.toggleSubscript(state)
        state = NoteRichTextEditorEngine.toggleBold(state)
        assertEquals(NoteScriptStyle.SUBSCRIPT, state.activeFormat.script)
        assertTrue(state.activeFormat.bold)

        // IME sends a no-op sync with same text and same cursor before inserting character
        state = NoteRichTextEditorEngine.onTextChanged(
            state = state,
            newText = "H",
            newSelection = NoteEditorSelection(1, 1),
        )
        assertEquals(NoteScriptStyle.SUBSCRIPT, state.activeFormat.script)
        assertTrue(state.activeFormat.bold)

        // Typing "2" applies subscript + bold
        state = NoteRichTextEditorEngine.onTextChanged(
            state = state,
            newText = "H2",
            newSelection = NoteEditorSelection(2, 2),
        )
        assertEquals(NoteScriptStyle.SUBSCRIPT, state.document.formatAt(1).script)
        assertTrue(state.document.formatAt(1).bold)
        assertEquals(NoteTextFormat.DEFAULT, state.document.formatAt(0))

        // Turn off subscript + bold, turn on italic + underline, simulate IME sync, type "O"
        state = NoteRichTextEditorEngine.toggleSubscript(state)
        state = NoteRichTextEditorEngine.toggleBold(state)
        state = NoteRichTextEditorEngine.toggleItalic(state)
        state = NoteRichTextEditorEngine.toggleUnderline(state)
        state = NoteRichTextEditorEngine.onTextChanged(
            state = state,
            newText = "H2",
            newSelection = NoteEditorSelection(2, 2),
        )
        state = NoteRichTextEditorEngine.onTextChanged(
            state = state,
            newText = "H2O",
            newSelection = NoteEditorSelection(3, 3),
        )
        assertTrue(state.document.formatAt(2).italic)
        assertTrue(state.document.formatAt(2).underline)
        assertEquals(NoteScriptStyle.NORMAL, state.document.formatAt(2).script)
        assertFalse(state.document.formatAt(2).bold)
    }
}
