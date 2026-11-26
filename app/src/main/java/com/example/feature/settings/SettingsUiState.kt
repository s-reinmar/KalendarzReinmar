package com.example.feature.settings

import com.example.domain.model.AppPreferences
import com.example.domain.model.ColorHarmonyRule
import com.example.domain.model.HsvColor
import com.example.domain.model.LocalDatabaseStats
import com.example.domain.model.SavedColorPalette
import com.example.domain.model.ThemeSurfaceMode
import com.example.domain.service.ColorHarmonyEngine

data class SettingsUiState(
    val isColorWheelExpanded: Boolean = true,
    val applyToAppTheme: Boolean = true,
    val surfaceMode: ThemeSurfaceMode = ThemeSurfaceMode.COLORFUL,
    val activeRule: ColorHarmonyRule = ColorHarmonyRule.ANALOGOUS,
    val swatches: List<HsvColor> = ColorHarmonyEngine.defaultSnapshot().swatches,
    val selectedSwatchIndex: Int = ColorHarmonyEngine.BASE_SWATCH_INDEX,
    val savedPalettes: List<SavedColorPalette> = emptyList(),
    val isSaveDialogVisible: Boolean = false,
    val preferences: AppPreferences = AppPreferences(),
    val databaseStats: LocalDatabaseStats = LocalDatabaseStats(),
    val isDefaultReminderPickerVisible: Boolean = false,
) {
    val colorCount: Int
        get() = swatches.size.coerceIn(
            ColorHarmonyEngine.MIN_SWATCH_COUNT,
            ColorHarmonyEngine.MAX_SWATCH_COUNT,
        )

    val baseSwatchIndex: Int
        get() = ColorHarmonyEngine.baseSwatchIndex(colorCount)

    val activeSwatch: HsvColor
        get() = swatches.getOrElse(selectedSwatchIndex) { ColorHarmonyEngine.defaultBaseColor }

    val baseSwatch: HsvColor
        get() = swatches.getOrElse(baseSwatchIndex) { ColorHarmonyEngine.defaultBaseColor }
}

sealed interface SettingsFeedback {
    data class PaletteSaved(val paletteName: String) : SettingsFeedback
    data class PaletteRestored(val paletteName: String) : SettingsFeedback
}
