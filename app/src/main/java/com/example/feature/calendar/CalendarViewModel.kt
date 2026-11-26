package com.example.feature.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.domain.port.AppPreferencesRepository
import com.example.domain.port.ClockProvider
import com.example.domain.port.EventRepository
import com.example.domain.port.ReminderPort

class CalendarViewModel(
    eventRepository: EventRepository,
    reminderPort: ReminderPort,
    clock: ClockProvider,
    appPreferencesRepository: AppPreferencesRepository,
) : ViewModel() {

    val controller = CalendarController(
        eventRepository = eventRepository,
        reminderPort = reminderPort,
        clock = clock,
        scope = viewModelScope,
        appPreferencesRepository = appPreferencesRepository,
    )

    companion object {
        fun factory(
            eventRepository: EventRepository,
            reminderPort: ReminderPort,
            clock: ClockProvider,
            appPreferencesRepository: AppPreferencesRepository,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                CalendarViewModel(
                    eventRepository = eventRepository,
                    reminderPort = reminderPort,
                    clock = clock,
                    appPreferencesRepository = appPreferencesRepository,
                ) as T
        }
    }
}
