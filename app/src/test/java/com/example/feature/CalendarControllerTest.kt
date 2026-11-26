package com.example.feature

import com.example.domain.model.CalendarEntryType
import com.example.domain.model.DateKey
import com.example.domain.model.EventColorTag
import com.example.domain.model.EventDraft
import com.example.domain.model.EventRecurrence
import com.example.domain.model.EventTimeRange
import com.example.domain.model.EventTypeDetails
import com.example.domain.model.ReminderTime
import com.example.domain.model.TaskPriority
import com.example.domain.model.TimeOfDay
import com.example.fakes.FakeClockProvider
import com.example.fakes.FakeEventRepository
import com.example.fakes.FakeReminderPort
import com.example.feature.calendar.CalendarController
import com.example.feature.calendar.CalendarDialogState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarControllerTest {

    @Test
    fun createUpdateAndDeleteEventSynchronizeRemindersAndRepository() = runTest {
        val controllerScope = CoroutineScope(coroutineContext + SupervisorJob())
        try {
            val repo = FakeEventRepository()
            val reminders = FakeReminderPort()
            val controller = CalendarController(
                eventRepository = repo,
                reminderPort = reminders,
                clock = FakeClockProvider(),
                scope = controllerScope,
            )
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                controller.uiState.collect()
            }
            advanceUntilIdle()

            val targetDate = requireNotNull(DateKey.parseOrNull("2026-11-26"))
            controller.createEvent(
                EventDraft(
                    title = "Spotkanie projektowe",
                    dateKey = targetDate,
                    isAllDay = false,
                    timeRange = EventTimeRange(
                        start = TimeOfDay.fromHourMinute(10, 0),
                        end = TimeOfDay.fromHourMinute(11, 0),
                    ),
                    description = "Agenda spotkania",
                    colorTag = EventColorTag.PINE,
                    details = EventTypeDetails.StandardEvent(guests = listOf("Anna")),
                    reminderTime = ReminderTime.fromHourMinute(10, 30),
                )
            )
            advanceUntilIdle()

            assertEquals(1, controller.uiState.value.events.size)
            assertEquals(1, reminders.scheduledEvents.size)

            val created = controller.uiState.value.events.first()
            controller.updateEvent(
                event = created,
                draft = created.toDraft().copy(
                    title = "Spotkanie bez alarmu",
                    reminderTime = null,
                ),
            )
            advanceUntilIdle()

            assertEquals("Spotkanie bez alarmu", controller.uiState.value.events.first().title)
            assertEquals(listOf(created.id), reminders.cancelledEventIds)

            controller.confirmDeleteEvent(controller.uiState.value.events.first())
            advanceUntilIdle()

            assertTrue(controller.uiState.value.events.isEmpty())
            assertEquals(2, reminders.cancelledEventIds.size)
        } finally {
            controllerScope.cancel()
        }
    }

    @Test
    fun dateSelectionTypeFilterAndTaskCompletionWorkTogether() = runTest {
        val controllerScope = CoroutineScope(coroutineContext + SupervisorJob())
        try {
            val repo = FakeEventRepository()
            val controller = CalendarController(
                eventRepository = repo,
                reminderPort = FakeReminderPort(),
                clock = FakeClockProvider(),
                scope = controllerScope,
            )
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                controller.uiState.collect()
            }
            advanceUntilIdle()

            val dayA = requireNotNull(DateKey.parseOrNull("2026-11-25"))
            val dayB = requireNotNull(DateKey.parseOrNull("2026-11-26"))
            controller.createEvent(
                EventDraft(
                    title = "Przygotować raport",
                    dateKey = dayA,
                    colorTag = EventColorTag.COBALT,
                    details = EventTypeDetails.Task(priority = TaskPriority.HIGH, isCompleted = false),
                )
            )
            controller.createEvent(
                EventDraft(
                    title = "Urodziny Marka",
                    dateKey = dayB,
                    recurrence = EventRecurrence.YEARLY,
                    colorTag = EventColorTag.AMBER,
                    details = EventTypeDetails.Birthday(celebrantName = "Marek", birthYear = 1990),
                )
            )
            advanceUntilIdle()

            // Filter by entry type
            controller.selectTypeFilter(CalendarEntryType.BIRTHDAY)
            advanceUntilIdle()
            assertEquals(1, controller.uiState.value.visibleEvents.size)
            assertEquals("Urodziny Marka", controller.uiState.value.visibleEvents.first().title)

            // Clear type filter by selecting same chip again
            controller.selectTypeFilter(CalendarEntryType.BIRTHDAY)
            advanceUntilIdle()
            assertNull(controller.uiState.value.activeTypeFilter)
            assertEquals(2, controller.uiState.value.visibleEvents.size)

            // Toggle task completion
            val task = controller.uiState.value.events.first { it.entryType == CalendarEntryType.TASK }
            assertFalse(task.isCompletedTask)
            controller.toggleTaskCompletion(task)
            advanceUntilIdle()
            assertTrue(controller.uiState.value.events.first { it.id == task.id }.isCompletedTask)

            // Date filter and toggle clear
            controller.selectDate(dayA)
            advanceUntilIdle()
            assertEquals(1, controller.uiState.value.visibleEvents.size)
            assertEquals("Przygotować raport", controller.uiState.value.visibleEvents.first().title)

            controller.selectDate(dayA)
            advanceUntilIdle()
            assertNull(controller.uiState.value.selectedDateKey)

            controller.nextMonth()
            controller.nextMonth()
            advanceUntilIdle()
            assertEquals(2027, controller.uiState.value.displayedYear)
            assertEquals(1, controller.uiState.value.displayedMonth)

            controller.openCreateEvent(entryType = CalendarEntryType.TRIP)
            advanceUntilIdle()
            val dialog = controller.uiState.value.dialog as? CalendarDialogState.CreateEvent
            assertEquals(CalendarEntryType.TRIP, dialog?.initialEntryType)
        } finally {
            controllerScope.cancel()
        }
    }

    @Test
    fun jumpToTodayRefreshTodayAndWeekStartPreferenceUpdateUiState() = runTest {
        val controllerScope = CoroutineScope(coroutineContext + SupervisorJob())
        try {
            val clock = FakeClockProvider(
                fixedDay = com.example.domain.model.CalendarDay(2026, 11, 25),
            )
            val prefsRepo = com.example.fakes.FakeAppPreferencesRepository()
            val controller = CalendarController(
                eventRepository = FakeEventRepository(),
                reminderPort = FakeReminderPort(),
                clock = clock,
                scope = controllerScope,
                appPreferencesRepository = prefsRepo,
            )
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                controller.uiState.collect()
            }
            advanceUntilIdle()

            // November 2026 starts on Sunday: Monday-first has 6 leading nulls, Sunday-first has 0
            assertTrue(controller.uiState.value.weekStartsOnMonday)
            assertEquals(6, controller.uiState.value.monthCells.takeWhile { it == null }.size)

            prefsRepo.updateWeekStartsOnMonday(false)
            advanceUntilIdle()
            assertFalse(controller.uiState.value.weekStartsOnMonday)
            assertEquals(0, controller.uiState.value.monthCells.takeWhile { it == null }.size)

            // Navigate away, advance clock past midnight, refresh and jumpToToday
            controller.nextMonth()
            advanceUntilIdle()
            assertEquals(12, controller.uiState.value.displayedMonth)

            clock.fixedDay = com.example.domain.model.CalendarDay(2026, 11, 26)
            controller.refreshToday()
            advanceUntilIdle()
            assertEquals(DateKey.parseOrNull("2026-11-26"), controller.uiState.value.todayDateKey)

            controller.jumpToToday()
            advanceUntilIdle()
            assertEquals(2026, controller.uiState.value.displayedYear)
            assertEquals(11, controller.uiState.value.displayedMonth)
            assertEquals(DateKey.parseOrNull("2026-11-26"), controller.uiState.value.selectedDateKey)
        } finally {
            controllerScope.cancel()
        }
    }
}
