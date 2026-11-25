package com.example.domain.model

import java.util.Locale

data class CalendarDay(
    val year: Int,
    val month: Int,
    val day: Int,
) {
    fun toDateKey(): DateKey = DateKey.fromParts(year, month, day)
        ?: DateKey.unsafe(String.format(Locale.US, "%04d-%02d-%02d", year, month, day))
}
