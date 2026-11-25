package com.example.data.local

import com.example.data.CalendarEventDao
import com.example.data.CalendarEventEntity
import com.example.domain.model.CalendarEvent
import com.example.domain.model.DateKey
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

    override suspend fun addEvent(
        title: String,
        dateKey: DateKey,
        reminderTime: ReminderTime?,
        location: EventLocation?,
    ): CalendarEvent? {
        val trimmedTitle = title.trim()
        if (trimmedTitle.isEmpty()) return null

        val createdAt = clock.nowMillis()
        val entity = CalendarEventEntity(
            title = trimmedTitle,
            dateKey = dateKey.value,
            reminderTimeMinutes = reminderTime?.minutesOfDay,
            locationLat = location?.point?.lat,
            locationLng = location?.point?.lng,
            locationLabel = location?.sanitizedLabel,
            createdAt = createdAt,
        )
        val insertedId = eventDao.insertEvent(entity)
        return CalendarEventMapper.toDomain(entity.copy(id = insertedId))
    }

    override suspend fun updateEvent(
        event: CalendarEvent,
        title: String,
        dateKey: DateKey,
        reminderTime: ReminderTime?,
        location: EventLocation?,
    ): CalendarEvent? {
        val trimmedTitle = title.trim()
        if (trimmedTitle.isEmpty()) return null

        val updated = event.copy(
            title = trimmedTitle,
            dateKey = dateKey,
            reminderTime = reminderTime,
            location = location?.let { EventLocation(it.point, it.sanitizedLabel) },
        )
        eventDao.updateEvent(CalendarEventMapper.toEntity(updated))
        return updated
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

    override suspend fun addEvents(events: List<CalendarEvent>): List<CalendarEvent> {
        val valid = events.mapNotNull { event ->
            val trimmedTitle = event.title.trim()
            if (trimmedTitle.isEmpty() || event.dateKey.value.isBlank()) {
                null
            } else {
                val createdAt = if (event.createdAtMillis > 0L) event.createdAtMillis else clock.nowMillis()
                CalendarEventEntity(
                    id = 0L,
                    title = trimmedTitle,
                    dateKey = event.dateKey.value,
                    reminderTimeMinutes = event.reminderTime?.minutesOfDay,
                    locationLat = event.location?.point?.lat,
                    locationLng = event.location?.point?.lng,
                    locationLabel = event.location?.sanitizedLabel,
                    createdAt = createdAt,
                )
            }
        }
        if (valid.isEmpty()) return emptyList()
        val ids = eventDao.insertEvents(valid)
        return valid.zip(ids) { entity, id ->
            CalendarEventMapper.toDomain(entity.copy(id = id))
        }
    }
}
