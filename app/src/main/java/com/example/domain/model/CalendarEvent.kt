package com.example.domain.model

data class CalendarEvent(
    val id: EventId,
    val title: String,
    val dateKey: DateKey,
    val reminderTime: ReminderTime? = null,
    val location: EventLocation? = null,
    val createdAtMillis: Long = 0L,
) {
    val hasLocation: Boolean
        get() = location != null

    val hasReminder: Boolean
        get() = reminderTime != null
}
