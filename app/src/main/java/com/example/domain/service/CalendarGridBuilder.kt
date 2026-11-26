package com.example.domain.service

import com.example.domain.model.CalendarDay
import com.example.domain.model.DateKey
import java.util.Calendar
import java.util.TimeZone

data class YearMonth(
    val year: Int,
    val month: Int,
) {
    fun previous(): YearMonth = if (month == 1) {
        YearMonth(year = year - 1, month = 12)
    } else {
        YearMonth(year = year, month = month - 1)
    }

    fun next(): YearMonth = if (month == 12) {
        YearMonth(year = year + 1, month = 1)
    } else {
        YearMonth(year = year, month = month + 1)
    }
}

object CalendarGridBuilder {

    fun buildMonthGrid(
        year: Int,
        month: Int,
        weekStartsOnMonday: Boolean = true,
    ): List<CalendarDay?> {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val leadingEmpty = if (weekStartsOnMonday) {
            // Calendar.MONDAY = 2 .. Calendar.SUNDAY = 1 -> map to 0..6 (Mon..Sun)
            (firstDayOfWeek + 5) % 7
        } else {
            // Calendar.SUNDAY = 1 .. Calendar.SATURDAY = 7 -> map to 0..6 (Sun..Sat)
            (firstDayOfWeek - 1) % 7
        }

        val cells = ArrayList<CalendarDay?>(leadingEmpty + daysInMonth)
        repeat(leadingEmpty) { cells.add(null) }
        for (day in 1..daysInMonth) {
            cells.add(CalendarDay(year = year, month = month, day = day))
        }
        while (cells.size % 7 != 0) {
            cells.add(null)
        }
        return cells
    }

    fun dateKeyToUtcMillis(dateKey: DateKey, fallbackDay: CalendarDay): Long {
        val parsed = dateKey.toCalendarDay() ?: fallbackDay
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(Calendar.YEAR, parsed.year)
            set(Calendar.MONTH, parsed.month - 1)
            set(Calendar.DAY_OF_MONTH, parsed.day)
        }
        return cal.timeInMillis
    }

    fun utcMillisToDateKey(millis: Long): DateKey {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = millis
        }
        return CalendarDay(
            year = cal.get(Calendar.YEAR),
            month = cal.get(Calendar.MONTH) + 1,
            day = cal.get(Calendar.DAY_OF_MONTH),
        ).toDateKey()
    }
}
