package com.example.domain.service

import com.example.domain.model.EventId
import com.example.domain.model.NoteRichSpan
import com.example.domain.model.NoteScriptStyle
import com.example.domain.model.NoteTextFormat
import com.example.domain.model.NoteTextSize

object NoteRichDocumentCodec {

    fun encodeSpans(spans: List<NoteRichSpan>): String {
        if (spans.isEmpty()) return ""
        return spans.joinToString(separator = ";") { span ->
            val flags = buildString {
                if (span.format.bold) append('B')
                if (span.format.italic) append('I')
                if (span.format.underline) append('U')
                if (span.format.strikethrough) append('S')
            }.ifEmpty { "-" }
            "${span.start}:${span.end}:$flags:${span.format.size.code}:${span.format.script.code}"
        }
    }

    fun decodeSpans(raw: String?): List<NoteRichSpan> {
        if (raw.isNullOrBlank()) return emptyList()
        return raw.split(';').mapNotNull { token ->
            val parts = token.split(':')
            if (parts.size < 5) return@mapNotNull null
            val start = parts[0].toIntOrNull() ?: return@mapNotNull null
            val end = parts[1].toIntOrNull() ?: return@mapNotNull null
            if (start < 0 || end <= start) return@mapNotNull null
            val flags = parts[2]
            val format = NoteTextFormat(
                bold = 'B' in flags,
                italic = 'I' in flags,
                underline = 'U' in flags,
                strikethrough = 'S' in flags,
                size = NoteTextSize.fromCodeOrDefault(parts[3]),
                script = NoteScriptStyle.fromCodeOrDefault(parts[4]),
            )
            if (format.isDefault) null else NoteRichSpan(start, end, format)
        }
    }

    fun encodeAttachedEventIds(ids: List<EventId>): String {
        val valid = ids.filter { it.isValid }.distinct()
        if (valid.isEmpty()) return ""
        return valid.joinToString(separator = ",") { it.value.toString() }
    }

    fun decodeAttachedEventIds(raw: String?): List<EventId> {
        if (raw.isNullOrBlank()) return emptyList()
        return raw.split(',')
            .mapNotNull { part -> part.trim().toLongOrNull()?.takeIf { it > 0L } }
            .distinct()
            .map(::EventId)
    }
}
