package com.example.domain.service

import com.example.domain.model.CalendarEvent
import com.example.domain.model.DateKey
import com.example.domain.model.EventColorTag
import com.example.domain.model.EventRecurrence

object EventScheduleMatcher {

    fun occursOn(event: CalendarEvent, targetDate: DateKey): Boolean {
        if (targetDate >= event.dateKey && targetDate <= event.effectiveEndDateKey) {
            return true
        }
        if (event.recurrence == EventRecurrence.YEARLY) {
            val startDay = event.dateKey.toCalendarDay() ?: return false
            val targetDay = targetDate.toCalendarDay() ?: return false
            return startDay.month == targetDay.month &&
                startDay.day == targetDay.day &&
                targetDay.year >= startDay.year
        }
        return false
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
}
