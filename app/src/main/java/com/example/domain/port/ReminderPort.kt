package com.example.domain.port

import com.example.domain.model.CalendarEvent
import com.example.domain.model.DateKey
import com.example.domain.model.EventId

interface ReminderPort {
    fun ensureChannel()
    fun schedule(event: CalendarEvent)
    fun cancel(eventId: EventId)
    fun scheduleSnooze(
        eventId: EventId,
        title: String,
        dateKey: DateKey,
        locationSummary: String?,
        snoozeMinutes: Int,
    ) {}
}
