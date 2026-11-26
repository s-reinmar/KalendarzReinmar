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
        } finally {
            controllerScope.cancel()
        }
    }
}
