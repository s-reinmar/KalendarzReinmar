package com.example.domain.service

import com.example.domain.model.BackupEventRecord
import com.example.domain.model.BackupNoteRecord
import com.example.domain.model.BackupRestoreMode
import com.example.domain.model.BackupRestoreSummary
import com.example.domain.model.CalendarEvent
import com.example.domain.model.EventId
import com.example.domain.model.FullBackupSnapshot
import com.example.domain.model.NoteId
import com.example.domain.port.AppPreferencesRepository
import com.example.domain.port.ClockProvider
import com.example.domain.port.ColorPaletteRepository
import com.example.domain.port.EventRepository
import com.example.domain.port.NoteRepository
import com.example.domain.port.ReminderPort
import kotlinx.coroutines.flow.first

class BackupRestoreService(
    private val eventRepository: EventRepository,
    private val noteRepository: NoteRepository,
    private val colorPaletteRepository: ColorPaletteRepository,
    private val appPreferencesRepository: AppPreferencesRepository,
    private val reminderPort: ReminderPort,
    private val clock: ClockProvider,
) {
    suspend fun createSnapshot(): FullBackupSnapshot {
        val events = eventRepository.observeEvents().first()
        val notes = noteRepository.observeNotes().first()
        val colorState = colorPaletteRepository.observeState().first()
        val preferences = appPreferencesRepository.observePreferences().first()

        val keyByEventId = HashMap<EventId, String>(events.size)
        val eventRecords = events.mapIndexed { index, event ->
            val key = "evt_${index + 1}"
            keyByEventId[event.id] = key
            BackupEventRecord(
                backupKey = key,
                event = event,
            )
        }

        val noteRecords = notes.map { note ->
            val attachedKeys = note.attachedEventIds.mapNotNull { keyByEventId[it] }.distinct()
            BackupNoteRecord(
                note = note,
                attachedEventBackupKeys = attachedKeys,
            )
        }

        return FullBackupSnapshot(
            schemaVersion = FullBackupSnapshot.CURRENT_SCHEMA_VERSION,
            exportedAtMillis = clock.nowMillis(),
            events = eventRecords,
            notes = noteRecords,
            colorWheelSnapshot = colorState.activeSnapshot,
            savedPalettes = colorState.savedPalettes,
            preferences = preferences,
        )
    }

    suspend fun restoreSnapshot(
        snapshot: FullBackupSnapshot,
        mode: BackupRestoreMode,
    ): BackupRestoreSummary {
        return when (mode) {
            BackupRestoreMode.REPLACE -> restoreReplace(snapshot)
            BackupRestoreMode.MERGE -> restoreMerge(snapshot)
        }
    }

    private suspend fun restoreReplace(snapshot: FullBackupSnapshot): BackupRestoreSummary {
        val existingWithReminders = eventRepository.getEventsWithReminders()
        existingWithReminders.forEach { reminderPort.cancel(it.id) }

        noteRepository.deleteAllNotes()
        eventRepository.deleteAllEvents()

        val validEventRecords = snapshot.events.filter { it.event.title.trim().isNotEmpty() }
        val insertedEvents = eventRepository.addEvents(
            validEventRecords.map { it.event.copy(id = EventId.UNASSIGNED) },
        )

        val newIdByBackupKey = HashMap<String, EventId>(insertedEvents.size)
        validEventRecords.zip(insertedEvents).forEach { (record, inserted) ->
            newIdByBackupKey[record.backupKey] = inserted.id
        }

        val remappedNotes = snapshot.notes.map { record ->
            val mappedIds = record.attachedEventBackupKeys
                .mapNotNull { newIdByBackupKey[it] }
                .distinct()
            record.note.copy(
                id = NoteId.UNASSIGNED,
                attachedEventIds = mappedIds,
            )
        }
        val insertedNotesCount = noteRepository.addNotes(remappedNotes)

        snapshot.colorWheelSnapshot?.let { colorPaletteRepository.saveCurrentSnapshot(it) }
        colorPaletteRepository.replaceSavedPalettes(snapshot.savedPalettes)
        snapshot.preferences?.let { appPreferencesRepository.updatePreferences(it.normalized()) }

        insertedEvents.filter { it.hasReminder }.forEach(reminderPort::schedule)

        return BackupRestoreSummary(
            importedEventsCount = insertedEvents.size,
            importedNotesCount = insertedNotesCount,
            importedPalettesCount = snapshot.savedPalettes.size,
            mode = BackupRestoreMode.REPLACE,
        )
    }

    private suspend fun restoreMerge(snapshot: FullBackupSnapshot): BackupRestoreSummary {
        val existingEvents = eventRepository.observeEvents().first()
        val existingNotes = noteRepository.observeNotes().first()
        val existingColorState = colorPaletteRepository.observeState().first()

        val existingByFingerprint = HashMap<String, CalendarEvent>(existingEvents.size)
        for (existing in existingEvents) {
            existingByFingerprint[EventDeduplicator.eventFingerprint(existing)] = existing
        }

        val idByBackupKey = HashMap<String, EventId>(snapshot.events.size)
        val recordsToInsert = mutableListOf<BackupEventRecord>()
        val newlySeenFingerprints = HashSet<String>()

        for (record in snapshot.events) {
            if (record.event.title.trim().isEmpty()) continue
            val fp = EventDeduplicator.eventFingerprint(record.event)
            val alreadyExisting = existingByFingerprint[fp]
            if (alreadyExisting != null) {
                idByBackupKey[record.backupKey] = alreadyExisting.id
            } else if (newlySeenFingerprints.add(fp)) {
                recordsToInsert.add(record)
            }
        }

        val insertedEvents = if (recordsToInsert.isNotEmpty()) {
            eventRepository.addEvents(
                recordsToInsert.map { it.event.copy(id = EventId.UNASSIGNED) },
            )
        } else {
            emptyList()
        }

        recordsToInsert.zip(insertedEvents).forEach { (record, inserted) ->
            idByBackupKey[record.backupKey] = inserted.id
        }

        val existingNoteFingerprints = existingNotes
            .mapTo(HashSet(existingNotes.size)) { EventDeduplicator.noteFingerprint(it) }
        val notesToInsert = snapshot.notes.mapNotNull { record ->
            val fp = EventDeduplicator.noteFingerprint(record.note)
            if (!existingNoteFingerprints.add(fp)) {
                null
            } else {
                val mappedIds = record.attachedEventBackupKeys
                    .mapNotNull { idByBackupKey[it] }
                    .distinct()
                record.note.copy(
                    id = NoteId.UNASSIGNED,
                    attachedEventIds = mappedIds,
                )
            }
        }
        val insertedNotesCount = if (notesToInsert.isNotEmpty()) {
            noteRepository.addNotes(notesToInsert)
        } else {
            0
        }

        val existingPaletteIds = existingColorState.savedPalettes.mapTo(HashSet()) { it.id }
        val newPalettes = snapshot.savedPalettes.filter { existingPaletteIds.add(it.id) }
        if (newPalettes.isNotEmpty()) {
            colorPaletteRepository.replaceSavedPalettes(existingColorState.savedPalettes + newPalettes)
        }

        insertedEvents.filter { it.hasReminder }.forEach(reminderPort::schedule)

        return BackupRestoreSummary(
            importedEventsCount = insertedEvents.size,
            importedNotesCount = insertedNotesCount,
            importedPalettesCount = newPalettes.size,
            mode = BackupRestoreMode.MERGE,
        )
    }
}
