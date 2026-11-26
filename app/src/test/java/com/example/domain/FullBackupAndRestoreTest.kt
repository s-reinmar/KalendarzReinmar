package com.example.domain

import com.example.data.transfer.FullBackupCodec
import com.example.domain.model.AppPreferences
import com.example.domain.model.BackupRestoreMode
import com.example.domain.model.ColorHarmonyRule
import com.example.domain.model.ColorWheelSnapshot
import com.example.domain.model.DateKey
import com.example.domain.model.EventColorTag
import com.example.domain.model.EventDraft
import com.example.domain.model.EventLocation
import com.example.domain.model.EventRecurrence
import com.example.domain.model.EventTimeRange
import com.example.domain.model.EventTypeDetails
import com.example.domain.model.GeoPoint
import com.example.domain.model.HsvColor
import com.example.domain.model.NoteDraft
import com.example.domain.model.NoteRichDocument
import com.example.domain.model.NoteRichSpan
import com.example.domain.model.NoteTextFormat
import com.example.domain.model.PaletteId
import com.example.domain.model.ReminderTime
import com.example.domain.model.SavedColorPalette
import com.example.domain.model.TaskPriority
import com.example.domain.model.ThemeSurfaceMode
import com.example.domain.model.TimeOfDay
import com.example.domain.model.TransportMode
import com.example.domain.service.BackupRestoreService
import com.example.domain.service.ColorHarmonyEngine
import com.example.fakes.FakeAppPreferencesRepository
import com.example.fakes.FakeClockProvider
import com.example.fakes.FakeColorPaletteRepository
import com.example.fakes.FakeEventRepository
import com.example.fakes.FakeNoteRepository
import com.example.fakes.FakeReminderPort
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FullBackupAndRestoreTest {

    @Test
    fun codecRoundTripsAllEntryTypesRichNotesPalettesAndPreferences() = runTest {
        val eventRepo = FakeEventRepository()
        val noteRepo = FakeNoteRepository()
        val paletteRepo = FakeColorPaletteRepository()
        val prefsRepo = FakeAppPreferencesRepository(
            AppPreferences(
                defaultReminderTime = ReminderTime.fromHourMinute(7, 45),
                defaultSnoozeMinutes = 10,
                weekStartsOnMonday = false,
            ),
        )
        val reminderPort = FakeReminderPort()
        val clock = FakeClockProvider()

        val e1 = requireNotNull(
            eventRepo.addEvent(
                EventDraft(
                    title = "Spotkanie zarządu",
                    dateKey = requireNotNull(DateKey.parseOrNull("2026-12-01")),
                    isAllDay = false,
                    timeRange = EventTimeRange(
                        start = TimeOfDay.fromHourMinute(10, 0),
                        end = TimeOfDay.fromHourMinute(11, 30),
                    ),
                    description = "Omówienie budżetu",
                    recurrence = EventRecurrence.Weekly,
                    colorTag = EventColorTag.fromRgb(0x33, 0x66, 0x99),
                    details = EventTypeDetails.StandardEvent(
                        guests = listOf("Jan", "Ewa"),
                        meetingLink = "https://meet.example.com/1",
                    ),
                    reminderTime = ReminderTime.fromHourMinute(9, 45),
                    location = EventLocation(
                        point = requireNotNull(GeoPoint.createOrNull(52.2297, 21.0122)),
                        label = "Warszawa Centrum",
                    ),
                ),
            ),
        )
        eventRepo.addEvent(
            EventDraft(
                title = "Oddać projekt",
                dateKey = requireNotNull(DateKey.parseOrNull("2026-12-02")),
                details = EventTypeDetails.Task(
                    priority = TaskPriority.HIGH,
                    isCompleted = true,
                    checklist = listOf("Testy", "Dokumentacja"),
                ),
            ),
        )
        eventRepo.addEvent(
            EventDraft(
                title = "Urodziny Kasi",
                dateKey = requireNotNull(DateKey.parseOrNull("2026-12-03")),
                recurrence = EventRecurrence.Yearly,
                details = EventTypeDetails.Birthday(
                    celebrantName = "Kasia",
                    birthYear = 1994,
                    giftIdea = "Książka",
                ),
            ),
        )
        eventRepo.addEvent(
            EventDraft(
                title = "Dentysta",
                dateKey = requireNotNull(DateKey.parseOrNull("2026-12-04")),
                details = EventTypeDetails.Appointment(
                    specialistName = "Dr Nowak",
                    contactPhone = "+48 500 600 700",
                    preparationNotes = "RTG",
                ),
            ),
        )
        eventRepo.addEvent(
            EventDraft(
                title = "Wyjazd w góry",
                dateKey = requireNotNull(DateKey.parseOrNull("2026-12-10")),
                endDateKey = requireNotNull(DateKey.parseOrNull("2026-12-14")),
                details = EventTypeDetails.Trip(
                    transportMode = TransportMode.TRAIN,
                    bookingReference = "PKP-991",
                    packingChecklist = listOf("Kurtka", "Termos"),
                ),
            ),
        )

        noteRepo.addNote(
            NoteDraft(
                title = "Agenda",
                document = NoteRichDocument(
                    text = "Ważne punkty na spotkanie",
                    spans = listOf(NoteRichSpan(start = 0, end = 5, format = NoteTextFormat(bold = true))),
                ),
                attachedEventIds = listOf(e1.id),
            ),
        )

        val customPalette = SavedColorPalette(
            id = PaletteId("pal_1"),
            name = "Jesień",
            rule = ColorHarmonyRule.TRIAD,
            swatches = ColorHarmonyEngine.generatePalette(
                rule = ColorHarmonyRule.TRIAD,
                baseColor = HsvColor.of(30f, 0.8f, 0.85f),
            ),
            createdAtMillis = 12345L,
            surfaceMode = ThemeSurfaceMode.DARK,
        )
        paletteRepo.addSavedPalette(customPalette)
        paletteRepo.saveCurrentSnapshot(
            ColorWheelSnapshot(
                rule = ColorHarmonyRule.TRIAD,
                swatches = customPalette.swatches,
                selectedSwatchIndex = 1,
                applyToAppTheme = true,
                surfaceMode = ThemeSurfaceMode.DARK,
            ),
        )

        val service = BackupRestoreService(
            eventRepository = eventRepo,
            noteRepository = noteRepo,
            colorPaletteRepository = paletteRepo,
            appPreferencesRepository = prefsRepo,
            reminderPort = reminderPort,
            clock = clock,
        )
        val codec = FullBackupCodec()

        val snapshot = service.createSnapshot()
        val rawJson = codec.encode(snapshot)
        val decoded = codec.decode(rawJson)

        assertNotNull(decoded)
        requireNotNull(decoded)
        assertEquals(5, decoded.events.size)
        assertEquals(1, decoded.notes.size)
        assertEquals(1, decoded.notes.first().attachedEventBackupKeys.size)
        assertEquals(1, decoded.savedPalettes.size)
        assertEquals(ReminderTime.fromHourMinute(7, 45), decoded.preferences?.defaultReminderTime)
        assertEquals(10, decoded.preferences?.defaultSnoozeMinutes)
        assertEquals(false, decoded.preferences?.weekStartsOnMonday)
        assertNull(codec.decode("not valid json"))
        assertNull(codec.decode("""{"app":"other","schemaVersion":1}"""))
    }

    @Test
    fun restoreMergeDeduplicatesAndReplaceRemapsAttachedEventIdsAndReschedulesAlarms() = runTest {
        val sourceEvents = FakeEventRepository()
        val sourceNotes = FakeNoteRepository()
        val sourcePalettes = FakeColorPaletteRepository()
        val sourcePrefs = FakeAppPreferencesRepository(
            AppPreferences(
                defaultReminderTime = ReminderTime.fromHourMinute(8, 15),
                defaultSnoozeMinutes = 30,
                weekStartsOnMonday = false,
            ),
        )
        val sourceClock = FakeClockProvider()

        val meeting = requireNotNull(
            sourceEvents.addEvent(
                EventDraft(
                    title = "Planowanie sprintu",
                    dateKey = requireNotNull(DateKey.parseOrNull("2026-12-05")),
                    reminderTime = ReminderTime.fromHourMinute(8, 0),
                ),
            ),
        )
        sourceNotes.addNote(
            NoteDraft(
                title = "Notatka ze sprintu",
                document = NoteRichDocument(text = "Zadania do wykonania"),
                attachedEventIds = listOf(meeting.id),
            ),
        )
        val snapshot = BackupRestoreService(
            eventRepository = sourceEvents,
            noteRepository = sourceNotes,
            colorPaletteRepository = sourcePalettes,
            appPreferencesRepository = sourcePrefs,
            reminderPort = FakeReminderPort(),
            clock = sourceClock,
        ).createSnapshot()

        // Target already has one unrelated event with reminder + duplicate of meeting
        val targetEvents = FakeEventRepository()
        val targetNotes = FakeNoteRepository()
        val targetPalettes = FakeColorPaletteRepository()
        val targetPrefs = FakeAppPreferencesRepository()
        val targetReminders = FakeReminderPort()
        val oldEvent = requireNotNull(
            targetEvents.addEvent(
                EventDraft(
                    title = "Stare wydarzenie",
                    dateKey = requireNotNull(DateKey.parseOrNull("2026-11-28")),
                    reminderTime = ReminderTime.fromHourMinute(9, 0),
                ),
            ),
        )
        val existingMeeting = requireNotNull(
            targetEvents.addEvent(
                EventDraft(
                    title = "Planowanie sprintu",
                    dateKey = requireNotNull(DateKey.parseOrNull("2026-12-05")),
                    reminderTime = ReminderTime.fromHourMinute(8, 0),
                ),
            ),
        )

        val targetService = BackupRestoreService(
            eventRepository = targetEvents,
            noteRepository = targetNotes,
            colorPaletteRepository = targetPalettes,
            appPreferencesRepository = targetPrefs,
            reminderPort = targetReminders,
            clock = sourceClock,
        )

        // MERGE skips duplicate event, imports note and attaches it to existingMeeting.id
        val mergeSummary = targetService.restoreSnapshot(snapshot, BackupRestoreMode.MERGE)
        assertEquals(0, mergeSummary.importedEventsCount)
        assertEquals(1, mergeSummary.importedNotesCount)
        val mergedNote = targetNotes.observeNotes().first().first()
        assertEquals(listOf(existingMeeting.id), mergedNote.attachedEventIds)

        // REPLACE cancels old reminders, wipes existing records, re-inserts & remaps note attachment
        val replaceSummary = targetService.restoreSnapshot(snapshot, BackupRestoreMode.REPLACE)
        assertEquals(1, replaceSummary.importedEventsCount)
        assertEquals(1, replaceSummary.importedNotesCount)
        assertTrue(targetReminders.cancelledEventIds.contains(oldEvent.id))

        val restoredEvent = targetEvents.observeEvents().first().single()
        val restoredNote = targetNotes.observeNotes().first().single()
        assertEquals(listOf(restoredEvent.id), restoredNote.attachedEventIds)
        assertTrue(targetReminders.scheduledEvents.any { it.id == restoredEvent.id })
        assertEquals(30, targetPrefs.observePreferences().first().defaultSnoozeMinutes)
        assertEquals(false, targetPrefs.observePreferences().first().weekStartsOnMonday)
    }
}
