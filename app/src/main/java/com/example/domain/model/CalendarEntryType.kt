package com.example.domain.model

import java.util.Locale

enum class CalendarEntryType(
    val code: String,
    val defaultAllDay: Boolean,
    val defaultRecurrence: EventRecurrence,
    val defaultColorTag: EventColorTag,
    val supportsTimeRange: Boolean,
    val supportsEndDate: Boolean,
) {
    EVENT(
        code = "EVENT",
        defaultAllDay = false,
        defaultRecurrence = EventRecurrence.NONE,
        defaultColorTag = EventColorTag.PINE,
        supportsTimeRange = true,
        supportsEndDate = true,
    ),
    TASK(
        code = "TASK",
        defaultAllDay = true,
        defaultRecurrence = EventRecurrence.NONE,
        defaultColorTag = EventColorTag.COBALT,
        supportsTimeRange = true,
        supportsEndDate = false,
    ),
    BIRTHDAY(
        code = "BIRTHDAY",
        defaultAllDay = true,
        defaultRecurrence = EventRecurrence.YEARLY,
        defaultColorTag = EventColorTag.AMBER,
        supportsTimeRange = false,
        supportsEndDate = false,
    ),
    APPOINTMENT(
        code = "APPOINTMENT",
        defaultAllDay = false,
        defaultRecurrence = EventRecurrence.NONE,
        defaultColorTag = EventColorTag.TERRACOTTA,
        supportsTimeRange = true,
        supportsEndDate = false,
    ),
    TRIP(
        code = "TRIP",
        defaultAllDay = true,
        defaultRecurrence = EventRecurrence.NONE,
        defaultColorTag = EventColorTag.PLUM,
        supportsTimeRange = true,
        supportsEndDate = true,
    ),
    ;

    companion object {
        fun fromCodeOrDefault(raw: String?): CalendarEntryType {
            val normalized = raw?.trim()?.uppercase(Locale.US) ?: return EVENT
            return when (normalized) {
                "EVENT", "WYDARZENIE" -> EVENT
                "TASK", "ZADANIE" -> TASK
                "BIRTHDAY", "URODZINY" -> BIRTHDAY
                "APPOINTMENT", "WIZYTA" -> APPOINTMENT
                "TRIP", "PODROZ", "PODRÓŻ", "WYJAZD" -> TRIP
                else -> EVENT
            }
        }
    }
}
