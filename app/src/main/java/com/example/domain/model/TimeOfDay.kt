package com.example.domain.model

import java.util.Locale

@JvmInline
value class TimeOfDay private constructor(val minutesOfDay: Int) : Comparable<TimeOfDay> {

    val hour: Int
        get() = minutesOfDay / 60

    val minute: Int
        get() = minutesOfDay % 60

    override fun compareTo(other: TimeOfDay): Int = minutesOfDay.compareTo(other.minutesOfDay)

    fun plusMinutes(deltaMinutes: Int): TimeOfDay = fromMinutes(minutesOfDay + deltaMinutes)

    fun formatHHmm(): String = String.format(Locale.US, "%02d:%02d", hour, minute)

    fun toReminderTime(): ReminderTime = ReminderTime.fromMinutes(minutesOfDay)

    companion object {
        const val MIN_MINUTES = 0
        const val MAX_MINUTES = 23 * 60 + 59
        val DEFAULT_START = TimeOfDay(9 * 60)
        val DEFAULT_END = TimeOfDay(10 * 60)

        private val HHMM_REGEX = Regex("^(\\d{1,2}):(\\d{2})$")

        fun fromMinutes(minutes: Int): TimeOfDay =
            TimeOfDay(minutes.coerceIn(MIN_MINUTES, MAX_MINUTES))

        fun fromHourMinute(hour: Int, minute: Int): TimeOfDay =
            fromMinutes(hour.coerceIn(0, 23) * 60 + minute.coerceIn(0, 59))

        fun parseOrNull(raw: String?): TimeOfDay? {
            val trimmed = raw?.trim() ?: return null
            val match = HHMM_REGEX.matchEntire(trimmed) ?: return null
            val hour = match.groupValues[1].toIntOrNull() ?: return null
            val minute = match.groupValues[2].toIntOrNull() ?: return null
            if (hour !in 0..23 || minute !in 0..59) return null
            return fromHourMinute(hour, minute)
        }
    }
}
