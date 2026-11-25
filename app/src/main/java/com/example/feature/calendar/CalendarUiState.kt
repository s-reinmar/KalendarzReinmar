package com.example.feature.calendar

import com.example.domain.model.CalendarDay
import com.example.domain.model.CalendarEntryType
import com.example.domain.model.CalendarEvent
import com.example.domain.model.DateKey
import com.example.domain.model.EventColorTag
import com.example.domain.model.EventId
import com.example.domain.model.EventLocation
import com.example.domain.service.CalendarGridBuilder
import com.example.domain.service.EventScheduleMatcher

sealed interface CalendarDialogState {
    data object None : CalendarDialogState
    data class CreateEvent(
        val initialDateKey: DateKey,
        val initialEntryType: CalendarEntryType = CalendarEntryType.EVENT,
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
    val activeTypeFilter: CalendarEntryType? = null,
    val todayDateKey: DateKey,
    val dialog: CalendarDialogState = CalendarDialogState.None,
) {
    val monthCells: List<CalendarDay?> by lazy {
        CalendarGridBuilder.buildMonthGrid(displayedYear, displayedMonth)
    }

    val typeFilteredEvents: List<CalendarEvent> by lazy {
        if (activeTypeFilter == null) {
            events
        } else {
            events.filter { it.entryType == activeTypeFilter }
        }
    }

    val dayColorDots: Map<DateKey, List<EventColorTag>> by lazy {
        EventScheduleMatcher.collectColorDotsForMonth(
            events = typeFilteredEvents,
            year = displayedYear,
            month = displayedMonth,
        )
    }

    val eventDates: Set<DateKey>
        get() = dayColorDots.keys

    val visibleEvents: List<CalendarEvent> by lazy {
        val date = selectedDateKey
        if (date == null) {
            typeFilteredEvents
        } else {
            typeFilteredEvents.filter { EventScheduleMatcher.occursOn(it, date) }
        }
    }

    val entryTypeCounts: Map<CalendarEntryType, Int> by lazy {
        events.groupingBy { it.entryType }.eachCount()
    }

    val detailEvent: CalendarEvent?
        get() = (dialog as? CalendarDialogState.EventDetails)?.let { details ->
            events.firstOrNull { it.id == details.eventId }
        }

    val hasOpenDialog: Boolean
        get() = dialog != CalendarDialogState.None
}
