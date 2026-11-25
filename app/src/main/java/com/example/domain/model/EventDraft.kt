package com.example.domain.model

data class EventDraft(
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
) {
    val entryType: CalendarEntryType
        get() = details.type

    fun sanitized(): EventDraft? {
        val trimmedTitle = title.trim()
        if (trimmedTitle.isEmpty()) return null

        val normalizedEndDate = if (entryType.supportsEndDate) {
            endDateKey?.takeIf { it > dateKey }
        } else {
            null
        }

        val effectiveAllDay = if (!entryType.supportsTimeRange) true else isAllDay
        val normalizedTimeRange = if (effectiveAllDay) null else timeRange?.normalized()

        return EventDraft(
            title = trimmedTitle,
            dateKey = dateKey,
            endDateKey = normalizedEndDate,
            isAllDay = effectiveAllDay,
            timeRange = normalizedTimeRange,
            description = description.trim(),
            recurrence = recurrence,
            colorTag = colorTag,
            details = EventTypeDetails.sanitize(details),
            reminderTime = reminderTime,
            location = location?.let { EventLocation(it.point, it.sanitizedLabel) },
        )
    }

    companion object {
        fun forNewEntry(
            entryType: CalendarEntryType,
            initialDateKey: DateKey,
            initialLocation: EventLocation? = null,
        ): EventDraft = EventDraft(
            title = "",
            dateKey = initialDateKey,
            endDateKey = null,
            isAllDay = entryType.defaultAllDay,
            timeRange = if (entryType.defaultAllDay) null else EventTimeRange.DEFAULT,
            description = "",
            recurrence = entryType.defaultRecurrence,
            colorTag = entryType.defaultColorTag,
            details = EventTypeDetails.defaultFor(entryType),
            reminderTime = null,
            location = initialLocation,
        )
    }
}
