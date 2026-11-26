package com.example.feature.settings

import com.example.domain.model.AppPreferences
import com.example.domain.model.ColorHarmonyRule
import com.example.domain.model.ColorWheelSnapshot
import com.example.domain.model.LocalDatabaseStats
import com.example.domain.model.PaletteId
import com.example.domain.model.ReminderTime
import com.example.domain.model.SavedColorPalette
import com.example.domain.model.ThemeSurfaceMode
import com.example.domain.port.AppPreferencesRepository
import com.example.domain.port.ClockProvider
import com.example.domain.port.ColorPaletteRepository
import com.example.domain.port.EventRepository
import com.example.domain.port.NoteRepository
import com.example.domain.service.ColorHarmonyEngine
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsController(
    private val repository: ColorPaletteRepository,
    private val clock: ClockProvider,
    private val scope: CoroutineScope,
    private val appPreferencesRepository: AppPreferencesRepository? = null,
    private val eventRepository: EventRepository? = null,
    private val noteRepository: NoteRepository? = null,
) {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _feedback = MutableSharedFlow<SettingsFeedback>(extraBufferCapacity = 4)
    val feedback: SharedFlow<SettingsFeedback> = _feedback.asSharedFlow()

    private var hasHydratedSnapshot = false
    private var hasLocalSnapshotMutation = false
    private var lastPersistedSnapshot: ColorWheelSnapshot? = null
    private var debouncedSaveJob: Job? = null

    init {
        observeColorPalettes()
        observePreferences()
        observeDatabaseStats()
    }

    private fun observeColorPalettes() {
        scope.launch {
            repository.observeState().collect { persisted ->
                _uiState.update { current ->
                    val isExternalRestore = hasHydratedSnapshot &&
                        debouncedSaveJob?.isActive != true &&
                        lastPersistedSnapshot != null &&
                        persisted.activeSnapshot != lastPersistedSnapshot
                    val shouldHydrateActive =
                        (!hasHydratedSnapshot && !hasLocalSnapshotMutation) || isExternalRestore
                    hasHydratedSnapshot = true
                    if (shouldHydrateActive) {
                        lastPersistedSnapshot = persisted.activeSnapshot
                        val hydratedSwatches = ColorHarmonyEngine.normalizeSwatches(
                            persisted.activeSnapshot.swatches,
                        )
                        current.copy(
                            applyToAppTheme = persisted.activeSnapshot.applyToAppTheme,
                            surfaceMode = persisted.activeSnapshot.surfaceMode,
                            activeRule = persisted.activeSnapshot.rule,
                            swatches = hydratedSwatches,
                            selectedSwatchIndex = persisted.activeSnapshot.selectedSwatchIndex
                                .coerceIn(0, hydratedSwatches.lastIndex),
                            savedPalettes = persisted.savedPalettes,
                        )
                    } else {
                        current.copy(savedPalettes = persisted.savedPalettes)
                    }
                }
            }
        }
    }

    private fun observePreferences() {
        val prefsRepo = appPreferencesRepository ?: return
        scope.launch {
            prefsRepo.observePreferences().collect { prefs ->
                _uiState.update { it.copy(preferences = prefs) }
            }
        }
    }

    private fun observeDatabaseStats() {
        val eventsFlow = eventRepository?.observeEvents() ?: flowOf(emptyList())
        val notesFlow = noteRepository?.observeNotes() ?: flowOf(emptyList())
        val palettesCountFlow = repository.observeState().map { it.savedPalettes.size }

        scope.launch {
            combine(eventsFlow, notesFlow, palettesCountFlow) { events, notes, palettesCount ->
                LocalDatabaseStats.compute(
                    events = events,
                    notes = notes,
                    palettesCount = palettesCount,
                )
            }.collect { stats ->
                _uiState.update { it.copy(databaseStats = stats) }
            }
        }
    }

    fun openDefaultReminderPicker() {
        _uiState.update { it.copy(isDefaultReminderPickerVisible = true) }
    }

    fun dismissDefaultReminderPicker() {
        _uiState.update { it.copy(isDefaultReminderPickerVisible = false) }
    }

    fun updateDefaultReminderTime(time: ReminderTime) {
        val updated = _uiState.value.preferences.copy(defaultReminderTime = time)
        _uiState.update { current ->
            current.copy(
                isDefaultReminderPickerVisible = false,
                preferences = updated,
            )
        }
        val prefsRepo = appPreferencesRepository ?: return
        scope.launch {
            prefsRepo.updatePreferences(updated)
        }
    }

    fun updateDefaultSnoozeMinutes(minutes: Int) {
        val safeMinutes = AppPreferences.normalizeSnoozeMinutes(minutes)
        val updated = _uiState.value.preferences.copy(defaultSnoozeMinutes = safeMinutes)
        _uiState.update { current ->
            current.copy(preferences = updated)
        }
        val prefsRepo = appPreferencesRepository ?: return
        scope.launch {
            prefsRepo.updatePreferences(updated)
        }
    }

    fun setWeekStartsOnMonday(startsOnMonday: Boolean) {
        val updated = _uiState.value.preferences.copy(weekStartsOnMonday = startsOnMonday)
        _uiState.update { current ->
            current.copy(preferences = updated)
        }
        val prefsRepo = appPreferencesRepository ?: return
        scope.launch {
            prefsRepo.updatePreferences(updated)
        }
    }

    fun toggleColorWheelExpanded() {
        _uiState.update { it.copy(isColorWheelExpanded = !it.isColorWheelExpanded) }
    }

    fun setApplyToAppTheme(enabled: Boolean) {
        hasLocalSnapshotMutation = true
        _uiState.update { it.copy(applyToAppTheme = enabled) }
        persistCurrentSnapshotImmediately()
    }

    fun selectSurfaceMode(mode: ThemeSurfaceMode) {
        hasLocalSnapshotMutation = true
        _uiState.update { it.copy(surfaceMode = mode, applyToAppTheme = true) }
        persistCurrentSnapshotImmediately()
    }

    fun selectColorCount(count: Int) {
        val safeCount = ColorHarmonyEngine.clampColorCount(count)
        hasLocalSnapshotMutation = true
        _uiState.update { current ->
            val regenerated = ColorHarmonyEngine.generatePalette(
                rule = current.activeRule,
                baseColor = current.baseSwatch,
                currentSwatches = current.swatches,
                colorCount = safeCount,
            )
            current.copy(
                swatches = regenerated,
                selectedSwatchIndex = ColorHarmonyEngine.baseSwatchIndex(safeCount),
                applyToAppTheme = true,
            )
        }
        persistCurrentSnapshotImmediately()
    }

    fun resetToDefaultPalette() {
        val defaultSnapshot = ColorHarmonyEngine.defaultSnapshot()
        hasLocalSnapshotMutation = true
        _uiState.update { current ->
            current.copy(
                activeRule = defaultSnapshot.rule,
                swatches = defaultSnapshot.swatches,
                selectedSwatchIndex = defaultSnapshot.selectedSwatchIndex,
                applyToAppTheme = true,
                surfaceMode = defaultSnapshot.surfaceMode,
            )
        }
        persistCurrentSnapshotImmediately()
    }

    fun selectHarmonyRule(rule: ColorHarmonyRule) {
        hasLocalSnapshotMutation = true
        _uiState.update { current ->
            val regenerated = ColorHarmonyEngine.generatePalette(
                rule = rule,
                baseColor = current.baseSwatch,
                currentSwatches = current.swatches,
                colorCount = current.colorCount,
            )
            current.copy(
                activeRule = rule,
                swatches = regenerated,
                selectedSwatchIndex = current.selectedSwatchIndex.coerceIn(0, regenerated.lastIndex),
            )
        }
        persistCurrentSnapshotImmediately()
    }

    fun selectSwatch(index: Int) {
        hasLocalSnapshotMutation = true
        _uiState.update { current ->
            val safeIndex = index.coerceIn(0, current.swatches.lastIndex)
            current.copy(selectedSwatchIndex = safeIndex)
        }
        persistCurrentSnapshotImmediately()
    }

    fun updateSwatchOnWheel(swatchIndex: Int, hue: Float, saturation: Float) {
        hasLocalSnapshotMutation = true
        _uiState.update { current ->
            val safeIndex = swatchIndex.coerceIn(0, current.swatches.lastIndex)
            val updatedSwatches = ColorHarmonyEngine.updateSwatchOnWheel(
                rule = current.activeRule,
                currentSwatches = current.swatches,
                swatchIndex = safeIndex,
                newHue = hue,
                newSaturation = saturation,
            )
            current.copy(
                swatches = updatedSwatches,
                selectedSwatchIndex = safeIndex,
            )
        }
        scheduleDebouncedSnapshotSave()
    }

    fun updateActiveSwatchValue(newValue: Float) {
        hasLocalSnapshotMutation = true
        _uiState.update { current ->
            val updatedSwatches = ColorHarmonyEngine.updateSwatchValue(
                currentSwatches = current.swatches,
                swatchIndex = current.selectedSwatchIndex,
                newValue = newValue,
            )
            current.copy(swatches = updatedSwatches)
        }
        scheduleDebouncedSnapshotSave()
    }

    fun commitContinuousAdjustment() {
        persistCurrentSnapshotImmediately()
    }

    fun openSavePaletteDialog() {
        _uiState.update { it.copy(isSaveDialogVisible = true) }
    }

    fun dismissSavePaletteDialog() {
        _uiState.update { it.copy(isSaveDialogVisible = false) }
    }

    fun saveNamedPalette(rawName: String) {
        val cleanName = rawName.trim()
        if (cleanName.isEmpty()) return

        val snapshot = _uiState.value
        val now = clock.nowMillis()
        val palette = SavedColorPalette(
            id = PaletteId("palette_${now}_${UUID.randomUUID().toString().take(8)}"),
            name = cleanName,
            rule = snapshot.activeRule,
            swatches = ColorHarmonyEngine.normalizeSwatches(snapshot.swatches),
            createdAtMillis = now,
            surfaceMode = snapshot.surfaceMode,
        )

        _uiState.update { it.copy(isSaveDialogVisible = false) }
        scope.launch {
            repository.addSavedPalette(palette)
            _feedback.tryEmit(SettingsFeedback.PaletteSaved(cleanName))
        }
    }

    fun restoreSavedPalette(palette: SavedColorPalette) {
        hasLocalSnapshotMutation = true
        val restoredSwatches = ColorHarmonyEngine.normalizeSwatches(palette.swatches)
        _uiState.update { current ->
            current.copy(
                applyToAppTheme = true,
                surfaceMode = palette.surfaceMode,
                activeRule = palette.rule,
                swatches = restoredSwatches,
                selectedSwatchIndex = ColorHarmonyEngine.baseSwatchIndex(restoredSwatches.size),
            )
        }
        persistCurrentSnapshotImmediately()
        _feedback.tryEmit(SettingsFeedback.PaletteRestored(palette.name))
    }

    fun deleteSavedPalette(id: PaletteId) {
        scope.launch {
            repository.deleteSavedPalette(id)
        }
    }

    private fun scheduleDebouncedSnapshotSave() {
        debouncedSaveJob?.cancel()
        val snapshot = currentSnapshot()
        lastPersistedSnapshot = snapshot
        debouncedSaveJob = scope.launch {
            delay(AUTO_SAVE_DEBOUNCE_MILLIS)
            repository.saveCurrentSnapshot(snapshot)
        }
    }

    private fun persistCurrentSnapshotImmediately() {
        debouncedSaveJob?.cancel()
        val snapshot = currentSnapshot()
        lastPersistedSnapshot = snapshot
        scope.launch {
            repository.saveCurrentSnapshot(snapshot)
        }
    }

    private fun currentSnapshot(): ColorWheelSnapshot {
        val state = _uiState.value
        val safeSwatches = ColorHarmonyEngine.normalizeSwatches(state.swatches)
        return ColorWheelSnapshot(
            rule = state.activeRule,
            swatches = safeSwatches,
            selectedSwatchIndex = state.selectedSwatchIndex.coerceIn(0, safeSwatches.lastIndex),
            applyToAppTheme = state.applyToAppTheme,
            surfaceMode = state.surfaceMode,
        )
    }

    companion object {
        private const val AUTO_SAVE_DEBOUNCE_MILLIS = 150L
    }
}
