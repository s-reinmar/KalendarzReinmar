package com.example.feature

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
    fun noteCrudAndDialogStateTransitionsWorkWithoutAndroidDependencies() = runTest {
        val controllerScope = CoroutineScope(coroutineContext + SupervisorJob())
        try {
            val repo = FakeNoteRepository()
            val controller = NotesController(
                noteRepository = repo,
                scope = controllerScope,
            )
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                controller.uiState.collect()
            }
            advanceUntilIdle()

            controller.openCreateNote()
            advanceUntilIdle()
            assertEquals(NotesDialogState.Create, controller.uiState.value.dialog)

            // Blank content is ignored
            controller.createNote("Tytuł", "   ")
            advanceUntilIdle()
            assertTrue(controller.uiState.value.notes.isEmpty())

            controller.createNote("Zakupy", "Kawa, mleko")
            advanceUntilIdle()
            assertEquals(1, controller.uiState.value.notes.size)
            assertEquals(NotesDialogState.None, controller.uiState.value.dialog)

            val created = controller.uiState.value.notes.first()
            controller.updateNote(created, "Zakupy pilne", "Kawa ziarnista")
            advanceUntilIdle()
            assertEquals("Zakupy pilne", controller.uiState.value.notes.first().title)
            assertEquals("Kawa ziarnista", controller.uiState.value.notes.first().content)

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
