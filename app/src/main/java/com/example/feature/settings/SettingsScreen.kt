package com.example.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.domain.model.ColorHarmonyRule
import com.example.domain.model.PaletteId
import com.example.domain.model.ReminderTime
import com.example.domain.model.SavedColorPalette
import com.example.domain.model.ThemeSurfaceMode
import com.example.feature.calendar.ReminderTimePickerDialog
import com.example.ui.theme.KalendarzReinmarTheme

@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onToggleColorWheelExpanded: () -> Unit,
    onToggleApplyToAppTheme: (Boolean) -> Unit,
    onSelectSurfaceMode: (ThemeSurfaceMode) -> Unit,
    onSelectColorCount: (Int) -> Unit,
    onResetToDefaultPalette: () -> Unit,
    onSelectHarmonyRule: (ColorHarmonyRule) -> Unit,
    onSwatchDragged: (index: Int, hue: Float, saturation: Float) -> Unit,
    onSelectSwatch: (Int) -> Unit,
    onActiveSwatchValueChange: (Float) -> Unit,
    onCommitContinuousAdjustment: () -> Unit,
    onOpenSavePaletteDialog: () -> Unit,
    onDismissSavePaletteDialog: () -> Unit,
    onConfirmSavePalette: (String) -> Unit,
    onRestoreSavedPalette: (SavedColorPalette) -> Unit,
    onDeleteSavedPalette: (PaletteId) -> Unit,
    onOpenDefaultReminderPicker: () -> Unit = {},
    onDismissDefaultReminderPicker: () -> Unit = {},
    onConfirmDefaultReminderTime: (ReminderTime) -> Unit = {},
    onSelectSnoozeMinutes: (Int) -> Unit = {},
    onSelectWeekStartsOnMonday: (Boolean) -> Unit = {},
    isTransferBusy: Boolean = false,
    onExportJsonBackup: () -> Unit = {},
    onImportJsonBackup: () -> Unit = {},
    onExportIcs: () -> Unit = {},
    onImportIcs: () -> Unit = {},
    onExportCsv: () -> Unit = {},
    onImportCsv: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp)
            .testTag("settings_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        PreferencesSettingsCard(
            preferences = uiState.preferences,
            onOpenDefaultReminderPicker = onOpenDefaultReminderPicker,
            onSelectSnoozeMinutes = onSelectSnoozeMinutes,
            onSelectWeekStartsOnMonday = onSelectWeekStartsOnMonday,
        )

        BackupSettingsCard(
            isBusy = isTransferBusy,
            onExportJsonBackup = onExportJsonBackup,
            onImportJsonBackup = onImportJsonBackup,
            onExportIcs = onExportIcs,
            onImportIcs = onImportIcs,
            onExportCsv = onExportCsv,
            onImportCsv = onImportCsv,
        )

        LocalStatsSettingsCard(
            stats = uiState.databaseStats,
        )

        ColorWheelSettingsCard(
            uiState = uiState,
            onToggleExpanded = onToggleColorWheelExpanded,
            onToggleApplyToAppTheme = onToggleApplyToAppTheme,
            onSelectSurfaceMode = onSelectSurfaceMode,
            onSelectColorCount = onSelectColorCount,
            onResetToDefaultPalette = onResetToDefaultPalette,
            onSelectHarmonyRule = onSelectHarmonyRule,
            onSwatchDragged = onSwatchDragged,
            onSelectSwatch = onSelectSwatch,
            onActiveSwatchValueChange = onActiveSwatchValueChange,
            onCommitContinuousAdjustment = onCommitContinuousAdjustment,
            onOpenSavePaletteDialog = onOpenSavePaletteDialog,
            onRestoreSavedPalette = onRestoreSavedPalette,
            onDeleteSavedPalette = onDeleteSavedPalette,
        )
    }

    if (uiState.isSaveDialogVisible) {
        SavePaletteDialog(
            activeRule = uiState.activeRule,
            swatches = uiState.swatches,
            onDismiss = onDismissSavePaletteDialog,
            onConfirmSave = onConfirmSavePalette,
        )
    }

    if (uiState.isDefaultReminderPickerVisible) {
        ReminderTimePickerDialog(
            initialTime = uiState.preferences.defaultReminderTime,
            onDismiss = onDismissDefaultReminderPicker,
            onConfirm = onConfirmDefaultReminderTime,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    KalendarzReinmarTheme {
        Surface {
            SettingsScreen(
                uiState = SettingsUiState(),
                onToggleColorWheelExpanded = {},
                onToggleApplyToAppTheme = {},
                onSelectSurfaceMode = {},
                onSelectColorCount = {},
                onResetToDefaultPalette = {},
                onSelectHarmonyRule = {},
                onSwatchDragged = { _, _, _ -> },
                onSelectSwatch = {},
                onActiveSwatchValueChange = {},
                onCommitContinuousAdjustment = {},
                onOpenSavePaletteDialog = {},
                onDismissSavePaletteDialog = {},
                onConfirmSavePalette = {},
                onRestoreSavedPalette = {},
                onDeleteSavedPalette = {},
            )
        }
    }
}
