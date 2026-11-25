package com.example.feature.notes

import com.example.domain.model.Note
import com.example.domain.port.NoteRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NotesController(
    private val noteRepository: NoteRepository,
    private val scope: CoroutineScope,
) {
    private val dialogState = MutableStateFlow<NotesDialogState>(NotesDialogState.None)

    val uiState: StateFlow<NotesUiState> = combine(
        noteRepository.observeNotes(),
        dialogState,
    ) { notes, dialog ->
        NotesUiState(
            notes = notes,
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

    fun createNote(title: String, content: String) {
        dialogState.value = NotesDialogState.None
        scope.launch {
            noteRepository.addNote(title = title, content = content)
        }
    }

    fun updateNote(note: Note, title: String, content: String) {
        dialogState.value = NotesDialogState.None
        scope.launch {
            noteRepository.updateNote(note = note, title = title, content = content)
        }
    }

    fun confirmDeleteNote(note: Note) {
        dialogState.value = NotesDialogState.None
        scope.launch {
            noteRepository.deleteNote(note.id)
        }
    }
}
