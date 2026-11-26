package com.example.domain.service

import com.example.domain.model.CalendarEvent
import com.example.domain.model.CustomRecurrenceUnit
import com.example.domain.model.DateKey
import com.example.domain.model.EventColorTag
import com.example.domain.model.EventRecurrence

object EventScheduleMatcher {

    private const val MINUTES_PER_DAY = 24L * 60L

    fun occursOn(event: CalendarEvent, targetDate: DateKey): Boolean {
        if (targetDate < event.dateKey) return false
        if (targetDate <= event.effectiveEndDateKey) return true

        val daysBetween = event.dateKey.daysUntil(targetDate) ?: return false
        if (daysBetween < 0L) return false
        val spanDays = (event.dateKey.daysUntil(event.effectiveEndDateKey) ?: 0L).coerceAtLeast(0L)

        return when (val recurrence = event.recurrence) {
            EventRecurrence.None -> false
            EventRecurrence.Daily -> true
            EventRecurrence.Weekly -> (daysBetween % 7L) <= spanDays
            EventRecurrence.Monthly -> matchesMonthly(event, targetDate)
            EventRecurrence.Yearly -> matchesYearly(event, targetDate)
            is EventRecurrence.Custom -> when (recurrence.unit) {
                CustomRecurrenceUnit.DAYS -> (daysBetween % recurrence.interval.toLong()) <= spanDays
                CustomRecurrenceUnit.HOURS -> matchesHourlyInterval(event, daysBetween, recurrence.interval)
            }
        }
    }

    fun collectColorDotsForMonth(
        events: List<CalendarEvent>,
        year: Int,
        month: Int,
        maxDotsPerDay: Int = 3,
    ): Map<DateKey, List<EventColorTag>> {
        val cells = CalendarGridBuilder.buildMonthGrid(year, month).filterNotNull()
        val result = HashMap<DateKey, List<EventColorTag>>(cells.size)

        for (day in cells) {
            val dayKey = day.toDateKey()
            val tags = events
                .asSequence()
                .filter { occursOn(it, dayKey) }
                .map { it.colorTag }
                .distinct()
                .take(maxDotsPerDay)
                .toList()
            if (tags.isNotEmpty()) {
                result[dayKey] = tags
            }
        }
        return result
    }

    private fun matchesMonthly(event: CalendarEvent, targetDate: DateKey): Boolean {
        val startDay = event.dateKey.toCalendarDay() ?: return false
        val targetDay = targetDate.toCalendarDay() ?: return false
        return targetDay.day == startDay.day && targetDate >= event.dateKey
    }

    private fun matchesYearly(event: CalendarEvent, targetDate: DateKey): Boolean {
        val startDay = event.dateKey.toCalendarDay() ?: return false
        val targetDay = targetDate.toCalendarDay() ?: return false
        return startDay.month == targetDay.month &&
            startDay.day == targetDay.day &&
            targetDay.year >= startDay.year
    }

    private fun matchesHourlyInterval(
        event: CalendarEvent,
        daysBetween: Long,
        intervalHours: Int,
    ): Boolean {
        val stepMinutes = intervalHours.toLong() * 60L
        if (stepMinutes <= 0L) return false
        val startMinuteOfDay = if (event.isAllDay) {
            0L
        } else {
            event.timeRange?.start?.minutesOfDay?.toLong() ?: 0L
        }
        val targetDayStartMin = daysBetween * MINUTES_PER_DAY
        val targetDayEndMin = (daysBetween + 1L) * MINUTES_PER_DAY
        val minOffset = (targetDayStartMin - startMinuteOfDay).coerceAtLeast(0L)
        val firstStepIndex = (minOffset + stepMinutes - 1L) / stepMinutes
        val nextOccurrenceMin = startMinuteOfDay + firstStepIndex * stepMinutes
        return nextOccurrenceMin < targetDayEndMin
    }
}
