package com.example.feature.notes

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.example.domain.model.NoteListStyle
import com.example.domain.model.NoteRichDocument
import com.example.domain.model.NoteRichSpan
import com.example.domain.model.NoteScriptStyle
import com.example.domain.model.NoteTextFormat
import com.example.domain.model.NoteTextSize
import com.example.domain.service.NoteListPrefixParser

@Immutable
data class NoteRichTextColors(
    val textColor: Color,
    val mutedColor: Color,
    val accentColor: Color,
)

@Composable
fun rememberNoteRichTextColors(): NoteRichTextColors {
    val scheme = MaterialTheme.colorScheme
    return NoteRichTextColors(
        textColor = scheme.onSurface,
        mutedColor = scheme.onSurfaceVariant,
        accentColor = scheme.primary,
    )
}

object NoteRichTextRenderer {

    fun toAnnotatedString(
        document: NoteRichDocument,
        colors: NoteRichTextColors,
    ): AnnotatedString = buildAnnotatedString {
        val text = document.text
        append(text)
        if (text.isEmpty()) return@buildAnnotatedString

        applyListStyling(text = text, colors = colors)
        for (span in document.spans) {
            val start = span.start.coerceIn(0, text.length)
            val end = span.end.coerceIn(0, text.length)
            if (start < end && !span.format.isDefault) {
                addStyle(
                    style = toSpanStyle(span.format, colors),
                    start = start,
                    end = end,
                )
            }
        }
    }

    private fun AnnotatedString.Builder.applyListStyling(
        text: String,
        colors: NoteRichTextColors,
    ) {
        var lineStart = 0
        while (lineStart <= text.length) {
            val nextBreak = text.indexOf('\n', lineStart)
            val lineEnd = if (nextBreak < 0) text.length else nextBreak
            if (lineEnd > lineStart) {
                val line = text.substring(lineStart, lineEnd)
                val detected = NoteListPrefixParser.detect(line)
                if (detected != null) {
                    val prefixEnd = (lineStart + detected.rawPrefix.length).coerceAtMost(lineEnd)
                    addStyle(
                        style = SpanStyle(
                            color = colors.accentColor,
                            fontWeight = FontWeight.SemiBold,
                        ),
                        start = lineStart,
                        end = prefixEnd,
                    )
                    if (detected.style == NoteListStyle.CHECKLIST && detected.isChecked && prefixEnd < lineEnd) {
                        addStyle(
                            style = SpanStyle(
                                color = colors.mutedColor,
                                textDecoration = TextDecoration.LineThrough,
                            ),
                            start = prefixEnd,
                            end = lineEnd,
                        )
                    }
                }
            }
            if (nextBreak < 0) break
            lineStart = nextBreak + 1
        }
    }

    private fun toSpanStyle(
        format: NoteTextFormat,
        colors: NoteRichTextColors,
    ): SpanStyle {
        val baseSize = fontSizeFor(format.size)
        val effectiveSize = when (format.script) {
            NoteScriptStyle.NORMAL -> baseSize
            NoteScriptStyle.SUPERSCRIPT,
            NoteScriptStyle.SUBSCRIPT,
            -> (baseSize.value * 0.72f).coerceAtLeast(10f).sp
        }
        val baselineShift = when (format.script) {
            NoteScriptStyle.NORMAL -> null
            NoteScriptStyle.SUPERSCRIPT -> BaselineShift.Superscript
            NoteScriptStyle.SUBSCRIPT -> BaselineShift.Subscript
        }
        val effectiveWeight = when {
            format.bold -> FontWeight.Bold
            format.size == NoteTextSize.HEADING_1 -> FontWeight.Bold
            format.size == NoteTextSize.HEADING_2 || format.size == NoteTextSize.HEADING_3 -> FontWeight.SemiBold
            else -> FontWeight.Normal
        }
        val effectiveStyle = if (format.italic) FontStyle.Italic else FontStyle.Normal
        val decorations = buildList {
            if (format.underline) add(TextDecoration.Underline)
            if (format.strikethrough) add(TextDecoration.LineThrough)
        }
        val textDecoration = when (decorations.size) {
            0 -> null
            1 -> decorations.first()
            else -> TextDecoration.combine(decorations)
        }
        val color = if (format.size == NoteTextSize.SMALL) {
            colors.mutedColor
        } else {
            colors.textColor
        }

        return SpanStyle(
            color = color,
            fontFamily = FontFamily.SansSerif,
            fontSize = effectiveSize,
            fontWeight = effectiveWeight,
            fontStyle = effectiveStyle,
            textDecoration = textDecoration,
            baselineShift = baselineShift,
        )
    }

    private fun fontSizeFor(size: NoteTextSize): TextUnit = when (size) {
        NoteTextSize.HEADING_1 -> 23.sp
        NoteTextSize.HEADING_2 -> 19.sp
        NoteTextSize.HEADING_3 -> 16.5.sp
        NoteTextSize.BODY -> 15.sp
        NoteTextSize.SMALL -> 12.5.sp
    }
}

data class NoteRichVisualTransformation(
    private val spans: List<NoteRichSpan>,
    private val colors: NoteRichTextColors,
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val styled = NoteRichTextRenderer.toAnnotatedString(
            document = NoteRichDocument(text = text.text, spans = spans),
            colors = colors,
        )
        return TransformedText(styled, OffsetMapping.Identity)
    }
}
