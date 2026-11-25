package com.example.domain.model

data class Note(
    val id: NoteId,
    val title: String,
    val content: String,
    val richSpans: List<NoteRichSpan> = emptyList(),
    val attachedEventIds: List<EventId> = emptyList(),
    val createdAtMillis: Long = 0L,
) {
    val document: NoteRichDocument
        get() = NoteRichDocument(text = content, spans = richSpans).normalized()

    val hasFormatting: Boolean
        get() = richSpans.isNotEmpty()

    val hasAttachedEvents: Boolean
        get() = attachedEventIds.isNotEmpty()

    fun toDraft(): NoteDraft = NoteDraft(
        title = title,
        document = document,
        attachedEventIds = attachedEventIds,
    )
}
