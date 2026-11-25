package com.example.data.local

import com.example.data.NoteEntity
import com.example.domain.model.Note
import com.example.domain.model.NoteId

object NoteMapper {
    fun toDomain(entity: NoteEntity): Note = Note(
        id = NoteId(entity.id),
        title = entity.title,
        content = entity.content,
        createdAtMillis = entity.createdAt,
    )

    fun toEntity(note: Note): NoteEntity = NoteEntity(
        id = note.id.value,
        title = note.title,
        content = note.content,
        createdAt = note.createdAtMillis,
    )
}
