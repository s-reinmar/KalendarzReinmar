package com.example.domain.port

import com.example.domain.model.CalendarEvent
import com.example.domain.model.DateKey
import com.example.domain.model.EventId
import com.example.domain.model.EventLocation
import com.example.domain.model.ReminderTime
import kotlinx.coroutines.flow.Flow

interface EventRepository {
    fun observeEvents(): Flow<List<CalendarEvent>>

    suspend fun getEventsWithReminders(): List<CalendarEvent>

    suspend fun addEvent(
        title: String,
        dateKey: DateKey,
        reminderTime: ReminderTime? = null,
        location: EventLocation? = null,
    ): CalendarEvent?

    suspend fun updateEvent(
        event: CalendarEvent,
        title: String,
        dateKey: DateKey,
        reminderTime: ReminderTime?,
        location: EventLocation?,
    ): CalendarEvent?

    suspend fun updateEventReminder(
        eventId: EventId,
        reminderTime: ReminderTime?,
        location: EventLocation?,
    )

    suspend fun deleteEvent(eventId: EventId)

    suspend fun addEvents(events: List<CalendarEvent>): List<CalendarEvent>
}
