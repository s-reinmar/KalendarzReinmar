package com.example.domain.model

data class NoteDraft(
    val title: String = "",
    val document: NoteRichDocument = NoteRichDocument(),
    val attachedEventIds: List<EventId> = emptyList(),
) {
    fun sanitized(): NoteDraft? {
        val cleanDoc = document.trimmed()
        if (cleanDoc.isBlank) return null
        val uniqueEventIds = attachedEventIds
            .filter { it.isValid }
            .distinct()
        return NoteDraft(
            title = title.trim(),
            document = cleanDoc,
            attachedEventIds = uniqueEventIds,
        )
    }

    companion object {
        fun plain(
            title: String,
            content: String,
            attachedEventIds: List<EventId> = emptyList(),
        ): NoteDraft = NoteDraft(
            title = title,
            document = NoteRichDocument.plain(content),
            attachedEventIds = attachedEventIds,
        )
    }
}
