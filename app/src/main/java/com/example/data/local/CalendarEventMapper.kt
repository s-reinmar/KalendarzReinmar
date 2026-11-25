package com.example.data.local

import com.example.data.CalendarEventEntity
import com.example.domain.model.CalendarEntryType
import com.example.domain.model.CalendarEvent
import com.example.domain.model.DateKey
import com.example.domain.model.EventColorTag
import com.example.domain.model.EventId
import com.example.domain.model.EventLocation
import com.example.domain.model.EventRecurrence
import com.example.domain.model.EventTimeRange
import com.example.domain.model.EventTypeDetails
import com.example.domain.model.ReminderTime
import com.example.domain.model.TaskPriority
import com.example.domain.model.TransportMode

object CalendarEventMapper {

    private const val LIST_SEPARATOR = "\n"

    fun toDomain(entity: CalendarEventEntity): CalendarEvent {
        val dateKey = DateKey.parseOrNull(entity.dateKey)
            ?: DateKey.unsafe(entity.dateKey)
        val endDateKey = entity.endDateKey?.let(DateKey::parseOrNull)
        val entryType = CalendarEntryType.fromCodeOrDefault(entity.entryType)
        val colorTag = EventColorTag.fromCodeOrDefault(
            raw = entity.colorTag,
            fallback = entryType.defaultColorTag,
        )
        val timeRange = if (entity.isAllDay) {
            null
        } else {
            EventTimeRange.fromRawMinutes(entity.startTimeMinutes, entity.endTimeMinutes)
        }

        return CalendarEvent(
            id = EventId(entity.id),
            title = entity.title,
            dateKey = dateKey,
            endDateKey = endDateKey,
            isAllDay = entity.isAllDay,
            timeRange = timeRange,
            description = entity.description,
            recurrence = EventRecurrence.fromCodeOrDefault(entity.recurrence),
            colorTag = colorTag,
            details = decodeDetails(entryType, entity),
            reminderTime = entity.reminderTimeMinutes?.let(ReminderTime::fromMinutes),
            location = EventLocation.fromRawOrNull(
                lat = entity.locationLat,
                lng = entity.locationLng,
                label = entity.locationLabel,
            ),
            createdAtMillis = entity.createdAt,
        )
    }

    fun toEntity(event: CalendarEvent): CalendarEventEntity {
        val standard = event.details as? EventTypeDetails.StandardEvent
        val task = event.details as? EventTypeDetails.Task
        val birthday = event.details as? EventTypeDetails.Birthday
        val appointment = event.details as? EventTypeDetails.Appointment
        val trip = event.details as? EventTypeDetails.Trip

        val encodedChecklist = when (val details = event.details) {
            is EventTypeDetails.Task -> encodeList(details.checklist)
            is EventTypeDetails.Trip -> encodeList(details.packingChecklist)
            else -> null
        }

        return CalendarEventEntity(
            id = event.id.value,
            title = event.title,
            dateKey = event.dateKey.value,
            reminderTimeMinutes = event.reminderTime?.minutesOfDay,
            locationLat = event.location?.point?.lat,
            locationLng = event.location?.point?.lng,
            locationLabel = event.location?.sanitizedLabel,
            createdAt = event.createdAtMillis,
            entryType = event.entryType.code,
            endDateKey = event.endDateKey?.value,
            isAllDay = event.isAllDay,
            startTimeMinutes = if (event.isAllDay) null else event.timeRange?.start?.minutesOfDay,
            endTimeMinutes = if (event.isAllDay) null else event.timeRange?.end?.minutesOfDay,
            description = event.description,
            recurrence = event.recurrence.code,
            colorTag = event.colorTag.code,
            guests = standard?.let { encodeList(it.guests) },
            meetingLink = standard?.meetingLink,
            taskPriority = task?.priority?.code,
            taskCompleted = task?.isCompleted == true,
            checklistItems = encodedChecklist,
            celebrantName = birthday?.celebrantName,
            birthYear = birthday?.birthYear,
            giftIdea = birthday?.giftIdea,
            specialistName = appointment?.specialistName,
            contactPhone = appointment?.contactPhone,
            preparationNotes = appointment?.preparationNotes,
            transportMode = trip?.transportMode?.code,
            bookingReference = trip?.bookingReference,
        )
    }

    private fun decodeDetails(
        entryType: CalendarEntryType,
        entity: CalendarEventEntity,
    ): EventTypeDetails = when (entryType) {
        CalendarEntryType.EVENT -> EventTypeDetails.StandardEvent(
            guests = decodeList(entity.guests),
            meetingLink = entity.meetingLink?.takeIf(String::isNotBlank),
        )
        CalendarEntryType.TASK -> EventTypeDetails.Task(
            priority = TaskPriority.fromCodeOrDefault(entity.taskPriority),
            isCompleted = entity.taskCompleted,
            checklist = decodeList(entity.checklistItems),
        )
        CalendarEntryType.BIRTHDAY -> EventTypeDetails.Birthday(
            celebrantName = entity.celebrantName?.takeIf(String::isNotBlank),
            birthYear = entity.birthYear,
            giftIdea = entity.giftIdea?.takeIf(String::isNotBlank),
        )
        CalendarEntryType.APPOINTMENT -> EventTypeDetails.Appointment(
            specialistName = entity.specialistName?.takeIf(String::isNotBlank),
            contactPhone = entity.contactPhone?.takeIf(String::isNotBlank),
            preparationNotes = entity.preparationNotes?.takeIf(String::isNotBlank),
        )
        CalendarEntryType.TRIP -> EventTypeDetails.Trip(
            transportMode = TransportMode.fromCodeOrDefault(entity.transportMode),
            bookingReference = entity.bookingReference?.takeIf(String::isNotBlank),
            packingChecklist = decodeList(entity.checklistItems),
        )
    }

    private fun encodeList(items: List<String>): String? {
        val cleaned = items.mapNotNull { it.trim().takeIf(String::isNotEmpty) }
        return if (cleaned.isEmpty()) null else cleaned.joinToString(LIST_SEPARATOR)
    }

    private fun decodeList(raw: String?): List<String> {
        if (raw.isNullOrBlank()) return emptyList()
        return raw.split(LIST_SEPARATOR).mapNotNull { it.trim().takeIf(String::isNotEmpty) }
    }
}
