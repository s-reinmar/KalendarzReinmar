package com.example.data.local

import com.example.data.NoteDao
import com.example.data.NoteEntity
import com.example.domain.model.Note
import com.example.domain.model.NoteId
import com.example.domain.port.ClockProvider
import com.example.domain.port.NoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomNoteRepository(
    private val noteDao: NoteDao,
    private val clock: ClockProvider,
) : NoteRepository {

    override fun observeNotes(): Flow<List<Note>> =
        noteDao.getAllNotes().map { entities -> entities.map(NoteMapper::toDomain) }

    override suspend fun addNote(title: String, content: String): Boolean {
        val trimmedContent = content.trim()
        if (trimmedContent.isEmpty()) return false
        noteDao.insertNote(
            NoteEntity(
                title = title.trim(),
                content = trimmedContent,
                createdAt = clock.nowMillis(),
            )
        )
        return true
    }

    override suspend fun updateNote(note: Note, title: String, content: String): Boolean {
        val trimmedContent = content.trim()
        if (trimmedContent.isEmpty()) return false
        noteDao.updateNote(
            NoteMapper.toEntity(
                note.copy(
                    title = title.trim(),
                    content = trimmedContent,
                )
            )
        )
        return true
    }

    override suspend fun deleteNote(noteId: NoteId) {
        noteDao.deleteNoteById(noteId.value)
    }

    override suspend fun addNotes(notes: List<Note>): Int {
        val valid = notes.mapNotNull { note ->
            val trimmedContent = note.content.trim()
            if (trimmedContent.isEmpty()) {
                null
            } else {
                val createdAt = if (note.createdAtMillis > 0L) note.createdAtMillis else clock.nowMillis()
                NoteEntity(
                    id = 0L,
                    title = note.title.trim(),
                    content = trimmedContent,
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
