package com.example.feature.calendar

import com.example.domain.model.CalendarDay
import com.example.domain.model.CalendarEvent
import com.example.domain.model.DateKey
import com.example.domain.model.EventId
import com.example.domain.model.EventLocation
import com.example.domain.service.CalendarGridBuilder

sealed interface CalendarDialogState {
    data object None : CalendarDialogState
    data class CreateEvent(
        val initialDateKey: DateKey,
        val initialLocation: EventLocation? = null,
    ) : CalendarDialogState

    data class EditEvent(val event: CalendarEvent) : CalendarDialogState
    data class EventDetails(val eventId: EventId) : CalendarDialogState
    data class ReminderEditor(val event: CalendarEvent) : CalendarDialogState
    data class QuickLocationPicker(val event: CalendarEvent) : CalendarDialogState
    data class ConfirmDelete(val event: CalendarEvent) : CalendarDialogState
}

data class CalendarUiState(
    val events: List<CalendarEvent> = emptyList(),
    val displayedYear: Int,
    val displayedMonth: Int,
    val selectedDateKey: DateKey? = null,
    val todayDateKey: DateKey,
    val dialog: CalendarDialogState = CalendarDialogState.None,
) {
    val monthCells: List<CalendarDay?> by lazy {
        CalendarGridBuilder.buildMonthGrid(displayedYear, displayedMonth)
    }

    val eventDates: Set<DateKey>
        get() = events.mapTo(HashSet()) { it.dateKey }

    val visibleEvents: List<CalendarEvent>
        get() = if (selectedDateKey == null) {
            events
        } else {
            events.filter { it.dateKey == selectedDateKey }
        }

    val detailEvent: CalendarEvent?
        get() = (dialog as? CalendarDialogState.EventDetails)?.let { details ->
            events.firstOrNull { it.id == details.eventId }
        }

    val hasOpenDialog: Boolean
        get() = dialog != CalendarDialogState.None
}
