package com.example.domain.port

import com.example.domain.model.CalendarEvent
import com.example.domain.model.EventDraft
import com.example.domain.model.EventId
import com.example.domain.model.EventLocation
import com.example.domain.model.ReminderTime
import kotlinx.coroutines.flow.Flow

interface EventRepository {
    fun observeEvents(): Flow<List<CalendarEvent>>

    suspend fun getEventsWithReminders(): List<CalendarEvent>

    suspend fun getEventById(eventId: EventId): CalendarEvent? = null

    suspend fun addEvent(draft: EventDraft): CalendarEvent?

    suspend fun updateEvent(
        event: CalendarEvent,
        draft: EventDraft,
    ): CalendarEvent?

    suspend fun setTaskCompleted(
        eventId: EventId,
        completed: Boolean,
    )

    suspend fun updateEventReminder(
        eventId: EventId,
        reminderTime: ReminderTime?,
        location: EventLocation?,
    )

    suspend fun deleteEvent(eventId: EventId)

    suspend fun deleteAllEvents() {}

    suspend fun addEvents(events: List<CalendarEvent>): List<CalendarEvent>
}
