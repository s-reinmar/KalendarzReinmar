package com.example.data

import kotlinx.coroutines.flow.Flow

class ReinmarRepository(
    private val eventDao: CalendarEventDao,
    private val noteDao: NoteDao,
) {
    val events: Flow<List<CalendarEventEntity>> = eventDao.getAllEvents()
    val notes: Flow<List<NoteEntity>> = noteDao.getAllNotes()

    suspend fun addEvent(
        title: String,
        dateKey: String,
        reminderTimeMinutes: Int? = null,
    ): Long? {
        val trimmedTitle = title.trim()
        if (trimmedTitle.isEmpty()) return null
        return eventDao.insertEvent(
            CalendarEventEntity(
                title = trimmedTitle,
                dateKey = dateKey,
                reminderTimeMinutes = reminderTimeMinutes,
            )
        )
    }

    suspend fun updateEventReminder(eventId: Long, reminderTimeMinutes: Int?) {
        eventDao.updateEventReminder(eventId = eventId, reminderTimeMinutes = reminderTimeMinutes)
    }

    suspend fun updateEvent(
        event: CalendarEventEntity,
        title: String,
        dateKey: String,
        reminderTimeMinutes: Int?,
    ): Boolean {
        val trimmedTitle = title.trim()
        if (trimmedTitle.isEmpty()) return false
        eventDao.updateEvent(
            event.copy(
                title = trimmedTitle,
                dateKey = dateKey,
                reminderTimeMinutes = reminderTimeMinutes,
            )
        )
        return true
    }

    suspend fun deleteEvent(eventId: Long) {
        eventDao.deleteEventById(eventId)
    }

    suspend fun addNote(title: String, content: String) {
        val trimmedTitle = title.trim()
        val trimmedContent = content.trim()
        if (trimmedContent.isEmpty()) return
        noteDao.insertNote(
            NoteEntity(
                title = trimmedTitle,
                content = trimmedContent,
            )
        )
    }

    suspend fun updateNote(note: NoteEntity, title: String, content: String) {
        val trimmedContent = content.trim()
        if (trimmedContent.isEmpty()) return
        noteDao.updateNote(
            note.copy(
                title = title.trim(),
                content = trimmedContent,
            )
        )
    }

    suspend fun deleteNote(noteId: Long) {
        noteDao.deleteNoteById(noteId)
    }

    suspend fun addEvents(events: List<CalendarEventEntity>): Int {
        val valid = events.mapNotNull { event ->
            val trimmedTitle = event.title.trim()
            if (trimmedTitle.isEmpty() || event.dateKey.isBlank()) {
                null
            } else {
                event.copy(id = 0L, title = trimmedTitle)
            }
        }
        if (valid.isNotEmpty()) {
            eventDao.insertEvents(valid)
        }
        return valid.size
    }

    suspend fun addNotes(notes: List<NoteEntity>): Int {
        val valid = notes.mapNotNull { note ->
            val trimmedContent = note.content.trim()
            if (trimmedContent.isEmpty()) {
                null
            } else {
                note.copy(id = 0L, title = note.title.trim(), content = trimmedContent)
            }
        }
        if (valid.isNotEmpty()) {
            noteDao.insertNotes(valid)
        }
        return valid.size
    }
}
