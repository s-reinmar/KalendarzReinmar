package com.example.domain.model

data class CalendarEvent(
    val id: EventId,
    val title: String,
    val dateKey: DateKey,
    val endDateKey: DateKey? = null,
    val isAllDay: Boolean = true,
    val timeRange: EventTimeRange? = null,
    val description: String = "",
    val recurrence: EventRecurrence = EventRecurrence.NONE,
    val colorTag: EventColorTag = EventColorTag.PINE,
    val details: EventTypeDetails = EventTypeDetails.StandardEvent(),
    val reminderTime: ReminderTime? = null,
    val location: EventLocation? = null,
    val createdAtMillis: Long = 0L,
) {
    val entryType: CalendarEntryType
        get() = details.type

    val hasLocation: Boolean
        get() = location != null

    val hasReminder: Boolean
        get() = reminderTime != null

    val hasDescription: Boolean
        get() = description.isNotBlank()

    val isCompletedTask: Boolean
        get() = (details as? EventTypeDetails.Task)?.isCompleted == true

    val effectiveEndDateKey: DateKey
        get() = endDateKey?.takeIf { it > dateKey } ?: dateKey

    val isMultiDay: Boolean
        get() = effectiveEndDateKey > dateKey

    fun toDraft(): EventDraft = EventDraft(
        title = title,
        dateKey = dateKey,
        endDateKey = endDateKey,
        isAllDay = isAllDay,
        timeRange = timeRange,
        description = description,
        recurrence = recurrence,
        colorTag = colorTag,
        details = details,
        reminderTime = reminderTime,
        location = location,
    )
}
