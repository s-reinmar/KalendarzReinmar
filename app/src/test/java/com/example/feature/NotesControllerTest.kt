package com.example.feature

import com.example.domain.model.DateKey
import com.example.domain.model.EventDraft
import com.example.domain.model.NoteDraft
import com.example.domain.model.NoteRichDocument
import com.example.domain.model.NoteRichSpan
import com.example.domain.model.NoteTextFormat
import com.example.domain.model.NoteTextSize
import com.example.fakes.FakeEventRepository
import com.example.fakes.FakeNoteRepository
import com.example.feature.notes.NotesController
import com.example.feature.notes.NotesDialogState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NotesControllerTest {

    @Test
    fun noteCrudRichFormattingAndEventAttachmentsWorkWithoutAndroidDependencies() = runTest {
        val controllerScope = CoroutineScope(coroutineContext + SupervisorJob())
        try {
            val noteRepo = FakeNoteRepository()
            val eventRepo = FakeEventRepository()
            val calendarEvent = requireNotNull(
                eventRepo.addEvent(
                    EventDraft(
                        title = "Spotkanie projektowe",
                        dateKey = requireNotNull(DateKey.parseOrNull("2026-11-26")),
                    )
                )
            )
            val controller = NotesController(
                noteRepository = noteRepo,
                eventRepository = eventRepo,
                scope = controllerScope,
            )
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                controller.uiState.collect()
            }
            advanceUntilIdle()

            assertEquals(1, controller.uiState.value.availableEvents.size)
            assertEquals(calendarEvent, controller.uiState.value.eventsById[calendarEvent.id])

            controller.openCreateNote()
            advanceUntilIdle()
            assertEquals(NotesDialogState.Create, controller.uiState.value.dialog)

            // Blank content is ignored
            controller.createNote(NoteDraft.plain("Tytuł", "   "))
            advanceUntilIdle()
            assertTrue(controller.uiState.value.notes.isEmpty())

            val richDoc = NoteRichDocument(
                text = "Lista\n☐ Kawa\n☐ Mleko",
                spans = listOf(
                    NoteRichSpan(0, 5, NoteTextFormat(bold = true, size = NoteTextSize.HEADING_2)),
                ),
            )
            controller.createNote(
                NoteDraft(
                    title = "Zakupy",
                    document = richDoc,
                    attachedEventIds = listOf(calendarEvent.id),
                )
            )
            advanceUntilIdle()
            assertEquals(1, controller.uiState.value.notes.size)
            assertEquals(NotesDialogState.None, controller.uiState.value.dialog)
            assertEquals(listOf(calendarEvent.id), controller.uiState.value.notes.first().attachedEventIds)
            assertEquals(1, controller.uiState.value.notes.first().richSpans.size)

            // Toggle checklist line persists checked item
            controller.toggleChecklistLine(controller.uiState.value.notes.first(), lineIndex = 1)
            advanceUntilIdle()
            assertTrue(controller.uiState.value.notes.first().content.contains("☑ Kawa"))

            val created = controller.uiState.value.notes.first()
            controller.updateNote(
                created,
                NoteDraft.plain("Zakupy pilne", "Kawa ziarnista", attachedEventIds = emptyList()),
            )
            advanceUntilIdle()
            assertEquals("Zakupy pilne", controller.uiState.value.notes.first().title)
            assertEquals("Kawa ziarnista", controller.uiState.value.notes.first().content)
            assertTrue(controller.uiState.value.notes.first().attachedEventIds.isEmpty())

            controller.requestDeleteNote(controller.uiState.value.notes.first())
            advanceUntilIdle()
            assertTrue(controller.uiState.value.dialog is NotesDialogState.ConfirmDelete)

            controller.confirmDeleteNote(controller.uiState.value.notes.first())
            advanceUntilIdle()
            assertTrue(controller.uiState.value.notes.isEmpty())
        } finally {
            controllerScope.cancel()
        }
    }
}
