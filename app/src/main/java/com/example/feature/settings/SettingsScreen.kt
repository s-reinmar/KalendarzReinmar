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
import com.example.domain.model.SavedColorPalette
import com.example.domain.model.ThemeSurfaceMode
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
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("settings_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
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
