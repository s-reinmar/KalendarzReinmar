package com.example.feature.notes

import com.example.domain.model.CalendarEvent
import com.example.domain.model.EventId
import com.example.domain.model.Note

sealed interface NotesDialogState {
    data object None : NotesDialogState
    data object Create : NotesDialogState
    data class Edit(val note: Note) : NotesDialogState
    data class ConfirmDelete(val note: Note) : NotesDialogState
}

data class NotesUiState(
    val notes: List<Note> = emptyList(),
    val availableEvents: List<CalendarEvent> = emptyList(),
    val eventsById: Map<EventId, CalendarEvent> = emptyMap(),
    val dialog: NotesDialogState = NotesDialogState.None,
)
