package com.example.ui

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class CalendarDay(
    val year: Int,
    val month: Int, // 1..12
    val day: Int,   // 1..31
) {
    val dateKey: String
        get() = String.format(Locale.US, "%04d-%02d-%02d", year, month, day)
}

object DateUtils {
    private val polishLocale = Locale.forLanguageTag("pl-PL")

    val weekdayHeaders: List<String> = listOf("Pn", "Wt", "Śr", "Cz", "Pt", "So", "Nd")

    private val monthNamesNominative: List<String> = listOf(
        "Styczeń", "Luty", "Marzec", "Kwiecień", "Maj", "Czerwiec",
        "Lipiec", "Sierpień", "Wrzesień", "Październik", "Listopad", "Grudzień",
    )

    private val monthNamesGenitive: List<String> = listOf(
        "stycznia", "lutego", "marca", "kwietnia", "maja", "czerwca",
        "lipca", "sierpnia", "września", "października", "listopada", "grudnia",
    )

    fun today(): CalendarDay {
        val cal = Calendar.getInstance()
        return CalendarDay(
            year = cal.get(Calendar.YEAR),
            month = cal.get(Calendar.MONTH) + 1,
            day = cal.get(Calendar.DAY_OF_MONTH),
        )
    }

    fun formatMonthYear(year: Int, month: Int): String {
        val name = monthNamesNominative.getOrElse(month - 1) { "" }
        return "$name $year"
    }

    fun parseDateKey(dateKey: String): CalendarDay? {
        val parts = dateKey.split("-")
        if (parts.size != 3) return null
        val y = parts[0].toIntOrNull() ?: return null
        val m = parts[1].toIntOrNull() ?: return null
        val d = parts[2].toIntOrNull() ?: return null
        return CalendarDay(y, m, d)
    }

    fun formatDateKeyDisplay(dateKey: String): String {
        val day = parseDateKey(dateKey) ?: return dateKey
        val monthName = monthNamesGenitive.getOrElse(day.month - 1) { "" }
        return "${day.day} $monthName ${day.year}"
    }

    fun formatTimestamp(timestampMillis: Long): String {
        val formatter = SimpleDateFormat("d MMM yyyy, HH:mm", polishLocale)
        return formatter.format(Date(timestampMillis))
    }

    fun buildMonthGrid(year: Int, month: Int): List<CalendarDay?> {
        val cal = Calendar.getInstance().apply {
            clear()
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        // Calendar.MONDAY = 2 .. Calendar.SUNDAY = 1 -> convert to 0..6 (Mon..Sun)
        val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val leadingEmpty = (firstDayOfWeek + 5) % 7

        val cells = ArrayList<CalendarDay?>(leadingEmpty + daysInMonth)
        repeat(leadingEmpty) { cells.add(null) }
        for (d in 1..daysInMonth) {
            cells.add(CalendarDay(year, month, d))
        }
        while (cells.size % 7 != 0) {
            cells.add(null)
        }
        return cells
    }

    fun dateKeyToUtcMillis(dateKey: String): Long {
        val parsed = parseDateKey(dateKey) ?: today()
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(Calendar.YEAR, parsed.year)
            set(Calendar.MONTH, parsed.month - 1)
            set(Calendar.DAY_OF_MONTH, parsed.day)
        }
        return cal.timeInMillis
    }

    fun utcMillisToDateKey(millis: Long): String {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = millis
        }
        return CalendarDay(
            year = cal.get(Calendar.YEAR),
            month = cal.get(Calendar.MONTH) + 1,
            day = cal.get(Calendar.DAY_OF_MONTH),
        ).dateKey
    }
}
