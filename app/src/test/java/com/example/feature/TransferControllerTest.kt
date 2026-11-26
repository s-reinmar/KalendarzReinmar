package com.example.feature

import com.example.data.transfer.CsvTransferCodec
import com.example.data.transfer.IcsEventCodec
import com.example.domain.model.TransferFeedback
import com.example.fakes.FakeClockProvider
import com.example.fakes.FakeDocumentStorage
import com.example.fakes.FakeEventRepository
import com.example.fakes.FakeNoteRepository
import com.example.feature.transfer.TransferController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TransferControllerTest {

    @Test
    fun exportAndImportIcsAndCsvUseDocumentStoragePort() = runTest {
        val controllerScope = CoroutineScope(coroutineContext + SupervisorJob())
        try {
            val clock = FakeClockProvider()
            val eventRepo = FakeEventRepository()
            val noteRepo = FakeNoteRepository()
            val storage = FakeDocumentStorage()
            val dispatcher = UnconfinedTestDispatcher(testScheduler)

            val controller = TransferController(
                eventRepository = eventRepo,
                noteRepository = noteRepo,
                documentStorage = storage,
                icsCodec = IcsEventCodec(clock),
                csvCodec = CsvTransferCodec(),
                scope = controllerScope,
                computationDispatcher = dispatcher,
            )

            val feedback = mutableListOf<TransferFeedback>()
            backgroundScope.launch(dispatcher) {
                controller.feedback.collect { feedback.add(it) }
            }

            storage.files["content://transfer/in.csv"] = "data;tytuł\r\n25.11.2026;Konferencja\r\n"
            controller.importCsv("content://transfer/in.csv", preferNotesWhenAmbiguous = false)
            advanceUntilIdle()

            assertEquals(TransferFeedback.ImportedEvents(1), feedback.last())
            assertEquals("Konferencja", eventRepo.observeEvents().first().first().title)

            controller.exportIcs("content://transfer/out.ics")
            advanceUntilIdle()

            assertEquals(TransferFeedback.ExportedEvents(1), feedback.last())
            assertTrue(storage.files["content://transfer/out.ics"].orEmpty().contains("SUMMARY:Konferencja"))

            // Re-importing the same CSV should deduplicate and emit NothingToImport
            controller.importCsv("content://transfer/in.csv", preferNotesWhenAmbiguous = false)
            advanceUntilIdle()
            assertEquals(TransferFeedback.NothingToImport, feedback.last())
            assertEquals(1, eventRepo.observeEvents().first().size)
        } finally {
            controllerScope.cancel()
        }
    }

    @Test
    fun fullJsonBackupExportPreviewAndRestoreWorkEndToEnd() = runTest {
        val controllerScope = CoroutineScope(coroutineContext + SupervisorJob())
        try {
            val clock = FakeClockProvider()
            val eventRepo = FakeEventRepository()
            val noteRepo = FakeNoteRepository()
            val paletteRepo = com.example.fakes.FakeColorPaletteRepository()
            val prefsRepo = com.example.fakes.FakeAppPreferencesRepository()
            val reminderPort = com.example.fakes.FakeReminderPort()
            val storage = FakeDocumentStorage()
            val dispatcher = UnconfinedTestDispatcher(testScheduler)
            val backupService = com.example.domain.service.BackupRestoreService(
                eventRepository = eventRepo,
                noteRepository = noteRepo,
                colorPaletteRepository = paletteRepo,
                appPreferencesRepository = prefsRepo,
                reminderPort = reminderPort,
                clock = clock,
            )

            eventRepo.addEvent(
                com.example.domain.model.EventDraft(
                    title = "Wizyta lekarska",
                    dateKey = requireNotNull(com.example.domain.model.DateKey.parseOrNull("2026-12-01")),
                    reminderTime = com.example.domain.model.ReminderTime.fromHourMinute(8, 30),
                ),
            )

            val controller = TransferController(
                eventRepository = eventRepo,
                noteRepository = noteRepo,
                documentStorage = storage,
                icsCodec = IcsEventCodec(clock),
                csvCodec = CsvTransferCodec(),
                scope = controllerScope,
                computationDispatcher = dispatcher,
                backupRestoreService = backupService,
                reminderPort = reminderPort,
            )

            val feedback = mutableListOf<TransferFeedback>()
            backgroundScope.launch(dispatcher) {
                controller.feedback.collect { feedback.add(it) }
            }

            controller.exportFullBackup("content://transfer/backup.json")
            advanceUntilIdle()
            assertEquals(
                TransferFeedback.ExportedBackup(eventsCount = 1, notesCount = 0, palettesCount = 0),
                feedback.last(),
            )

            // Wipe repository, prepare import, verify preview, then confirm REPLACE
            eventRepo.deleteAllEvents()
            controller.prepareImportFullBackup("content://transfer/backup.json")
            advanceUntilIdle()
            assertEquals(1, controller.uiState.value.pendingBackupRestore?.preview?.eventsCount)

            controller.confirmRestoreBackup(com.example.domain.model.BackupRestoreMode.REPLACE)
            advanceUntilIdle()
            assertEquals(
                TransferFeedback.RestoredBackup(eventsCount = 1, notesCount = 0, palettesCount = 0),
                feedback.last(),
            )
            assertEquals(1, eventRepo.observeEvents().first().size)
            assertTrue(reminderPort.scheduledEvents.isNotEmpty())
        } finally {
            controllerScope.cancel()
        }
    }
}
