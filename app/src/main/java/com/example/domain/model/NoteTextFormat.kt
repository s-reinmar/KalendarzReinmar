package com.example.domain.model

enum class NoteTextSize(val code: String) {
    HEADING_1("H1"),
    HEADING_2("H2"),
    HEADING_3("H3"),
    BODY("BODY"),
    SMALL("SMALL"),
    ;

    val isHeading: Boolean
        get() = this == HEADING_1 || this == HEADING_2 || this == HEADING_3

    companion object {
        fun fromCodeOrDefault(code: String?): NoteTextSize =
            entries.firstOrNull { it.code.equals(code?.trim(), ignoreCase = true) } ?: BODY
    }
}

enum class NoteScriptStyle(val code: String) {
    NORMAL("N"),
    SUPERSCRIPT("SUP"),
    SUBSCRIPT("SUB"),
    ;

    companion object {
        fun fromCodeOrDefault(code: String?): NoteScriptStyle =
            entries.firstOrNull { it.code.equals(code?.trim(), ignoreCase = true) } ?: NORMAL
    }
}

enum class NoteListStyle(val code: String) {
    BULLET("BULLET"),
    NUMBERED("NUMBERED"),
    CHECKLIST("CHECKLIST"),
    LETTERED("LETTERED"),
    DASH("DASH"),
}

data class NoteTextFormat(
    val bold: Boolean = false,
    val italic: Boolean = false,
    val underline: Boolean = false,
    val strikethrough: Boolean = false,
    val size: NoteTextSize = NoteTextSize.BODY,
    val script: NoteScriptStyle = NoteScriptStyle.NORMAL,
) {
    val isDefault: Boolean
        get() = !bold &&
            !italic &&
            !underline &&
            !strikethrough &&
            size == NoteTextSize.BODY &&
            script == NoteScriptStyle.NORMAL

    fun toggleBold(): NoteTextFormat = copy(bold = !bold)

    fun toggleItalic(): NoteTextFormat = copy(italic = !italic)

    fun toggleUnderline(): NoteTextFormat = copy(underline = !underline)

    fun toggleStrikethrough(): NoteTextFormat = copy(strikethrough = !strikethrough)

    fun withSize(newSize: NoteTextSize): NoteTextFormat = copy(size = newSize)

    fun toggleSuperscript(): NoteTextFormat = copy(
        script = if (script == NoteScriptStyle.SUPERSCRIPT) {
            NoteScriptStyle.NORMAL
        } else {
            NoteScriptStyle.SUPERSCRIPT
        },
    )

    fun toggleSubscript(): NoteTextFormat = copy(
        script = if (script == NoteScriptStyle.SUBSCRIPT) {
            NoteScriptStyle.NORMAL
        } else {
            NoteScriptStyle.SUBSCRIPT
        },
    )

    fun forNewParagraph(): NoteTextFormat = if (size.isHeading) {
        copy(size = NoteTextSize.BODY)
    } else {
        this
    }

    companion object {
        val DEFAULT = NoteTextFormat()
    }
}
