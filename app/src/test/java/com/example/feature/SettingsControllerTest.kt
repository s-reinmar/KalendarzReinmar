package com.example.feature

import com.example.domain.model.ColorHarmonyRule
import com.example.domain.model.ColorWheelSnapshot
import com.example.domain.model.HsvColor
import com.example.domain.model.PersistedColorWheelState
import com.example.domain.service.ColorHarmonyEngine
import com.example.fakes.FakeClockProvider
import com.example.fakes.FakeColorPaletteRepository
import com.example.feature.settings.SettingsController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsControllerTest {

    @Test
    fun hydratesPersistedSnapshotOnStartup() = runTest {
        val controllerScope = CoroutineScope(coroutineContext + SupervisorJob())
        try {
            val customSwatches = ColorHarmonyEngine.generatePalette(
                rule = ColorHarmonyRule.TRIAD,
                baseColor = HsvColor.of(45f, 0.9f, 0.85f),
            )
            val repository = FakeColorPaletteRepository(
                initialState = PersistedColorWheelState(
                    activeSnapshot = ColorWheelSnapshot(
                        rule = ColorHarmonyRule.TRIAD,
                        swatches = customSwatches,
                        selectedSwatchIndex = 1,
                    ),
                    savedPalettes = emptyList(),
                ),
            )
            val controller = SettingsController(
                repository = repository,
                clock = FakeClockProvider(),
                scope = controllerScope,
            )
            advanceUntilIdle()

            assertEquals(ColorHarmonyRule.TRIAD, controller.uiState.value.activeRule)
            assertEquals(1, controller.uiState.value.selectedSwatchIndex)
            assertEquals(customSwatches, controller.uiState.value.swatches)
        } finally {
            controllerScope.cancel()
        }
    }

    @Test
    fun switchingRuleDraggingAndValueSliderPersistAutomatically() = runTest {
        val controllerScope = CoroutineScope(coroutineContext + SupervisorJob())
        try {
            val repository = FakeColorPaletteRepository()
            val controller = SettingsController(
                repository = repository,
                clock = FakeClockProvider(),
                scope = controllerScope,
            )
            advanceUntilIdle()

            controller.selectHarmonyRule(ColorHarmonyRule.SQUARE)
            controller.updateSwatchOnWheel(swatchIndex = 2, hue = 120f, saturation = 0.8f)
            controller.updateActiveSwatchValue(0.55f)
            controller.commitContinuousAdjustment()
            advanceUntilIdle()

            val state = controller.uiState.value
            assertEquals(ColorHarmonyRule.SQUARE, state.activeRule)
            assertEquals(120f, state.baseSwatch.hue, 0.01f)
            assertEquals(0.55f, state.activeSwatch.value, 0.01f)
            assertTrue(repository.savedSnapshots.isNotEmpty())
            assertEquals(ColorHarmonyRule.SQUARE, repository.savedSnapshots.last().rule)
        } finally {
            controllerScope.cancel()
        }
    }

    @Test
    fun saveRestoreAndDeleteNamedPaletteWorkEndToEnd() = runTest {
        val controllerScope = CoroutineScope(coroutineContext + SupervisorJob())
        try {
            val repository = FakeColorPaletteRepository()
            val controller = SettingsController(
                repository = repository,
                clock = FakeClockProvider(),
                scope = controllerScope,
            )
            advanceUntilIdle()

            controller.selectHarmonyRule(ColorHarmonyRule.COMPOUND)
            controller.openSavePaletteDialog()
            assertTrue(controller.uiState.value.isSaveDialogVisible)

            controller.saveNamedPalette("Wieczorna harmonia")
            advanceUntilIdle()

            assertFalse(controller.uiState.value.isSaveDialogVisible)
            assertEquals(1, controller.uiState.value.savedPalettes.size)
            val saved = controller.uiState.value.savedPalettes.first()
            assertEquals("Wieczorna harmonia", saved.name)
            assertEquals(ColorHarmonyRule.COMPOUND, saved.rule)

            controller.selectHarmonyRule(ColorHarmonyRule.SHADES)
            assertEquals(ColorHarmonyRule.SHADES, controller.uiState.value.activeRule)

            controller.restoreSavedPalette(saved)
            advanceUntilIdle()
            assertEquals(ColorHarmonyRule.COMPOUND, controller.uiState.value.activeRule)

            controller.deleteSavedPalette(saved.id)
            advanceUntilIdle()
            assertTrue(controller.uiState.value.savedPalettes.isEmpty())
        } finally {
            controllerScope.cancel()
        }
    }

    @Test
    fun toggleApplyToAppThemeAndResetPersistCorrectly() = runTest {
        val controllerScope = CoroutineScope(coroutineContext + SupervisorJob())
        try {
            val repository = FakeColorPaletteRepository()
            val controller = SettingsController(
                repository = repository,
                clock = FakeClockProvider(),
                scope = controllerScope,
            )
            advanceUntilIdle()

            assertTrue(controller.uiState.value.applyToAppTheme)

            controller.setApplyToAppTheme(false)
            advanceUntilIdle()
            assertFalse(controller.uiState.value.applyToAppTheme)
            assertFalse(repository.savedSnapshots.last().applyToAppTheme)

            controller.selectHarmonyRule(ColorHarmonyRule.TRIAD)
            controller.resetToDefaultPalette()
            advanceUntilIdle()
            assertEquals(ColorHarmonyRule.ANALOGOUS, controller.uiState.value.activeRule)
            assertTrue(controller.uiState.value.applyToAppTheme)
        } finally {
            controllerScope.cancel()
        }
    }

    @Test
    fun selectingColorCountTwoToFiveAndSurfaceModeUpdatesAndPersists() = runTest {
        val controllerScope = CoroutineScope(coroutineContext + SupervisorJob())
        try {
            val repository = FakeColorPaletteRepository()
            val controller = SettingsController(
                repository = repository,
                clock = FakeClockProvider(),
                scope = controllerScope,
            )
            advanceUntilIdle()

            controller.selectHarmonyRule(ColorHarmonyRule.TRIAD)
            controller.selectColorCount(3)
            controller.selectSurfaceMode(com.example.domain.model.ThemeSurfaceMode.LIGHT)
            advanceUntilIdle()

            assertEquals(3, controller.uiState.value.colorCount)
            assertEquals(3, controller.uiState.value.swatches.size)
            assertEquals(1, controller.uiState.value.baseSwatchIndex)
            assertEquals(
                com.example.domain.model.ThemeSurfaceMode.LIGHT,
                controller.uiState.value.surfaceMode,
            )
            assertEquals(3, repository.savedSnapshots.last().swatches.size)
            assertEquals(
                com.example.domain.model.ThemeSurfaceMode.LIGHT,
                repository.savedSnapshots.last().surfaceMode,
            )

            controller.selectColorCount(2)
            advanceUntilIdle()
            assertEquals(2, controller.uiState.value.swatches.size)
            assertEquals(0, controller.uiState.value.baseSwatchIndex)
        } finally {
            controllerScope.cancel()
        }
    }

    @Test
    fun preferencesAndLocalDatabaseStatsStaySynchronized() = runTest {
        val controllerScope = CoroutineScope(coroutineContext + SupervisorJob())
        try {
            val paletteRepo = FakeColorPaletteRepository()
            val prefsRepo = com.example.fakes.FakeAppPreferencesRepository()
            val eventRepo = com.example.fakes.FakeEventRepository()
            val noteRepo = com.example.fakes.FakeNoteRepository()
            val controller = SettingsController(
                repository = paletteRepo,
                clock = FakeClockProvider(),
                scope = controllerScope,
                appPreferencesRepository = prefsRepo,
                eventRepository = eventRepo,
                noteRepository = noteRepo,
            )
            advanceUntilIdle()

            eventRepo.addEvent(
                com.example.domain.model.EventDraft(
                    title = "Zadanie 1",
                    dateKey = requireNotNull(com.example.domain.model.DateKey.parseOrNull("2026-11-25")),
                    details = com.example.domain.model.EventTypeDetails.Task(isCompleted = false),
                    reminderTime = com.example.domain.model.ReminderTime.fromHourMinute(9, 0),
                ),
            )
            noteRepo.addNote(
                com.example.domain.model.NoteDraft(
                    title = "Notatka",
                    document = com.example.domain.model.NoteRichDocument(text = "Treść"),
                ),
            )
            controller.updateDefaultReminderTime(com.example.domain.model.ReminderTime.fromHourMinute(7, 30))
            controller.updateDefaultSnoozeMinutes(10)
            controller.setWeekStartsOnMonday(false)
            advanceUntilIdle()

            val state = controller.uiState.value
            assertEquals(
                com.example.domain.model.ReminderTime.fromHourMinute(7, 30),
                state.preferences.defaultReminderTime,
            )
            assertEquals(10, state.preferences.defaultSnoozeMinutes)
            assertFalse(state.preferences.weekStartsOnMonday)
            assertEquals(1, state.databaseStats.totalEvents)
            assertEquals(1, state.databaseStats.pendingTasks)
            assertEquals(1, state.databaseStats.eventsWithReminder)
            assertEquals(1, state.databaseStats.totalNotes)
        } finally {
            controllerScope.cancel()
        }
    }
}
