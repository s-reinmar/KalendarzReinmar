package com.example.data.preferences

import com.example.domain.model.ColorHarmonyRule
import com.example.domain.model.ColorWheelSnapshot
import com.example.domain.model.HsvColor
import com.example.domain.model.PaletteId
import com.example.domain.model.SavedColorPalette
import com.example.domain.model.ThemeSurfaceMode
import com.example.domain.service.ColorHarmonyEngine
import kotlinx.serialization.Serializable

@Serializable
data class HsvColorDto(
    val hue: Float,
    val saturation: Float,
    val value: Float,
)

@Serializable
data class ColorWheelSnapshotDto(
    val ruleName: String,
    val swatches: List<HsvColorDto>,
    val selectedSwatchIndex: Int = ColorHarmonyEngine.BASE_SWATCH_INDEX,
    val applyToAppTheme: Boolean = true,
    val surfaceModeName: String = ThemeSurfaceMode.COLORFUL.name,
)

@Serializable
data class SavedColorPaletteDto(
    val id: String,
    val name: String,
    val ruleName: String,
    val swatches: List<HsvColorDto>,
    val createdAtMillis: Long,
    val surfaceModeName: String = ThemeSurfaceMode.COLORFUL.name,
)

@Serializable
data class SavedPalettesEnvelopeDto(
    val palettes: List<SavedColorPaletteDto> = emptyList(),
)

internal fun HsvColor.toDto(): HsvColorDto = HsvColorDto(
    hue = hue,
    saturation = saturation,
    value = value,
)

internal fun HsvColorDto.toDomain(): HsvColor = HsvColor.of(
    hue = hue,
    saturation = saturation,
    value = value,
)

internal fun ColorWheelSnapshot.toDto(): ColorWheelSnapshotDto {
    val safeSwatches = ColorHarmonyEngine.normalizeSwatches(swatches)
    return ColorWheelSnapshotDto(
        ruleName = rule.name,
        swatches = safeSwatches.map(HsvColor::toDto),
        selectedSwatchIndex = selectedSwatchIndex.coerceIn(0, safeSwatches.lastIndex),
        applyToAppTheme = applyToAppTheme,
        surfaceModeName = surfaceMode.name,
    )
}

internal fun ColorWheelSnapshotDto.toDomainOrDefault(): ColorWheelSnapshot {
    val rule = parseHarmonyRule(ruleName)
    val domainSwatches = swatches.map(HsvColorDto::toDomain)
    if (!ColorHarmonyEngine.isValidSwatchCount(domainSwatches.size)) {
        return ColorHarmonyEngine.defaultSnapshot()
    }
    return ColorWheelSnapshot(
        rule = rule,
        swatches = domainSwatches,
        selectedSwatchIndex = selectedSwatchIndex.coerceIn(0, domainSwatches.lastIndex),
        applyToAppTheme = applyToAppTheme,
        surfaceMode = parseSurfaceMode(surfaceModeName),
    )
}

internal fun SavedColorPalette.toDto(): SavedColorPaletteDto {
    val safeSwatches = ColorHarmonyEngine.normalizeSwatches(swatches)
    return SavedColorPaletteDto(
        id = id.value,
        name = name,
        ruleName = rule.name,
        swatches = safeSwatches.map(HsvColor::toDto),
        createdAtMillis = createdAtMillis,
        surfaceModeName = surfaceMode.name,
    )
}

internal fun SavedColorPaletteDto.toDomainOrNull(): SavedColorPalette? {
    val paletteId = PaletteId(id)
    val cleanName = name.trim()
    val domainSwatches = swatches.map(HsvColorDto::toDomain)
    if (!paletteId.isValid || cleanName.isEmpty() || !ColorHarmonyEngine.isValidSwatchCount(domainSwatches.size)) {
        return null
    }
    return SavedColorPalette(
        id = paletteId,
        name = cleanName,
        rule = parseHarmonyRule(ruleName),
        swatches = domainSwatches,
        createdAtMillis = createdAtMillis,
        surfaceMode = parseSurfaceMode(surfaceModeName),
    )
}

private fun parseHarmonyRule(raw: String): ColorHarmonyRule =
    ColorHarmonyRule.entries.firstOrNull { it.name == raw } ?: ColorHarmonyRule.ANALOGOUS

private fun parseSurfaceMode(raw: String): ThemeSurfaceMode =
    ThemeSurfaceMode.entries.firstOrNull { it.name == raw } ?: ThemeSurfaceMode.COLORFUL
