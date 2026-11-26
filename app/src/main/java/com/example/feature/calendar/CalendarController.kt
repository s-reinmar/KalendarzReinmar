package com.example.feature.calendar

import com.example.domain.model.AppPreferences
import com.example.domain.model.CalendarEntryType
import com.example.domain.model.CalendarEvent
import com.example.domain.model.DateKey
import com.example.domain.model.EventDraft
import com.example.domain.model.EventId
import com.example.domain.model.EventLocation
import com.example.domain.model.EventTypeDetails
import com.example.domain.model.ReminderTime
import com.example.domain.port.AppPreferencesRepository
import com.example.domain.port.ClockProvider
import com.example.domain.port.EventRepository
import com.example.domain.port.ReminderPort
import com.example.domain.service.YearMonth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CalendarController(
    private val eventRepository: EventRepository,
    private val reminderPort: ReminderPort,
    private val clock: ClockProvider,
    private val scope: CoroutineScope,
    private val appPreferencesRepository: AppPreferencesRepository? = null,
) {
    private val initialToday = clock.today()
    private val todayFlow = MutableStateFlow(initialToday)

    private val displayedMonth = MutableStateFlow(
        YearMonth(year = initialToday.year, month = initialToday.month),
    )
    private val selectedDateKey = MutableStateFlow<DateKey?>(null)
    private val activeTypeFilter = MutableStateFlow<CalendarEntryType?>(null)
    private val dialogState = MutableStateFlow<CalendarDialogState>(CalendarDialogState.None)

    private val preferencesFlow = appPreferencesRepository?.observePreferences()
        ?: flowOf(AppPreferences())

    private val navigationState = combine(
        displayedMonth,
        selectedDateKey,
        activeTypeFilter,
        dialogState,
    ) { month, selectedDate, typeFilter, dialog ->
        CalendarNavigationSnapshot(
            month = month,
            selectedDateKey = selectedDate,
            activeTypeFilter = typeFilter,
            dialog = dialog,
        )
    }

    val uiState: StateFlow<CalendarUiState> = combine(
        eventRepository.observeEvents(),
        navigationState,
        todayFlow,
        preferencesFlow,
    ) { events, nav, today, preferences ->
        CalendarUiState(
            events = events,
            displayedYear = nav.month.year,
            displayedMonth = nav.month.month,
            selectedDateKey = nav.selectedDateKey,
            activeTypeFilter = nav.activeTypeFilter,
            todayDateKey = today.toDateKey(),
            weekStartsOnMonday = preferences.weekStartsOnMonday,
            defaultReminderTime = preferences.defaultReminderTime,
            dialog = nav.dialog,
        )
    }.stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = CalendarUiState(
            displayedYear = initialToday.year,
            displayedMonth = initialToday.month,
            todayDateKey = initialToday.toDateKey(),
        ),
    )

    fun refreshToday() {
        val current = clock.today()
        if (todayFlow.value != current) {
            todayFlow.value = current
        }
    }

    fun jumpToToday() {
        val current = clock.today()
        todayFlow.value = current
        displayedMonth.value = YearMonth(year = current.year, month = current.month)
        selectedDateKey.value = current.toDateKey()
    }

    fun selectDate(dateKey: DateKey?) {
        selectedDateKey.update { current ->
            if (dateKey != null && current == dateKey) null else dateKey
        }
    }

    fun selectTypeFilter(type: CalendarEntryType?) {
        activeTypeFilter.update { current ->
            if (type != null && current == type) null else type
        }
    }

    fun previousMonth() {
        displayedMonth.update(YearMonth::previous)
    }

    fun nextMonth() {
        displayedMonth.update(YearMonth::next)
    }

    fun openCreateEvent(
        entryType: CalendarEntryType = CalendarEntryType.EVENT,
        initialLocation: EventLocation? = null,
    ) {
        val targetDate = selectedDateKey.value ?: todayFlow.value.toDateKey()
        dialogState.value = CalendarDialogState.CreateEvent(
            initialDateKey = targetDate,
            initialEntryType = entryType,
            initialLocation = initialLocation,
        )
    }

    private data class CalendarNavigationSnapshot(
        val month: YearMonth,
        val selectedDateKey: DateKey?,
        val activeTypeFilter: CalendarEntryType?,
        val dialog: CalendarDialogState,
    )

    fun openEventDetails(eventId: EventId) {
        if (!eventId.isValid) return
        dialogState.value = CalendarDialogState.EventDetails(eventId)
    }

    fun openLocationPinAction(event: CalendarEvent) {
        dialogState.value = if (event.hasLocation) {
            CalendarDialogState.EventDetails(event.id)
        } else {
            CalendarDialogState.QuickLocationPicker(event)
        }
    }

    fun openEditEvent(event: CalendarEvent) {
        dialogState.value = CalendarDialogState.EditEvent(event)
    }

    fun openReminderEditor(event: CalendarEvent) {
        dialogState.value = CalendarDialogState.ReminderEditor(event)
    }

    fun requestDeleteEvent(event: CalendarEvent) {
        dialogState.value = CalendarDialogState.ConfirmDelete(event)
    }

    fun dismissDialog() {
        dialogState.value = CalendarDialogState.None
    }

    fun createEvent(draft: EventDraft) {
        dialogState.value = CalendarDialogState.None
        scope.launch {
            val created = eventRepository.addEvent(draft) ?: return@launch
            if (created.hasReminder) {
                reminderPort.schedule(created)
            }
        }
    }

    fun updateEvent(
        event: CalendarEvent,
        draft: EventDraft,
    ) {
        dialogState.value = CalendarDialogState.None
        scope.launch {
            val updated = eventRepository.updateEvent(event = event, draft = draft) ?: return@launch
            if (updated.hasReminder) {
                reminderPort.schedule(updated)
            } else {
                reminderPort.cancel(updated.id)
            }
        }
    }

    fun toggleTaskCompletion(event: CalendarEvent) {
        val taskDetails = event.details as? EventTypeDetails.Task ?: return
        val nextCompleted = !taskDetails.isCompleted
        scope.launch {
            eventRepository.setTaskCompleted(
                eventId = event.id,
                completed = nextCompleted,
            )
        }
    }

    fun updateEventReminderAndLocation(
        event: CalendarEvent,
        reminderTime: ReminderTime?,
        location: EventLocation?,
    ) {
        dialogState.value = CalendarDialogState.None
        scope.launch {
            eventRepository.updateEventReminder(
                eventId = event.id,
                reminderTime = reminderTime,
                location = location,
            )
            val updated = event.copy(
                reminderTime = reminderTime,
                location = location,
            )
            if (updated.hasReminder) {
                reminderPort.schedule(updated)
            } else {
                reminderPort.cancel(updated.id)
            }
        }
    }

    fun confirmDeleteEvent(event: CalendarEvent) {
        dialogState.value = CalendarDialogState.None
        scope.launch {
            reminderPort.cancel(event.id)
            eventRepository.deleteEvent(event.id)
        }
    }
}
