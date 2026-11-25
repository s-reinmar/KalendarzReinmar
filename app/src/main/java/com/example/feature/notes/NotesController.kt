package com.example.feature.notes

import com.example.domain.model.Note
import com.example.domain.model.NoteDraft
import com.example.domain.port.EventRepository
import com.example.domain.port.NoteRepository
import com.example.domain.service.NoteRichTextEditorEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NotesController(
    private val noteRepository: NoteRepository,
    eventRepository: EventRepository,
    private val scope: CoroutineScope,
) {
    private val dialogState = MutableStateFlow<NotesDialogState>(NotesDialogState.None)

    val uiState: StateFlow<NotesUiState> = combine(
        noteRepository.observeNotes(),
        eventRepository.observeEvents(),
        dialogState,
    ) { notes, events, dialog ->
        val sortedEvents = events.sortedWith(
            compareBy<com.example.domain.model.CalendarEvent> { it.dateKey.value }
                .thenBy { it.timeRange?.start?.minutesOfDay ?: Int.MAX_VALUE }
                .thenBy { it.title.lowercase() }
        )
        NotesUiState(
            notes = notes,
            availableEvents = sortedEvents,
            eventsById = sortedEvents.associateBy { it.id },
            dialog = dialog,
        )
    }.stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = NotesUiState(),
    )

    fun openCreateNote() {
        dialogState.value = NotesDialogState.Create
    }

    fun openEditNote(note: Note) {
        dialogState.value = NotesDialogState.Edit(note)
    }

    fun requestDeleteNote(note: Note) {
        dialogState.value = NotesDialogState.ConfirmDelete(note)
    }

    fun dismissDialog() {
        dialogState.value = NotesDialogState.None
    }

    fun createNote(draft: NoteDraft) {
        dialogState.value = NotesDialogState.None
        scope.launch {
            noteRepository.addNote(draft)
        }
    }

    fun updateNote(note: Note, draft: NoteDraft) {
        dialogState.value = NotesDialogState.None
        scope.launch {
            noteRepository.updateNote(note = note, draft = draft)
        }
    }

    fun toggleChecklistLine(note: Note, lineIndex: Int) {
        val updatedDoc = NoteRichTextEditorEngine.toggleChecklistLine(note.document, lineIndex)
        if (updatedDoc == note.document) return
        scope.launch {
            noteRepository.updateNote(
                note = note,
                draft = note.toDraft().copy(document = updatedDoc),
            )
        }
    }

    fun confirmDeleteNote(note: Note) {
        dialogState.value = NotesDialogState.None
        scope.launch {
            noteRepository.deleteNote(note.id)
        }
    }
}
