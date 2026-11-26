package com.example.data.transfer

import com.example.data.preferences.toDomainOrDefault
import com.example.data.preferences.toDomainOrNull
import com.example.data.preferences.toDto
import com.example.domain.model.AppPreferences
import com.example.domain.model.BackupEventRecord
import com.example.domain.model.BackupNoteRecord
import com.example.domain.model.CalendarEntryType
import com.example.domain.model.CalendarEvent
import com.example.domain.model.DateKey
import com.example.domain.model.EventColorTag
import com.example.domain.model.EventId
import com.example.domain.model.EventLocation
import com.example.domain.model.EventRecurrence
import com.example.domain.model.EventTimeRange
import com.example.domain.model.EventTypeDetails
import com.example.domain.model.FullBackupSnapshot
import com.example.domain.model.GeoPoint
import com.example.domain.model.Note
import com.example.domain.model.NoteId
import com.example.domain.model.ReminderTime
import com.example.domain.model.TaskPriority
import com.example.domain.model.TimeOfDay
import com.example.domain.model.TransportMode
import com.example.domain.service.NoteRichDocumentCodec
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class FullBackupCodec(
    private val json: Json = defaultJson,
) {
    fun encode(snapshot: FullBackupSnapshot): String {
        val envelope = FullBackupEnvelopeDto(
            app = FullBackupEnvelopeDto.APP_MARKER,
            schemaVersion = snapshot.schemaVersion,
            exportedAtMillis = snapshot.exportedAtMillis,
            events = snapshot.events.map(::toEventDto),
            notes = snapshot.notes.map(::toNoteDto),
            colorWheelSnapshot = snapshot.colorWheelSnapshot?.toDto(),
            savedPalettes = snapshot.savedPalettes.map { it.toDto() },
            preferences = snapshot.preferences?.let { prefs ->
                BackupPreferencesDto(
                    defaultReminderMinutes = prefs.defaultReminderTime.minutesOfDay,
                    defaultSnoozeMinutes = prefs.defaultSnoozeMinutes,
                    weekStartsOnMonday = prefs.weekStartsOnMonday,
                )
            },
        )
        return json.encodeToString(envelope)
    }

    fun decode(raw: String): FullBackupSnapshot? {
        if (raw.isBlank()) return null
        val envelope = runCatching {
            json.decodeFromString<FullBackupEnvelopeDto>(raw)
        }.getOrNull() ?: return null

        if (envelope.app != FullBackupEnvelopeDto.APP_MARKER || envelope.schemaVersion <= 0) {
            return null
        }

        val events = envelope.events.mapNotNull(::toEventRecordOrNull)
        val notes = envelope.notes.mapNotNull(::toNoteRecordOrNull)
        val colorSnapshot = envelope.colorWheelSnapshot?.toDomainOrDefault()
        val savedPalettes = envelope.savedPalettes.mapNotNull { it.toDomainOrNull() }
        val preferences = envelope.preferences?.let { dto ->
            AppPreferences(
                defaultReminderTime = ReminderTime.fromMinutes(dto.defaultReminderMinutes),
                defaultSnoozeMinutes = AppPreferences.normalizeSnoozeMinutes(dto.defaultSnoozeMinutes),
                weekStartsOnMonday = dto.weekStartsOnMonday,
            )
        }

        return FullBackupSnapshot(
            schemaVersion = envelope.schemaVersion,
            exportedAtMillis = envelope.exportedAtMillis,
            events = events,
            notes = notes,
            colorWheelSnapshot = colorSnapshot,
            savedPalettes = savedPalettes,
            preferences = preferences,
        )
    }

    private fun toEventDto(record: BackupEventRecord): BackupEventDto {
        val e = record.event
        val details = e.details
        return BackupEventDto(
            backupKey = record.backupKey,
            title = e.title,
            dateKey = e.dateKey.value,
            endDateKey = e.endDateKey?.value,
            isAllDay = e.isAllDay,
            startTimeMinutes = e.timeRange?.start?.minutesOfDay,
            endTimeMinutes = e.timeRange?.end?.minutesOfDay,
            description = e.description,
            recurrenceCode = e.recurrence.code,
            colorTagCode = e.colorTag.code,
            entryTypeName = e.entryType.name,
            reminderTimeMinutes = e.reminderTime?.minutesOfDay,
            locationLat = e.location?.point?.lat,
            locationLng = e.location?.point?.lng,
            locationLabel = e.location?.sanitizedLabel,
            createdAtMillis = e.createdAtMillis,
            guests = (details as? EventTypeDetails.StandardEvent)?.guests.orEmpty(),
            meetingLink = (details as? EventTypeDetails.StandardEvent)?.meetingLink,
            taskPriorityName = (details as? EventTypeDetails.Task)?.priority?.name,
            taskCompleted = (details as? EventTypeDetails.Task)?.isCompleted == true,
            checklist = when (details) {
                is EventTypeDetails.Task -> details.checklist
                is EventTypeDetails.Trip -> details.packingChecklist
                else -> emptyList()
            },
            celebrantName = (details as? EventTypeDetails.Birthday)?.celebrantName,
            birthYear = (details as? EventTypeDetails.Birthday)?.birthYear,
            giftIdea = (details as? EventTypeDetails.Birthday)?.giftIdea,
            specialistName = (details as? EventTypeDetails.Appointment)?.specialistName,
            contactPhone = (details as? EventTypeDetails.Appointment)?.contactPhone,
            preparationNotes = (details as? EventTypeDetails.Appointment)?.preparationNotes,
            transportModeName = (details as? EventTypeDetails.Trip)?.transportMode?.name,
            bookingReference = (details as? EventTypeDetails.Trip)?.bookingReference,
        )
    }

    private fun toEventRecordOrNull(dto: BackupEventDto): BackupEventRecord? {
        val title = dto.title.trim()
        val dateKey = DateKey.parseOrNull(dto.dateKey) ?: return null
        if (title.isEmpty()) return null

        val entryType = CalendarEntryType.entries.firstOrNull { it.name == dto.entryTypeName }
            ?: CalendarEntryType.EVENT
        val endDateKey = dto.endDateKey?.let(DateKey::parseOrNull)?.takeIf { it > dateKey }
        val timeRange = if (!dto.isAllDay && dto.startTimeMinutes != null) {
            val start = TimeOfDay.fromMinutes(dto.startTimeMinutes)
            val end = dto.endTimeMinutes?.let(TimeOfDay::fromMinutes) ?: start.plusMinutes(60)
            EventTimeRange(start = start, end = end).normalized()
        } else {
            null
        }
        val location = GeoPoint.createOrNull(dto.locationLat, dto.locationLng)?.let { point ->
            EventLocation(point = point, label = dto.locationLabel)
        }
        val details: EventTypeDetails = when (entryType) {
            CalendarEntryType.EVENT -> EventTypeDetails.StandardEvent(
                guests = dto.guests,
                meetingLink = dto.meetingLink,
            ).sanitized()
            CalendarEntryType.TASK -> EventTypeDetails.Task(
                priority = TaskPriority.entries.firstOrNull { it.name == dto.taskPriorityName }
                    ?: TaskPriority.MEDIUM,
                isCompleted = dto.taskCompleted,
                checklist = dto.checklist,
            ).sanitized()
            CalendarEntryType.BIRTHDAY -> EventTypeDetails.Birthday(
                celebrantName = dto.celebrantName,
                birthYear = dto.birthYear,
                giftIdea = dto.giftIdea,
            ).sanitized()
            CalendarEntryType.APPOINTMENT -> EventTypeDetails.Appointment(
                specialistName = dto.specialistName,
                contactPhone = dto.contactPhone,
                preparationNotes = dto.preparationNotes,
            ).sanitized()
            CalendarEntryType.TRIP -> EventTypeDetails.Trip(
                transportMode = TransportMode.entries.firstOrNull { it.name == dto.transportModeName }
                    ?: TransportMode.CAR,
                bookingReference = dto.bookingReference,
                packingChecklist = dto.checklist,
            ).sanitized()
        }

        val event = CalendarEvent(
            id = EventId.UNASSIGNED,
            title = title,
            dateKey = dateKey,
            endDateKey = endDateKey,
            isAllDay = dto.isAllDay || timeRange == null,
            timeRange = timeRange,
            description = dto.description.trim(),
            recurrence = EventRecurrence.fromCodeOrDefault(dto.recurrenceCode),
            colorTag = EventColorTag.fromCodeOrDefault(dto.colorTagCode, entryType.defaultColorTag),
            details = details,
            reminderTime = dto.reminderTimeMinutes?.let(ReminderTime::fromMinutes),
            location = location,
            createdAtMillis = dto.createdAtMillis,
        )
        return BackupEventRecord(
            backupKey = dto.backupKey.ifBlank { "evt_${dto.dateKey}_${title.hashCode()}" },
            event = event,
        )
    }

    private fun toNoteDto(record: BackupNoteRecord): BackupNoteDto = BackupNoteDto(
        title = record.note.title,
        content = record.note.content,
        richSpansEncoded = NoteRichDocumentCodec.encodeSpans(record.note.richSpans),
        attachedEventBackupKeys = record.attachedEventBackupKeys,
        createdAtMillis = record.note.createdAtMillis,
    )

    private fun toNoteRecordOrNull(dto: BackupNoteDto): BackupNoteRecord? {
        if (dto.content.isBlank()) return null
        val spans = NoteRichDocumentCodec.decodeSpans(dto.richSpansEncoded)
        return BackupNoteRecord(
            note = Note(
                id = NoteId.UNASSIGNED,
                title = dto.title.trim(),
                content = dto.content,
                richSpans = spans,
                attachedEventIds = emptyList(),
                createdAtMillis = dto.createdAtMillis,
            ),
            attachedEventBackupKeys = dto.attachedEventBackupKeys.distinct(),
        )
    }

    companion object {
        private val defaultJson: Json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
            prettyPrint = true
        }
    }
}
