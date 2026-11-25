package com.example.feature.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.domain.port.EventRepository
import com.example.domain.port.NoteRepository

class NotesViewModel(
    noteRepository: NoteRepository,
    eventRepository: EventRepository,
) : ViewModel() {

    val controller = NotesController(
        noteRepository = noteRepository,
        eventRepository = eventRepository,
        scope = viewModelScope,
    )

    companion object {
        fun factory(
            noteRepository: NoteRepository,
            eventRepository: EventRepository,
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    NotesViewModel(noteRepository, eventRepository) as T
            }
    }
}
