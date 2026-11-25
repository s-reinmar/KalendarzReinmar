package com.example.data.local

import com.example.data.NoteEntity
import com.example.domain.model.Note
import com.example.domain.model.NoteId
import com.example.domain.service.NoteRichDocumentCodec

object NoteMapper {
    fun toDomain(entity: NoteEntity): Note = Note(
        id = NoteId(entity.id),
        title = entity.title,
        content = entity.content,
        richSpans = NoteRichDocumentCodec.decodeSpans(entity.richSpans),
        attachedEventIds = NoteRichDocumentCodec.decodeAttachedEventIds(entity.attachedEventIds),
        createdAtMillis = entity.createdAt,
    )

    fun toEntity(note: Note): NoteEntity = NoteEntity(
        id = note.id.value,
        title = note.title,
        content = note.content,
        richSpans = NoteRichDocumentCodec.encodeSpans(note.document.spans),
        attachedEventIds = NoteRichDocumentCodec.encodeAttachedEventIds(note.attachedEventIds),
        createdAt = note.createdAtMillis,
    )
}
