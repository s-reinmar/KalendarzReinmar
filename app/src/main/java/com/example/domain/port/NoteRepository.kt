package com.example.domain.port

import com.example.domain.model.Note
import com.example.domain.model.NoteId
import kotlinx.coroutines.flow.Flow

interface NoteRepository {
    fun observeNotes(): Flow<List<Note>>

    suspend fun addNote(title: String, content: String): Boolean

    suspend fun updateNote(note: Note, title: String, content: String): Boolean

    suspend fun deleteNote(noteId: NoteId)

    suspend fun addNotes(notes: List<Note>): Int
}
