package com.example.domain

import com.example.domain.model.CustomRecurrenceUnit
import com.example.domain.model.DateKey
import com.example.domain.model.EventRecurrence
import com.example.domain.model.ReminderTime
import com.example.domain.model.TimeOfDay
import com.example.domain.service.ReminderTriggerCalculator
import java.util.Calendar
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ReminderTriggerCalculatorTest {

    private val utc: TimeZone = TimeZone.getTimeZone("UTC")

    private fun utcMillis(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long {
        return Calendar.getInstance(utc).apply {
            clear()
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    @Test
    fun nonRecurringReturnsNullWhenInPastAndExactMillisWhenInFuture() {
        val date = requireNotNull(DateKey.parseOrNull("2026-11-25"))
        val reminder = ReminderTime.fromHourMinute(9, 0)

        val future = ReminderTriggerCalculator.computeNextTriggerMillis(
            dateKey = date,
            recurrence = EventRecurrence.None,
            reminderTime = reminder,
            nowMillis = utcMillis(2026, 11, 25, 8, 59),
            timeZone = utc,
        )
        assertEquals(utcMillis(2026, 11, 25, 9, 0), future)

        val past = ReminderTriggerCalculator.computeNextTriggerMillis(
            dateKey = date,
            recurrence = EventRecurrence.None,
            reminderTime = reminder,
            nowMillis = utcMillis(2026, 11, 25, 9, 0),
            timeZone = utc,
        )
        assertNull(past)
    }

    @Test
    fun dailyAndWeeklyRecurrenceAdvancePastAnchorDate() {
        val start = requireNotNull(DateKey.parseOrNull("2026-11-01"))
        val reminder = ReminderTime.fromHourMinute(8, 30)

        val nextDaily = ReminderTriggerCalculator.computeNextTriggerMillis(
            dateKey = start,
            recurrence = EventRecurrence.Daily,
            reminderTime = reminder,
            nowMillis = utcMillis(2026, 11, 10, 9, 0),
            timeZone = utc,
        )
        assertEquals(utcMillis(2026, 11, 11, 8, 30), nextDaily)

        val nextWeekly = ReminderTriggerCalculator.computeNextTriggerMillis(
            dateKey = start,
            recurrence = EventRecurrence.Weekly,
            reminderTime = reminder,
            nowMillis = utcMillis(2026, 11, 10, 9, 0),
            timeZone = utc,
        )
        assertEquals(utcMillis(2026, 11, 15, 8, 30), nextWeekly)
    }

    @Test
    fun monthlyRecurrenceClampsEndOfMonthForFebruary() {
        val start = requireNotNull(DateKey.parseOrNull("2026-01-31"))
        val reminder = ReminderTime.fromHourMinute(10, 0)

        val feb2026 = ReminderTriggerCalculator.computeNextTriggerMillis(
            dateKey = start,
            recurrence = EventRecurrence.Monthly,
            reminderTime = reminder,
            nowMillis = utcMillis(2026, 2, 1, 0, 0),
            timeZone = utc,
        )
        assertEquals(utcMillis(2026, 2, 28, 10, 0), feb2026)

        val feb2028Leap = ReminderTriggerCalculator.computeNextTriggerMillis(
            dateKey = start,
            recurrence = EventRecurrence.Monthly,
            reminderTime = reminder,
            nowMillis = utcMillis(2028, 2, 1, 0, 0),
            timeZone = utc,
        )
        assertEquals(utcMillis(2028, 2, 29, 10, 0), feb2028Leap)
    }

    @Test
    fun yearlyBirthdayAdvancesToUpcomingYear() {
        val start = requireNotNull(DateKey.parseOrNull("1995-05-14"))
        val reminder = ReminderTime.fromHourMinute(9, 0)

        val next = ReminderTriggerCalculator.computeNextTriggerMillis(
            dateKey = start,
            recurrence = EventRecurrence.Yearly,
            reminderTime = reminder,
            nowMillis = utcMillis(2026, 6, 1, 12, 0),
            timeZone = utc,
        )
        assertEquals(utcMillis(2027, 5, 14, 9, 0), next)
    }

    @Test
    fun customDaysAndCustomHoursComputeExpectedNextOccurrence() {
        val start = requireNotNull(DateKey.parseOrNull("2026-11-01"))
        val reminder = ReminderTime.fromHourMinute(8, 0)

        val nextEvery3Days = ReminderTriggerCalculator.computeNextTriggerMillis(
            dateKey = start,
            recurrence = EventRecurrence.Custom(3, CustomRecurrenceUnit.DAYS),
            reminderTime = reminder,
            nowMillis = utcMillis(2026, 11, 5, 0, 0),
            timeZone = utc,
        )
        // Nov 1 -> Nov 4 -> Nov 7
        assertEquals(utcMillis(2026, 11, 7, 8, 0), nextEvery3Days)

        val nextEvery4Hours = ReminderTriggerCalculator.computeNextTriggerMillis(
            dateKey = start,
            recurrence = EventRecurrence.Custom(4, CustomRecurrenceUnit.HOURS),
            reminderTime = reminder,
            nowMillis = utcMillis(2026, 11, 1, 13, 0),
            timeZone = utc,
        )
        // 08:00 -> 12:00 -> 16:00
        assertEquals(utcMillis(2026, 11, 1, 16, 0), nextEvery4Hours)
    }

    @Test
    fun snoozeAndOffsetPresetsComputeExpectedValues() {
        val now = utcMillis(2026, 11, 25, 10, 0)
        val snoozed = ReminderTriggerCalculator.computeSnoozeTriggerMillis(now, 15)
        assertEquals(utcMillis(2026, 11, 25, 10, 15), snoozed)

        val offset15 = ReminderTriggerCalculator.computeOffsetReminderTime(
            eventStart = TimeOfDay.fromHourMinute(14, 10),
            minutesBefore = 15,
        )
        assertNotNull(offset15)
        assertEquals(ReminderTime.fromHourMinute(13, 55), offset15)

        val clampedMidnight = ReminderTriggerCalculator.computeOffsetReminderTime(
            eventStart = TimeOfDay.fromHourMinute(0, 10),
            minutesBefore = 60,
        )
        assertEquals(ReminderTime.fromHourMinute(0, 0), clampedMidnight)
    }
}
