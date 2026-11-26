package com.example.domain.service

import com.example.domain.model.CalendarEvent
import com.example.domain.model.CustomRecurrenceUnit
import com.example.domain.model.DateKey
import com.example.domain.model.EventRecurrence
import com.example.domain.model.ReminderTime
import com.example.domain.model.TimeOfDay
import java.util.Calendar
import java.util.TimeZone

object ReminderTriggerCalculator {
    private const val MILLIS_PER_MINUTE = 60_000L
    private const val MILLIS_PER_HOUR = 3_600_000L
    private const val MILLIS_PER_DAY = 86_400_000L
    private const val MAX_MONTH_STEPS = 240
    private const val MAX_YEAR_STEPS = 120

    fun computeTriggerMillis(
        dateKey: DateKey,
        reminderTime: ReminderTime,
        timeZone: TimeZone = TimeZone.getDefault(),
    ): Long? {
        val day = dateKey.toCalendarDay() ?: return null
        val cal = Calendar.getInstance(timeZone).apply {
            clear()
            set(Calendar.YEAR, day.year)
            set(Calendar.MONTH, day.month - 1)
            set(Calendar.DAY_OF_MONTH, day.day)
            set(Calendar.HOUR_OF_DAY, reminderTime.hour)
            set(Calendar.MINUTE, reminderTime.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    fun computeNextTriggerMillis(
        event: CalendarEvent,
        nowMillis: Long,
        timeZone: TimeZone = TimeZone.getDefault(),
    ): Long? {
        val reminderTime = event.reminderTime ?: return null
        return computeNextTriggerMillis(
            dateKey = event.dateKey,
            recurrence = event.recurrence,
            reminderTime = reminderTime,
            nowMillis = nowMillis,
            timeZone = timeZone,
        )
    }

    fun computeNextTriggerMillis(
        dateKey: DateKey,
        recurrence: EventRecurrence,
        reminderTime: ReminderTime,
        nowMillis: Long,
        timeZone: TimeZone = TimeZone.getDefault(),
    ): Long? {
        val firstTrigger = computeTriggerMillis(dateKey, reminderTime, timeZone) ?: return null
        if (firstTrigger > nowMillis) return firstTrigger

        return when (recurrence) {
            EventRecurrence.None -> null
            EventRecurrence.Daily -> computeStepDaysTrigger(
                startDateKey = dateKey,
                stepDays = 1,
                reminderTime = reminderTime,
                nowMillis = nowMillis,
                timeZone = timeZone,
            )
            EventRecurrence.Weekly -> computeStepDaysTrigger(
                startDateKey = dateKey,
                stepDays = 7,
                reminderTime = reminderTime,
                nowMillis = nowMillis,
                timeZone = timeZone,
            )
            EventRecurrence.Monthly -> computeMonthlyTrigger(
                startDateKey = dateKey,
                reminderTime = reminderTime,
                nowMillis = nowMillis,
                timeZone = timeZone,
            )
            EventRecurrence.Yearly -> computeYearlyTrigger(
                startDateKey = dateKey,
                reminderTime = reminderTime,
                nowMillis = nowMillis,
                timeZone = timeZone,
            )
            is EventRecurrence.Custom -> when (recurrence.unit) {
                CustomRecurrenceUnit.DAYS -> computeStepDaysTrigger(
                    startDateKey = dateKey,
                    stepDays = recurrence.interval,
                    reminderTime = reminderTime,
                    nowMillis = nowMillis,
                    timeZone = timeZone,
                )
                CustomRecurrenceUnit.HOURS -> computeHourlyTrigger(
                    firstTriggerMillis = firstTrigger,
                    intervalHours = recurrence.interval,
                    nowMillis = nowMillis,
                )
            }
        }
    }

    fun computeSnoozeTriggerMillis(
        nowMillis: Long,
        snoozeMinutes: Int,
    ): Long {
        val safeMinutes = snoozeMinutes.coerceIn(1, 240)
        return nowMillis + safeMinutes * MILLIS_PER_MINUTE
    }

    fun computeOffsetReminderTime(
        eventStart: TimeOfDay,
        minutesBefore: Int,
    ): ReminderTime {
        val targetMinutes = (eventStart.minutesOfDay - minutesBefore).coerceIn(0, 23 * 60 + 59)
        return ReminderTime.fromMinutes(targetMinutes)
    }

    private fun computeStepDaysTrigger(
        startDateKey: DateKey,
        stepDays: Int,
        reminderTime: ReminderTime,
        nowMillis: Long,
        timeZone: TimeZone,
    ): Long? {
        if (stepDays <= 0) return null
        val startMillis = computeTriggerMillis(startDateKey, reminderTime, timeZone) ?: return null
        val elapsedMillis = (nowMillis - startMillis).coerceAtLeast(0L)
        val approxSteps = (elapsedMillis / (stepDays.toLong() * MILLIS_PER_DAY)).toInt()
        var candidateSteps = (approxSteps - 1).coerceAtLeast(0)

        repeat(8) {
            val candidateDate = startDateKey.plusDays(candidateSteps * stepDays)
            val trigger = computeTriggerMillis(candidateDate, reminderTime, timeZone) ?: return null
            if (trigger > nowMillis) return trigger
            candidateSteps++
        }
        return null
    }

    private fun computeMonthlyTrigger(
        startDateKey: DateKey,
        reminderTime: ReminderTime,
        nowMillis: Long,
        timeZone: TimeZone,
    ): Long? {
        val startDay = startDateKey.toCalendarDay() ?: return null
        val nowCal = Calendar.getInstance(timeZone).apply { timeInMillis = nowMillis }
        val nowYear = nowCal.get(Calendar.YEAR)
        val nowMonth = nowCal.get(Calendar.MONTH) + 1
        val initialMonthOffset = ((nowYear - startDay.year) * 12 + (nowMonth - startDay.month) - 1)
            .coerceAtLeast(0)

        for (i in 0..MAX_MONTH_STEPS) {
            val totalMonths = (startDay.month - 1) + initialMonthOffset + i
            val year = startDay.year + totalMonths / 12
            val month = (totalMonths % 12) + 1
            val maxDayInMonth = maxDayOfMonth(year, month, timeZone)
            val clampedDay = startDay.day.coerceAtMost(maxDayInMonth)
            val candidateKey = DateKey.fromParts(year, month, clampedDay) ?: continue
            val trigger = computeTriggerMillis(candidateKey, reminderTime, timeZone) ?: continue
            if (trigger > nowMillis) return trigger
        }
        return null
    }

    private fun computeYearlyTrigger(
        startDateKey: DateKey,
        reminderTime: ReminderTime,
        nowMillis: Long,
        timeZone: TimeZone,
    ): Long? {
        val startDay = startDateKey.toCalendarDay() ?: return null
        val nowCal = Calendar.getInstance(timeZone).apply { timeInMillis = nowMillis }
        val startYear = maxOf(startDay.year, nowCal.get(Calendar.YEAR) - 1)

        for (offset in 0..MAX_YEAR_STEPS) {
            val year = startYear + offset
            val maxDayInMonth = maxDayOfMonth(year, startDay.month, timeZone)
            val clampedDay = startDay.day.coerceAtMost(maxDayInMonth)
            val candidateKey = DateKey.fromParts(year, startDay.month, clampedDay) ?: continue
            val trigger = computeTriggerMillis(candidateKey, reminderTime, timeZone) ?: continue
            if (trigger > nowMillis) return trigger
        }
        return null
    }

    private fun computeHourlyTrigger(
        firstTriggerMillis: Long,
        intervalHours: Int,
        nowMillis: Long,
    ): Long? {
        val stepMillis = intervalHours.toLong() * MILLIS_PER_HOUR
        if (stepMillis <= 0L) return null
        val elapsed = (nowMillis - firstTriggerMillis).coerceAtLeast(0L)
        val steps = (elapsed / stepMillis) + 1L
        return firstTriggerMillis + steps * stepMillis
    }

    private fun maxDayOfMonth(year: Int, month: Int, timeZone: TimeZone): Int {
        val cal = Calendar.getInstance(timeZone).apply {
            clear()
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        return cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    }
}
