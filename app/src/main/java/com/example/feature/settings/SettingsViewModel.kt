package com.example.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.domain.port.AppPreferencesRepository
import com.example.domain.port.ClockProvider
import com.example.domain.port.ColorPaletteRepository
import com.example.domain.port.EventRepository
import com.example.domain.port.NoteRepository

class SettingsViewModel(
    colorPaletteRepository: ColorPaletteRepository,
    clock: ClockProvider,
    appPreferencesRepository: AppPreferencesRepository,
    eventRepository: EventRepository,
    noteRepository: NoteRepository,
) : ViewModel() {

    val controller = SettingsController(
        repository = colorPaletteRepository,
        clock = clock,
        scope = viewModelScope,
        appPreferencesRepository = appPreferencesRepository,
        eventRepository = eventRepository,
        noteRepository = noteRepository,
    )

    companion object {
        fun factory(
            colorPaletteRepository: ColorPaletteRepository,
            clock: ClockProvider,
            appPreferencesRepository: AppPreferencesRepository,
            eventRepository: EventRepository,
            noteRepository: NoteRepository,
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    SettingsViewModel(
                        colorPaletteRepository = colorPaletteRepository,
                        clock = clock,
                        appPreferencesRepository = appPreferencesRepository,
                        eventRepository = eventRepository,
                        noteRepository = noteRepository,
                    ) as T
            }
    }
}
