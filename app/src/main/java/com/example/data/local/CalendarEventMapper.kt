package com.example.data.local

import com.example.data.CalendarEventEntity
import com.example.domain.model.CalendarEvent
import com.example.domain.model.DateKey
import com.example.domain.model.EventId
import com.example.domain.model.EventLocation
import com.example.domain.model.ReminderTime

object CalendarEventMapper {
    fun toDomain(entity: CalendarEventEntity): CalendarEvent {
        val dateKey = DateKey.parseOrNull(entity.dateKey)
            ?: DateKey.unsafe(entity.dateKey)
        return CalendarEvent(
            id = EventId(entity.id),
            title = entity.title,
            dateKey = dateKey,
            reminderTime = entity.reminderTimeMinutes?.let(ReminderTime::fromMinutes),
            location = EventLocation.fromRawOrNull(
                lat = entity.locationLat,
                lng = entity.locationLng,
                label = entity.locationLabel,
            ),
            createdAtMillis = entity.createdAt,
        )
    }

    fun toEntity(event: CalendarEvent): CalendarEventEntity = CalendarEventEntity(
        id = event.id.value,
        title = event.title,
        dateKey = event.dateKey.value,
        reminderTimeMinutes = event.reminderTime?.minutesOfDay,
        locationLat = event.location?.point?.lat,
        locationLng = event.location?.point?.lng,
        locationLabel = event.location?.sanitizedLabel,
        createdAt = event.createdAtMillis,
    )
}
