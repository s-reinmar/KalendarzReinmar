package com.example.data.local

import com.example.data.NoteDao
import com.example.data.NoteEntity
import com.example.domain.model.Note
import com.example.domain.model.NoteDraft
import com.example.domain.model.NoteId
import com.example.domain.port.ClockProvider
import com.example.domain.port.NoteRepository
import com.example.domain.service.NoteRichDocumentCodec
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomNoteRepository(
    private val noteDao: NoteDao,
    private val clock: ClockProvider,
) : NoteRepository {

    override fun observeNotes(): Flow<List<Note>> =
        noteDao.getAllNotes().map { entities -> entities.map(NoteMapper::toDomain) }

    override suspend fun addNote(draft: NoteDraft): Boolean {
        val clean = draft.sanitized() ?: return false
        noteDao.insertNote(
            NoteEntity(
                title = clean.title,
                content = clean.document.text,
                richSpans = NoteRichDocumentCodec.encodeSpans(clean.document.spans),
                attachedEventIds = NoteRichDocumentCodec.encodeAttachedEventIds(clean.attachedEventIds),
                createdAt = clock.nowMillis(),
            )
        )
        return true
    }

    override suspend fun updateNote(note: Note, draft: NoteDraft): Boolean {
        val clean = draft.sanitized() ?: return false
        noteDao.updateNote(
            NoteMapper.toEntity(
                note.copy(
                    title = clean.title,
                    content = clean.document.text,
                    richSpans = clean.document.spans,
                    attachedEventIds = clean.attachedEventIds,
                )
            )
        )
        return true
    }

    override suspend fun deleteNote(noteId: NoteId) {
        noteDao.deleteNoteById(noteId.value)
    }

    override suspend fun deleteAllNotes() {
        noteDao.deleteAllNotes()
    }

    override suspend fun addNotes(notes: List<Note>): Int {
        val valid = notes.mapNotNull { note ->
            val cleanDoc = note.document.trimmed()
            if (cleanDoc.isBlank) {
                null
            } else {
                val createdAt = if (note.createdAtMillis > 0L) note.createdAtMillis else clock.nowMillis()
                NoteEntity(
                    id = 0L,
                    title = note.title.trim(),
                    content = cleanDoc.text,
                    richSpans = NoteRichDocumentCodec.encodeSpans(cleanDoc.spans),
                    attachedEventIds = NoteRichDocumentCodec.encodeAttachedEventIds(note.attachedEventIds),
                    createdAt = createdAt,
                )
            }
        }
        if (valid.isNotEmpty()) {
            noteDao.insertNotes(valid)
        }
        return valid.size
    }
}
