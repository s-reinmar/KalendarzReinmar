package com.example.data.local

import com.example.data.CalendarEventDao
import com.example.domain.model.CalendarEvent
import com.example.domain.model.EventDraft
import com.example.domain.model.EventId
import com.example.domain.model.EventLocation
import com.example.domain.model.ReminderTime
import com.example.domain.port.ClockProvider
import com.example.domain.port.EventRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomEventRepository(
    private val eventDao: CalendarEventDao,
    private val clock: ClockProvider,
) : EventRepository {

    override fun observeEvents(): Flow<List<CalendarEvent>> =
        eventDao.getAllEvents().map { entities -> entities.map(CalendarEventMapper::toDomain) }

    override suspend fun getEventsWithReminders(): List<CalendarEvent> =
        eventDao.getEventsWithReminders().map(CalendarEventMapper::toDomain)

    override suspend fun getEventById(eventId: EventId): CalendarEvent? =
        eventDao.getEventById(eventId.value)?.let(CalendarEventMapper::toDomain)

    override suspend fun addEvent(draft: EventDraft): CalendarEvent? {
        val cleanDraft = draft.sanitized() ?: return null
        val createdAt = clock.nowMillis()
        val domainEvent = CalendarEvent(
            id = EventId.UNASSIGNED,
            title = cleanDraft.title,
            dateKey = cleanDraft.dateKey,
            endDateKey = cleanDraft.endDateKey,
            isAllDay = cleanDraft.isAllDay,
            timeRange = cleanDraft.timeRange,
            description = cleanDraft.description,
            recurrence = cleanDraft.recurrence,
            colorTag = cleanDraft.colorTag,
            details = cleanDraft.details,
            reminderTime = cleanDraft.reminderTime,
            location = cleanDraft.location,
            createdAtMillis = createdAt,
        )
        val insertedId = eventDao.insertEvent(CalendarEventMapper.toEntity(domainEvent))
        return domainEvent.copy(id = EventId(insertedId))
    }

    override suspend fun updateEvent(
        event: CalendarEvent,
        draft: EventDraft,
    ): CalendarEvent? {
        val cleanDraft = draft.sanitized() ?: return null
        val updated = event.copy(
            title = cleanDraft.title,
            dateKey = cleanDraft.dateKey,
            endDateKey = cleanDraft.endDateKey,
            isAllDay = cleanDraft.isAllDay,
            timeRange = cleanDraft.timeRange,
            description = cleanDraft.description,
            recurrence = cleanDraft.recurrence,
            colorTag = cleanDraft.colorTag,
            details = cleanDraft.details,
            reminderTime = cleanDraft.reminderTime,
            location = cleanDraft.location,
        )
        eventDao.updateEvent(CalendarEventMapper.toEntity(updated))
        return updated
    }

    override suspend fun setTaskCompleted(eventId: EventId, completed: Boolean) {
        eventDao.updateTaskCompleted(
            eventId = eventId.value,
            completed = completed,
        )
    }

    override suspend fun updateEventReminder(
        eventId: EventId,
        reminderTime: ReminderTime?,
        location: EventLocation?,
    ) {
        eventDao.updateEventReminder(
            eventId = eventId.value,
            reminderTimeMinutes = reminderTime?.minutesOfDay,
            locationLat = location?.point?.lat,
            locationLng = location?.point?.lng,
            locationLabel = location?.sanitizedLabel,
        )
    }

    override suspend fun deleteEvent(eventId: EventId) {
        eventDao.deleteEventById(eventId.value)
    }

    override suspend fun deleteAllEvents() {
        eventDao.deleteAllEvents()
    }

    override suspend fun addEvents(events: List<CalendarEvent>): List<CalendarEvent> {
        val valid = events.mapNotNull { event ->
            val cleanDraft = event.toDraft().sanitized() ?: return@mapNotNull null
            if (cleanDraft.dateKey.value.isBlank()) return@mapNotNull null
            val createdAt = if (event.createdAtMillis > 0L) event.createdAtMillis else clock.nowMillis()
            val normalizedEvent = CalendarEvent(
                id = EventId.UNASSIGNED,
                title = cleanDraft.title,
                dateKey = cleanDraft.dateKey,
                endDateKey = cleanDraft.endDateKey,
                isAllDay = cleanDraft.isAllDay,
                timeRange = cleanDraft.timeRange,
                description = cleanDraft.description,
                recurrence = cleanDraft.recurrence,
                colorTag = cleanDraft.colorTag,
                details = cleanDraft.details,
                reminderTime = cleanDraft.reminderTime,
                location = cleanDraft.location,
                createdAtMillis = createdAt,
            )
            CalendarEventMapper.toEntity(normalizedEvent)
        }
        if (valid.isEmpty()) return emptyList()
        val ids = eventDao.insertEvents(valid)
        return valid.zip(ids) { entity, id ->
            CalendarEventMapper.toDomain(entity.copy(id = id))
        }
    }
}
