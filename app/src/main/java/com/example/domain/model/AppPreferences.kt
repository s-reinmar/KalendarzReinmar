package com.example.domain.model

data class AppPreferences(
    val defaultReminderTime: ReminderTime = ReminderTime.DEFAULT,
    val defaultSnoozeMinutes: Int = DEFAULT_SNOOZE_MINUTES,
    val weekStartsOnMonday: Boolean = true,
) {
    fun normalized(): AppPreferences = copy(
        defaultSnoozeMinutes = normalizeSnoozeMinutes(defaultSnoozeMinutes),
    )

    companion object {
        const val DEFAULT_SNOOZE_MINUTES: Int = 15
        val SUPPORTED_SNOOZE_OPTIONS: List<Int> = listOf(5, 10, 15, 30)

        fun normalizeSnoozeMinutes(raw: Int): Int =
            if (raw in SUPPORTED_SNOOZE_OPTIONS) raw else DEFAULT_SNOOZE_MINUTES
    }
}
