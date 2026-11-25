package com.example.domain.model

data class NoteRichSpan(
    val start: Int,
    val end: Int,
    val format: NoteTextFormat,
) {
    val isEmpty: Boolean
        get() = start >= end
}

data class NoteRichDocument(
    val text: String = "",
    val spans: List<NoteRichSpan> = emptyList(),
) {
    val isBlank: Boolean
        get() = text.isBlank()

    fun formatAt(index: Int): NoteTextFormat {
        if (text.isEmpty() || spans.isEmpty()) return NoteTextFormat.DEFAULT
        val target = index.coerceIn(0, text.lastIndex)
        return spans.firstOrNull { target in it.start until it.end }?.format
            ?: NoteTextFormat.DEFAULT
    }

    fun normalized(): NoteRichDocument {
        if (text.isEmpty() || spans.isEmpty()) {
            return copy(spans = emptyList())
        }
        val length = text.length
        val valid = spans.mapNotNull { span ->
            val clampedStart = span.start.coerceIn(0, length)
            val clampedEnd = span.end.coerceIn(0, length)
            if (clampedStart >= clampedEnd || span.format.isDefault) {
                null
            } else {
                NoteRichSpan(clampedStart, clampedEnd, span.format)
            }
        }
        if (valid.isEmpty()) return copy(spans = emptyList())

        val boundaries = sortedSetOf<Int>()
        for (span in valid) {
            boundaries.add(span.start)
            boundaries.add(span.end)
        }
        val points = boundaries.toList()
        val merged = mutableListOf<NoteRichSpan>()

        for (i in 0 until points.lastIndex) {
            val segStart = points[i]
            val segEnd = points[i + 1]
            if (segStart >= segEnd) continue
            val activeFormat = valid.lastOrNull { it.start <= segStart && it.end >= segEnd }?.format
                ?: continue
            if (activeFormat.isDefault) continue

            val previous = merged.lastOrNull()
            if (previous != null && previous.end == segStart && previous.format == activeFormat) {
                merged[merged.lastIndex] = previous.copy(end = segEnd)
            } else {
                merged.add(NoteRichSpan(segStart, segEnd, activeFormat))
            }
        }
        return copy(spans = merged)
    }

    fun trimmed(): NoteRichDocument {
        if (text.isEmpty()) return NoteRichDocument()
        val firstNonWhitespace = text.indexOfFirst { !it.isWhitespace() }
        if (firstNonWhitespace < 0) return NoteRichDocument()
        val lastNonWhitespace = text.indexOfLast { !it.isWhitespace() }
        val trimmedText = text.substring(firstNonWhitespace, lastNonWhitespace + 1)
        val shiftedSpans = spans.mapNotNull { span ->
            val newStart = (span.start - firstNonWhitespace).coerceIn(0, trimmedText.length)
            val newEnd = (span.end - firstNonWhitespace).coerceIn(0, trimmedText.length)
            if (newStart >= newEnd || span.format.isDefault) {
                null
            } else {
                NoteRichSpan(newStart, newEnd, span.format)
            }
        }
        return NoteRichDocument(text = trimmedText, spans = shiftedSpans).normalized()
    }

    companion object {
        fun plain(text: String): NoteRichDocument = NoteRichDocument(text = text)
    }
}
