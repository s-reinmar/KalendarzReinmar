package com.example.domain.model

@JvmInline
value class PaletteId(val value: String) {
    val isValid: Boolean
        get() = value.isNotBlank()
}

data class SavedColorPalette(
    val id: PaletteId,
    val name: String,
    val rule: ColorHarmonyRule,
    val swatches: List<HsvColor>,
    val createdAtMillis: Long,
    val surfaceMode: ThemeSurfaceMode = ThemeSurfaceMode.COLORFUL,
)

data class ColorWheelSnapshot(
    val rule: ColorHarmonyRule,
    val swatches: List<HsvColor>,
    val selectedSwatchIndex: Int,
    val applyToAppTheme: Boolean = true,
    val surfaceMode: ThemeSurfaceMode = ThemeSurfaceMode.COLORFUL,
)

data class PersistedColorWheelState(
    val activeSnapshot: ColorWheelSnapshot,
    val savedPalettes: List<SavedColorPalette>,
)
