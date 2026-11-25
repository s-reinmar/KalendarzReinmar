package com.example.domain.model

data class EventTimeRange(
    val start: TimeOfDay,
    val end: TimeOfDay? = null,
) {
    fun normalized(): EventTimeRange {
        val validEnd = end?.takeIf { it > start }
        return EventTimeRange(start = start, end = validEnd)
    }

    fun formatDisplay(): String = if (end != null && end > start) {
        "${start.formatHHmm()} – ${end.formatHHmm()}"
    } else {
        start.formatHHmm()
    }

    companion object {
        val DEFAULT = EventTimeRange(
            start = TimeOfDay.DEFAULT_START,
            end = TimeOfDay.DEFAULT_END,
        )

        fun fromRawMinutes(startMinutes: Int?, endMinutes: Int?): EventTimeRange? {
            if (startMinutes == null) return null
            val start = TimeOfDay.fromMinutes(startMinutes)
            val end = endMinutes?.let(TimeOfDay::fromMinutes)
            return EventTimeRange(start = start, end = end).normalized()
        }
    }
}
