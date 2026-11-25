package com.example.domain.service

import com.example.domain.model.NoteListStyle

data class DetectedListPrefix(
    val style: NoteListStyle,
    val rawPrefix: String,
    val number: Int? = null,
    val letter: Char? = null,
    val isChecked: Boolean = false,
)

object NoteListPrefixParser {
    private val NUMBERED_REGEX = Regex("^(\\d+)\\.\\s")
    private val LETTERED_REGEX = Regex("^([a-z])\\)\\s")

    const val BULLET_PREFIX = "• "
    const val CHECKBOX_UNCHECKED_PREFIX = "☐ "
    const val CHECKBOX_CHECKED_PREFIX = "☑ "
    const val DASH_PREFIX = "– "

    fun detect(line: String): DetectedListPrefix? {
        if (line.startsWith(BULLET_PREFIX)) {
            return DetectedListPrefix(style = NoteListStyle.BULLET, rawPrefix = BULLET_PREFIX)
        }
        if (line.startsWith(CHECKBOX_UNCHECKED_PREFIX)) {
            return DetectedListPrefix(
                style = NoteListStyle.CHECKLIST,
                rawPrefix = CHECKBOX_UNCHECKED_PREFIX,
                isChecked = false,
            )
        }
        if (line.startsWith(CHECKBOX_CHECKED_PREFIX)) {
            return DetectedListPrefix(
                style = NoteListStyle.CHECKLIST,
                rawPrefix = CHECKBOX_CHECKED_PREFIX,
                isChecked = true,
            )
        }
        if (line.startsWith(DASH_PREFIX)) {
            return DetectedListPrefix(style = NoteListStyle.DASH, rawPrefix = DASH_PREFIX)
        }
        if (line.startsWith("- ")) {
            return DetectedListPrefix(style = NoteListStyle.DASH, rawPrefix = "- ")
        }
        val numberedMatch = NUMBERED_REGEX.find(line)
        if (numberedMatch != null) {
            return DetectedListPrefix(
                style = NoteListStyle.NUMBERED,
                rawPrefix = numberedMatch.value,
                number = numberedMatch.groupValues[1].toIntOrNull() ?: 1,
            )
        }
        val letteredMatch = LETTERED_REGEX.find(line)
        if (letteredMatch != null) {
            return DetectedListPrefix(
                style = NoteListStyle.LETTERED,
                rawPrefix = letteredMatch.value,
                letter = letteredMatch.groupValues[1].firstOrNull() ?: 'a',
            )
        }
        return null
    }

    fun prefixFor(style: NoteListStyle, itemIndex: Int): String = when (style) {
        NoteListStyle.BULLET -> BULLET_PREFIX
        NoteListStyle.NUMBERED -> "${itemIndex + 1}. "
        NoteListStyle.CHECKLIST -> CHECKBOX_UNCHECKED_PREFIX
        NoteListStyle.LETTERED -> "${('a'.code + (itemIndex % 26)).toChar()}) "
        NoteListStyle.DASH -> DASH_PREFIX
    }

    fun nextPrefixAfter(previous: DetectedListPrefix): String = when (previous.style) {
        NoteListStyle.BULLET -> BULLET_PREFIX
        NoteListStyle.NUMBERED -> "${(previous.number ?: 0) + 1}. "
        NoteListStyle.CHECKLIST -> CHECKBOX_UNCHECKED_PREFIX
        NoteListStyle.LETTERED -> {
            val currentChar = previous.letter ?: 'a'
            val nextChar = if (currentChar < 'z') (currentChar.code + 1).toChar() else 'a'
            "$nextChar) "
        }
        NoteListStyle.DASH -> DASH_PREFIX
    }
}
