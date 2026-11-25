package com.example.domain.service

import com.example.domain.model.DateKey
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PolishDateFormatter {
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

    fun formatMonthYear(year: Int, month: Int): String {
        val name = monthNamesNominative.getOrElse(month - 1) { "" }
        return "$name $year"
    }

    fun formatDateKeyDisplay(dateKey: DateKey): String {
        val day = dateKey.toCalendarDay() ?: return dateKey.value
        val monthName = monthNamesGenitive.getOrElse(day.month - 1) { "" }
        return "${day.day} $monthName ${day.year}"
    }

    fun formatTimestamp(timestampMillis: Long): String {
        val formatter = SimpleDateFormat("d MMM yyyy, HH:mm", polishLocale)
        return formatter.format(Date(timestampMillis))
    }
}
