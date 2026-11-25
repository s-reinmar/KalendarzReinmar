package com.example.feature

import com.example.data.transfer.CsvTransferCodec
import com.example.data.transfer.IcsEventCodec
import com.example.domain.model.NoteDraft
import com.example.domain.model.TransferFeedback
import com.example.domain.service.DemoDataFactory
import com.example.fakes.FakeClockProvider
import com.example.fakes.FakeDocumentStorage
import com.example.fakes.FakeEventRepository
import com.example.fakes.FakeNoteRepository
import com.example.fakes.FakeReminderPort
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
    fun loadDemoDataIsIdempotentAndClearDemoDataRemovesOnlyDemoItems() = runTest {
        val controllerScope = CoroutineScope(coroutineContext + SupervisorJob())
        try {
            val clock = FakeClockProvider()
            val eventRepo = FakeEventRepository()
            val noteRepo = FakeNoteRepository()
            val reminders = FakeReminderPort()
            val storage = FakeDocumentStorage()
            val dispatcher = UnconfinedTestDispatcher(testScheduler)

            val controller = TransferController(
                eventRepository = eventRepo,
                noteRepository = noteRepo,
                reminderPort = reminders,
                documentStorage = storage,
                icsCodec = IcsEventCodec(clock),
                csvCodec = CsvTransferCodec(),
                demoDataFactory = DemoDataFactory(clock),
                scope = controllerScope,
                computationDispatcher = dispatcher,
            )

            val feedback = mutableListOf<TransferFeedback>()
            backgroundScope.launch(dispatcher) {
                controller.feedback.collect { feedback.add(it) }
            }

            // Add one custom user note that must survive clearing demo data
            noteRepo.addNote(NoteDraft.plain("Prywatna", "Nie usuwać"))

            controller.loadDemoData()
            advanceUntilIdle()

            assertEquals(TransferFeedback.DemoLoaded(eventsCount = 5, notesCount = 3), feedback.last())
            assertEquals(5, eventRepo.observeEvents().first().size)
            assertEquals(4, noteRepo.observeNotes().first().size)
            assertTrue(reminders.scheduledEvents.isNotEmpty())
            assertTrue(noteRepo.observeNotes().first().any { it.hasAttachedEvents })

            // Second click must not duplicate demo items
            controller.loadDemoData()
            advanceUntilIdle()
            assertEquals(TransferFeedback.DemoAlreadyLoaded, feedback.last())
            assertEquals(5, eventRepo.observeEvents().first().size)

            // Clearing demo data keeps user's own note intact
            controller.clearDemoData()
            advanceUntilIdle()
            assertEquals(TransferFeedback.DemoCleared(eventsCount = 5, notesCount = 3), feedback.last())
            assertTrue(eventRepo.observeEvents().first().isEmpty())
            assertEquals(1, noteRepo.observeNotes().first().size)
            assertEquals("Prywatna", noteRepo.observeNotes().first().first().title)
        } finally {
            controllerScope.cancel()
        }
    }

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
                reminderPort = FakeReminderPort(),
                documentStorage = storage,
                icsCodec = IcsEventCodec(clock),
                csvCodec = CsvTransferCodec(),
                demoDataFactory = DemoDataFactory(clock),
                scope = controllerScope,
                computationDispatcher = dispatcher,
            )

            val feedback = mutableListOf<TransferFeedback>()
            backgroundScope.launch(dispatcher) {
                controller.feedback.collect { feedback.add(it) }
            }

            storage.files["content://demo/in.csv"] = "data;tytuł\r\n25.11.2026;Konferencja\r\n"
            controller.importCsv("content://demo/in.csv", preferNotesWhenAmbiguous = false)
            advanceUntilIdle()

            assertEquals(TransferFeedback.ImportedEvents(1), feedback.last())
            assertEquals("Konferencja", eventRepo.observeEvents().first().first().title)

            controller.exportIcs("content://demo/out.ics")
            advanceUntilIdle()

            assertEquals(TransferFeedback.ExportedEvents(1), feedback.last())
            assertTrue(storage.files["content://demo/out.ics"].orEmpty().contains("SUMMARY:Konferencja"))
        } finally {
            controllerScope.cancel()
        }
    }
}
