package com.example.domain.model

import java.util.Locale

@JvmInline
value class ReminderTime private constructor(val minutesOfDay: Int) {

    val hour: Int
        get() = minutesOfDay / 60

    val minute: Int
        get() = minutesOfDay % 60

    fun formatHHmm(): String = String.format(Locale.US, "%02d:%02d", hour, minute)

    companion object {
        const val MIN_MINUTES = 0
        const val MAX_MINUTES = 23 * 60 + 59
        val DEFAULT = ReminderTime(9 * 60)

        fun fromMinutes(minutes: Int): ReminderTime =
            ReminderTime(minutes.coerceIn(MIN_MINUTES, MAX_MINUTES))

        fun fromHourMinute(hour: Int, minute: Int): ReminderTime =
            fromMinutes(hour.coerceIn(0, 23) * 60 + minute.coerceIn(0, 59))
    }
}
