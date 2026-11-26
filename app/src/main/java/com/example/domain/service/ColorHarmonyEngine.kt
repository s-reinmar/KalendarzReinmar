package com.example.domain.service

import com.example.domain.model.ColorHarmonyRule
import com.example.domain.model.ColorWheelSnapshot
import com.example.domain.model.HsvColor
import com.example.domain.model.ThemeSurfaceMode

object ColorHarmonyEngine {
    const val MIN_SWATCH_COUNT: Int = 2
    const val MAX_SWATCH_COUNT: Int = 5
    const val SWATCH_COUNT: Int = MAX_SWATCH_COUNT
    const val BASE_SWATCH_INDEX: Int = 2

    val supportedColorCounts: List<Int> = (MIN_SWATCH_COUNT..MAX_SWATCH_COUNT).toList()

    val defaultBaseColor: HsvColor = HsvColor.of(
        hue = 162f,
        saturation = 0.68f,
        value = 0.72f,
    )

    fun clampColorCount(count: Int): Int = count.coerceIn(MIN_SWATCH_COUNT, MAX_SWATCH_COUNT)

    fun isValidSwatchCount(count: Int): Boolean = count in MIN_SWATCH_COUNT..MAX_SWATCH_COUNT

    fun baseSwatchIndex(colorCount: Int): Int = when (clampColorCount(colorCount)) {
        2 -> 0
        3 -> 1
        4 -> 1
        else -> BASE_SWATCH_INDEX
    }

    fun defaultSnapshot(colorCount: Int = SWATCH_COUNT): ColorWheelSnapshot {
        val safeCount = clampColorCount(colorCount)
        return ColorWheelSnapshot(
            rule = ColorHarmonyRule.ANALOGOUS,
            swatches = generatePalette(
                rule = ColorHarmonyRule.ANALOGOUS,
                baseColor = defaultBaseColor,
                colorCount = safeCount,
            ),
            selectedSwatchIndex = baseSwatchIndex(safeCount),
            applyToAppTheme = true,
            surfaceMode = ThemeSurfaceMode.COLORFUL,
        )
    }

    fun generatePalette(
        rule: ColorHarmonyRule,
        baseColor: HsvColor,
        currentSwatches: List<HsvColor> = emptyList(),
        colorCount: Int = currentSwatches.size.takeIf(::isValidSwatchCount) ?: SWATCH_COUNT,
    ): List<HsvColor> {
        val safeCount = clampColorCount(colorCount)
        val base = baseColor.normalized()
        return when (rule) {
            ColorHarmonyRule.CUSTOM -> {
                if (currentSwatches.size == safeCount) {
                    currentSwatches.map(HsvColor::normalized)
                } else if (isValidSwatchCount(currentSwatches.size)) {
                    resizeCustomSwatches(
                        currentSwatches = currentSwatches.map(HsvColor::normalized),
                        baseColor = base,
                        targetCount = safeCount,
                    )
                } else {
                    generateFromSpecs(base, specsFor(ColorHarmonyRule.ANALOGOUS, safeCount))
                }
            }
            ColorHarmonyRule.SHADES -> generateShades(base, safeCount)
            ColorHarmonyRule.MONOCHROMATIC -> generateMonochromatic(base, safeCount)
            else -> generateFromSpecs(base, specsFor(rule, safeCount))
        }
    }

    fun updateSwatchOnWheel(
        rule: ColorHarmonyRule,
        currentSwatches: List<HsvColor>,
        swatchIndex: Int,
        newHue: Float,
        newSaturation: Float,
    ): List<HsvColor> {
        val safeSwatches = normalizeSwatches(currentSwatches)
        val count = safeSwatches.size
        val safeIndex = swatchIndex.coerceIn(0, count - 1)
        val baseIndex = baseSwatchIndex(count)
        val normalizedHue = HsvColor.normalizeHue(newHue)
        val normalizedSat = newSaturation.coerceIn(0f, 1f)

        return when (rule) {
            ColorHarmonyRule.CUSTOM -> {
                safeSwatches.mapIndexed { index, color ->
                    if (index == safeIndex) {
                        color.withHueSaturation(normalizedHue, normalizedSat)
                    } else {
                        color
                    }
                }
            }
            ColorHarmonyRule.SHADES -> {
                safeSwatches.map { color ->
                    color.withHueSaturation(normalizedHue, normalizedSat)
                }
            }
            ColorHarmonyRule.MONOCHROMATIC -> {
                if (safeIndex == baseIndex) {
                    val base = safeSwatches[baseIndex].withHueSaturation(normalizedHue, normalizedSat)
                    val regenerated = generateMonochromatic(base, count)
                    regenerated.mapIndexed { index, generated ->
                        generated.withValue(safeSwatches[index].value)
                    }
                } else {
                    safeSwatches.mapIndexed { index, color ->
                        if (index == safeIndex) {
                            color.withHueSaturation(normalizedHue, normalizedSat)
                        } else {
                            color.withHueSaturation(normalizedHue, color.saturation)
                        }
                    }
                }
            }
            else -> {
                val specs = specsFor(rule, count)
                val draggedSpec = specs[safeIndex]
                val impliedBaseHue = HsvColor.normalizeHue(normalizedHue - draggedSpec.hueOffset)
                val impliedBaseSat = if (safeIndex == baseIndex || draggedSpec.saturationScale <= 0f) {
                    normalizedSat
                } else {
                    (normalizedSat / draggedSpec.saturationScale).coerceIn(0f, 1f)
                }
                specs.mapIndexed { index, spec ->
                    val swatchSat = if (index == safeIndex) {
                        normalizedSat
                    } else {
                        (impliedBaseSat * spec.saturationScale).coerceIn(0.05f, 1f)
                    }
                    HsvColor.of(
                        hue = impliedBaseHue + spec.hueOffset,
                        saturation = swatchSat,
                        value = safeSwatches[index].value,
                    )
                }
            }
        }
    }

    fun updateSwatchValue(
        currentSwatches: List<HsvColor>,
        swatchIndex: Int,
        newValue: Float,
    ): List<HsvColor> {
        val safeSwatches = normalizeSwatches(currentSwatches)
        val safeIndex = swatchIndex.coerceIn(0, safeSwatches.size - 1)
        val clampedValue = newValue.coerceIn(0f, 1f)
        return safeSwatches.mapIndexed { index, color ->
            if (index == safeIndex) color.withValue(clampedValue) else color
        }
    }

    fun normalizeSwatches(swatches: List<HsvColor>): List<HsvColor> {
        if (isValidSwatchCount(swatches.size)) {
            return swatches.map(HsvColor::normalized)
        }
        return defaultSnapshot().swatches
    }

    private fun resizeCustomSwatches(
        currentSwatches: List<HsvColor>,
        baseColor: HsvColor,
        targetCount: Int,
    ): List<HsvColor> {
        val generatedFallback = generateFromSpecs(
            base = baseColor,
            specs = specsFor(ColorHarmonyRule.ANALOGOUS, targetCount),
        )
        val oldBaseIdx = baseSwatchIndex(currentSwatches.size)
        val newBaseIdx = baseSwatchIndex(targetCount)
        val nonBaseExisting = currentSwatches.filterIndexed { index, _ -> index != oldBaseIdx }
        val result = MutableList(targetCount) { generatedFallback[it] }
        result[newBaseIdx] = currentSwatches[oldBaseIdx]

        var cursor = 0
        for (idx in 0 until targetCount) {
            if (idx == newBaseIdx) continue
            if (cursor < nonBaseExisting.size) {
                result[idx] = nonBaseExisting[cursor]
                cursor++
            }
        }
        return result
    }

    private fun generateFromSpecs(
        base: HsvColor,
        specs: List<SwatchHarmonySpec>,
    ): List<HsvColor> = specs.map { spec ->
        val sat = (base.saturation * spec.saturationScale).coerceIn(0.08f, 1f)
        val value = (base.value * spec.valueScale + spec.valueOffset).coerceIn(0.12f, 1f)
        HsvColor.of(
            hue = base.hue + spec.hueOffset,
            saturation = sat,
            value = value,
        )
    }

    private fun generateShades(base: HsvColor, count: Int): List<HsvColor> {
        val values = when (clampColorCount(count)) {
            2 -> listOf(
                base.value,
                (base.value + (1f - base.value) * 0.65f).coerceIn(0.32f, 1f),
            )
            3 -> listOf(
                (base.value * 0.52f).coerceIn(0.18f, 0.94f),
                base.value,
                (base.value + (1f - base.value) * 0.72f).coerceIn(0.42f, 1f),
            )
            4 -> listOf(
                (base.value * 0.44f).coerceIn(0.15f, 0.94f),
                base.value,
                (base.value + (1f - base.value) * 0.48f).coerceIn(0.35f, 0.98f),
                (base.value + (1f - base.value) * 0.84f).coerceIn(0.50f, 1f),
            )
            else -> listOf(
                (base.value * 0.30f).coerceIn(0.12f, 0.95f),
                (base.value * 0.62f).coerceIn(0.24f, 0.96f),
                base.value,
                (base.value + (1f - base.value) * 0.45f).coerceIn(0.35f, 0.98f),
                (base.value + (1f - base.value) * 0.82f).coerceIn(0.48f, 1f),
            )
        }
        return values.map { shadeValue ->
            HsvColor.of(
                hue = base.hue,
                saturation = base.saturation,
                value = shadeValue,
            )
        }
    }

    private fun generateMonochromatic(base: HsvColor, count: Int): List<HsvColor> {
        val fullFive = listOf(
            HsvColor.of(
                hue = base.hue,
                saturation = (base.saturation * 0.32f).coerceIn(0.10f, 1f),
                value = (base.value + 0.22f).coerceIn(0.35f, 0.98f),
            ),
            HsvColor.of(
                hue = base.hue,
                saturation = (base.saturation * 0.64f).coerceIn(0.20f, 1f),
                value = (base.value * 0.88f).coerceIn(0.26f, 0.96f),
            ),
            base,
            HsvColor.of(
                hue = base.hue,
                saturation = (base.saturation * 0.92f).coerceIn(0.28f, 1f),
                value = (base.value * 0.62f).coerceIn(0.20f, 0.92f),
            ),
            HsvColor.of(
                hue = base.hue,
                saturation = (base.saturation * 1.00f).coerceIn(0.35f, 1f),
                value = (base.value * 0.36f).coerceIn(0.14f, 0.82f),
            ),
        )
        return when (clampColorCount(count)) {
            2 -> listOf(fullFive[2], fullFive[0])
            3 -> listOf(fullFive[0], fullFive[2], fullFive[4])
            4 -> listOf(fullFive[0], fullFive[2], fullFive[3], fullFive[4])
            else -> fullFive
        }
    }

    private fun specsFor(rule: ColorHarmonyRule, count: Int): List<SwatchHarmonySpec> {
        val safeCount = clampColorCount(count)
        return when (rule) {
            ColorHarmonyRule.ANALOGOUS -> analogousSpecs(safeCount)
            ColorHarmonyRule.COMPLEMENTARY -> complementarySpecs(safeCount)
            ColorHarmonyRule.SPLIT_COMPLEMENTARY -> splitComplementarySpecs(safeCount)
            ColorHarmonyRule.TRIAD -> triadSpecs(safeCount)
            ColorHarmonyRule.SQUARE -> squareSpecs(safeCount)
            ColorHarmonyRule.COMPOUND -> compoundSpecs(safeCount)
            ColorHarmonyRule.CUSTOM,
            ColorHarmonyRule.SHADES,
            ColorHarmonyRule.MONOCHROMATIC,
            -> analogousSpecs(safeCount)
        }
    }

    private fun analogousSpecs(count: Int): List<SwatchHarmonySpec> = when (count) {
        2 -> listOf(
            SwatchHarmonySpec(hueOffset = 0f, saturationScale = 1.00f, valueScale = 1.00f),
            SwatchHarmonySpec(hueOffset = 30f, saturationScale = 0.94f, valueScale = 0.96f),
        )
        3 -> listOf(
            SwatchHarmonySpec(hueOffset = -30f, saturationScale = 0.94f, valueScale = 0.96f),
            SwatchHarmonySpec(hueOffset = 0f, saturationScale = 1.00f, valueScale = 1.00f),
            SwatchHarmonySpec(hueOffset = 30f, saturationScale = 0.94f, valueScale = 0.96f),
        )
        4 -> listOf(
            SwatchHarmonySpec(hueOffset = -30f, saturationScale = 0.94f, valueScale = 0.95f),
            SwatchHarmonySpec(hueOffset = 0f, saturationScale = 1.00f, valueScale = 1.00f),
            SwatchHarmonySpec(hueOffset = 18f, saturationScale = 0.92f, valueScale = 0.98f),
            SwatchHarmonySpec(hueOffset = 36f, saturationScale = 0.96f, valueScale = 0.94f),
        )
        else -> ANALOGOUS_SPECS_5
    }

    private fun complementarySpecs(count: Int): List<SwatchHarmonySpec> = when (count) {
        2 -> listOf(
            SwatchHarmonySpec(hueOffset = 0f, saturationScale = 1.00f, valueScale = 1.00f),
            SwatchHarmonySpec(hueOffset = 180f, saturationScale = 1.00f, valueScale = 1.00f),
        )
        3 -> listOf(
            SwatchHarmonySpec(hueOffset = 0f, saturationScale = 0.56f, valueScale = 1.00f, valueOffset = 0.14f),
            SwatchHarmonySpec(hueOffset = 0f, saturationScale = 1.00f, valueScale = 1.00f),
            SwatchHarmonySpec(hueOffset = 180f, saturationScale = 1.00f, valueScale = 1.00f),
        )
        4 -> listOf(
            SwatchHarmonySpec(hueOffset = 0f, saturationScale = 0.62f, valueScale = 1.00f, valueOffset = 0.12f),
            SwatchHarmonySpec(hueOffset = 0f, saturationScale = 1.00f, valueScale = 1.00f),
            SwatchHarmonySpec(hueOffset = 180f, saturationScale = 0.78f, valueScale = 0.84f),
            SwatchHarmonySpec(hueOffset = 180f, saturationScale = 1.00f, valueScale = 1.00f),
        )
        else -> COMPLEMENTARY_SPECS_5
    }

    private fun splitComplementarySpecs(count: Int): List<SwatchHarmonySpec> = when (count) {
        2 -> listOf(
            SwatchHarmonySpec(hueOffset = 0f, saturationScale = 1.00f, valueScale = 1.00f),
            SwatchHarmonySpec(hueOffset = 150f, saturationScale = 1.00f, valueScale = 1.00f),
        )
        3 -> listOf(
            SwatchHarmonySpec(hueOffset = 150f, saturationScale = 1.00f, valueScale = 1.00f),
            SwatchHarmonySpec(hueOffset = 0f, saturationScale = 1.00f, valueScale = 1.00f),
            SwatchHarmonySpec(hueOffset = 210f, saturationScale = 1.00f, valueScale = 1.00f),
        )
        4 -> listOf(
            SwatchHarmonySpec(hueOffset = 150f, saturationScale = 1.00f, valueScale = 1.00f),
            SwatchHarmonySpec(hueOffset = 0f, saturationScale = 1.00f, valueScale = 1.00f),
            SwatchHarmonySpec(hueOffset = 210f, saturationScale = 1.00f, valueScale = 1.00f),
            SwatchHarmonySpec(hueOffset = 210f, saturationScale = 0.78f, valueScale = 0.84f),
        )
        else -> SPLIT_COMPLEMENTARY_SPECS_5
    }

    private fun triadSpecs(count: Int): List<SwatchHarmonySpec> = when (count) {
        2 -> listOf(
            SwatchHarmonySpec(hueOffset = 0f, saturationScale = 1.00f, valueScale = 1.00f),
            SwatchHarmonySpec(hueOffset = 120f, saturationScale = 1.00f, valueScale = 1.00f),
        )
        3 -> listOf(
            SwatchHarmonySpec(hueOffset = 120f, saturationScale = 1.00f, valueScale = 1.00f),
            SwatchHarmonySpec(hueOffset = 0f, saturationScale = 1.00f, valueScale = 1.00f),
            SwatchHarmonySpec(hueOffset = 240f, saturationScale = 1.00f, valueScale = 1.00f),
        )
        4 -> listOf(
            SwatchHarmonySpec(hueOffset = 120f, saturationScale = 1.00f, valueScale = 1.00f),
            SwatchHarmonySpec(hueOffset = 0f, saturationScale = 1.00f, valueScale = 1.00f),
            SwatchHarmonySpec(hueOffset = 240f, saturationScale = 1.00f, valueScale = 1.00f),
            SwatchHarmonySpec(hueOffset = 240f, saturationScale = 0.74f, valueScale = 0.82f),
        )
        else -> TRIAD_SPECS_5
    }

    private fun squareSpecs(count: Int): List<SwatchHarmonySpec> = when (count) {
        2 -> listOf(
            SwatchHarmonySpec(hueOffset = 0f, saturationScale = 1.00f, valueScale = 1.00f),
            SwatchHarmonySpec(hueOffset = 90f, saturationScale = 1.00f, valueScale = 1.00f),
        )
        3 -> listOf(
            SwatchHarmonySpec(hueOffset = 90f, saturationScale = 1.00f, valueScale = 1.00f),
            SwatchHarmonySpec(hueOffset = 0f, saturationScale = 1.00f, valueScale = 1.00f),
            SwatchHarmonySpec(hueOffset = 180f, saturationScale = 1.00f, valueScale = 1.00f),
        )
        4 -> listOf(
            SwatchHarmonySpec(hueOffset = 90f, saturationScale = 1.00f, valueScale = 1.00f),
            SwatchHarmonySpec(hueOffset = 0f, saturationScale = 1.00f, valueScale = 1.00f),
            SwatchHarmonySpec(hueOffset = 180f, saturationScale = 1.00f, valueScale = 1.00f),
            SwatchHarmonySpec(hueOffset = 270f, saturationScale = 1.00f, valueScale = 1.00f),
        )
        else -> SQUARE_SPECS_5
    }

    private fun compoundSpecs(count: Int): List<SwatchHarmonySpec> = when (count) {
        2 -> listOf(
            SwatchHarmonySpec(hueOffset = 0f, saturationScale = 1.00f, valueScale = 1.00f),
            SwatchHarmonySpec(hueOffset = 160f, saturationScale = 0.92f, valueScale = 0.94f),
        )
        3 -> listOf(
            SwatchHarmonySpec(hueOffset = 20f, saturationScale = 0.96f, valueScale = 0.96f),
            SwatchHarmonySpec(hueOffset = 0f, saturationScale = 1.00f, valueScale = 1.00f),
            SwatchHarmonySpec(hueOffset = 160f, saturationScale = 0.92f, valueScale = 0.92f),
        )
        4 -> listOf(
            SwatchHarmonySpec(hueOffset = 20f, saturationScale = 0.96f, valueScale = 0.96f),
            SwatchHarmonySpec(hueOffset = 0f, saturationScale = 1.00f, valueScale = 1.00f),
            SwatchHarmonySpec(hueOffset = 160f, saturationScale = 0.80f, valueScale = 0.94f),
            SwatchHarmonySpec(hueOffset = 160f, saturationScale = 1.00f, valueScale = 0.78f),
        )
        else -> COMPOUND_SPECS_5
    }

    private data class SwatchHarmonySpec(
        val hueOffset: Float,
        val saturationScale: Float = 1f,
        val valueScale: Float = 1f,
        val valueOffset: Float = 0f,
    )

    private val ANALOGOUS_SPECS_5 = listOf(
        SwatchHarmonySpec(hueOffset = -30f, saturationScale = 0.96f, valueScale = 0.94f),
        SwatchHarmonySpec(hueOffset = -15f, saturationScale = 0.92f, valueScale = 1.00f),
        SwatchHarmonySpec(hueOffset = 0f, saturationScale = 1.00f, valueScale = 1.00f),
        SwatchHarmonySpec(hueOffset = 15f, saturationScale = 0.92f, valueScale = 1.00f),
        SwatchHarmonySpec(hueOffset = 30f, saturationScale = 0.96f, valueScale = 0.94f),
    )

    private val COMPLEMENTARY_SPECS_5 = listOf(
        SwatchHarmonySpec(hueOffset = 0f, saturationScale = 0.86f, valueScale = 0.72f),
        SwatchHarmonySpec(hueOffset = 0f, saturationScale = 0.56f, valueScale = 1.00f, valueOffset = 0.14f),
        SwatchHarmonySpec(hueOffset = 0f, saturationScale = 1.00f, valueScale = 1.00f),
        SwatchHarmonySpec(hueOffset = 180f, saturationScale = 0.78f, valueScale = 0.82f),
        SwatchHarmonySpec(hueOffset = 180f, saturationScale = 1.00f, valueScale = 1.00f),
    )

    private val SPLIT_COMPLEMENTARY_SPECS_5 = listOf(
        SwatchHarmonySpec(hueOffset = 150f, saturationScale = 0.80f, valueScale = 0.84f),
        SwatchHarmonySpec(hueOffset = 150f, saturationScale = 1.00f, valueScale = 1.00f),
        SwatchHarmonySpec(hueOffset = 0f, saturationScale = 1.00f, valueScale = 1.00f),
        SwatchHarmonySpec(hueOffset = 210f, saturationScale = 1.00f, valueScale = 1.00f),
        SwatchHarmonySpec(hueOffset = 210f, saturationScale = 0.80f, valueScale = 0.84f),
    )

    private val TRIAD_SPECS_5 = listOf(
        SwatchHarmonySpec(hueOffset = 120f, saturationScale = 0.74f, valueScale = 0.84f),
        SwatchHarmonySpec(hueOffset = 120f, saturationScale = 1.00f, valueScale = 1.00f),
        SwatchHarmonySpec(hueOffset = 0f, saturationScale = 1.00f, valueScale = 1.00f),
        SwatchHarmonySpec(hueOffset = 240f, saturationScale = 1.00f, valueScale = 1.00f),
        SwatchHarmonySpec(hueOffset = 240f, saturationScale = 0.74f, valueScale = 0.80f),
    )

    private val SQUARE_SPECS_5 = listOf(
        SwatchHarmonySpec(hueOffset = 90f, saturationScale = 1.00f, valueScale = 1.00f),
        SwatchHarmonySpec(hueOffset = 180f, saturationScale = 1.00f, valueScale = 1.00f),
        SwatchHarmonySpec(hueOffset = 0f, saturationScale = 1.00f, valueScale = 1.00f),
        SwatchHarmonySpec(hueOffset = 270f, saturationScale = 1.00f, valueScale = 1.00f),
        SwatchHarmonySpec(hueOffset = 180f, saturationScale = 0.56f, valueScale = 0.82f),
    )

    private val COMPOUND_SPECS_5 = listOf(
        SwatchHarmonySpec(hueOffset = 20f, saturationScale = 0.84f, valueScale = 0.78f),
        SwatchHarmonySpec(hueOffset = 20f, saturationScale = 1.00f, valueScale = 1.00f),
        SwatchHarmonySpec(hueOffset = 0f, saturationScale = 1.00f, valueScale = 1.00f),
        SwatchHarmonySpec(hueOffset = 160f, saturationScale = 0.78f, valueScale = 0.92f),
        SwatchHarmonySpec(hueOffset = 160f, saturationScale = 1.00f, valueScale = 0.74f),
    )
}
