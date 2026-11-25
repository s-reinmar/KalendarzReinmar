package com.example.feature

import com.example.domain.model.DateKey
import com.example.domain.model.ReminderTime
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
                title = "Spotkanie projektowe",
                dateKey = targetDate,
                reminderTime = ReminderTime.fromHourMinute(10, 30),
                location = null,
            )
            advanceUntilIdle()

            assertEquals(1, controller.uiState.value.events.size)
            assertEquals(1, reminders.scheduledEvents.size)

            val created = controller.uiState.value.events.first()
            controller.updateEvent(
                event = created,
                title = "Spotkanie bez alarmu",
                dateKey = targetDate,
                reminderTime = null,
                location = null,
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
    fun dateSelectionTogglesFilterAndMonthNavigationUpdatesDisplayedMonth() = runTest {
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
            controller.createEvent("A", dayA, null, null)
            controller.createEvent("B", dayB, null, null)
            advanceUntilIdle()

            controller.selectDate(dayA)
            advanceUntilIdle()
            assertEquals(1, controller.uiState.value.visibleEvents.size)
            assertEquals("A", controller.uiState.value.visibleEvents.first().title)

            // Clicking same day again clears selection
            controller.selectDate(dayA)
            advanceUntilIdle()
            assertNull(controller.uiState.value.selectedDateKey)
            assertEquals(2, controller.uiState.value.visibleEvents.size)

            controller.nextMonth()
            controller.nextMonth()
            advanceUntilIdle()
            assertEquals(2027, controller.uiState.value.displayedYear)
            assertEquals(1, controller.uiState.value.displayedMonth)

            controller.openCreateEvent()
            advanceUntilIdle()
            assertTrue(controller.uiState.value.dialog is CalendarDialogState.CreateEvent)
        } finally {
            controllerScope.cancel()
        }
    }
}
