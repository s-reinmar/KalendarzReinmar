package com.example.domain

import com.example.domain.model.ColorHarmonyRule
import com.example.domain.model.HsvColor
import com.example.domain.service.ColorHarmonyEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ColorHarmonyEngineTest {

    @Test
    fun hsvToHexConvertsPrimaryColorsAccurately() {
        assertEquals("#FF0000", HsvColor.of(0f, 1f, 1f).toHex())
        assertEquals("#00FF00", HsvColor.of(120f, 1f, 1f).toHex())
        assertEquals("#0000FF", HsvColor.of(240f, 1f, 1f).toHex())
        assertEquals("#FFFFFF", HsvColor.of(0f, 0f, 1f).toHex())
        assertEquals("#000000", HsvColor.of(180f, 1f, 0f).toHex())
    }

    @Test
    fun allNineHarmonyRulesProduceFiveNormalizedSwatches() {
        val base = HsvColor.of(200f, 0.75f, 0.80f)
        ColorHarmonyRule.entries.forEach { rule ->
            val palette = ColorHarmonyEngine.generatePalette(rule, base)
            assertEquals("Rule $rule must generate 5 swatches", 5, palette.size)
            palette.forEach { swatch ->
                assertTrue(swatch.hue in 0f..<360f)
                assertTrue(swatch.saturation in 0f..1f)
                assertTrue(swatch.value in 0f..1f)
            }
        }
    }

    @Test
    fun analogousUsesExpectedHueOffsetsAroundBase() {
        val base = HsvColor.of(100f, 0.8f, 0.8f)
        val palette = ColorHarmonyEngine.generatePalette(ColorHarmonyRule.ANALOGOUS, base)
        assertEquals(70f, palette[0].hue, 0.01f)
        assertEquals(85f, palette[1].hue, 0.01f)
        assertEquals(100f, palette[2].hue, 0.01f)
        assertEquals(115f, palette[3].hue, 0.01f)
        assertEquals(130f, palette[4].hue, 0.01f)
    }

    @Test
    fun complementaryPlacesOppositeHueAt180Degrees() {
        val base = HsvColor.of(30f, 0.8f, 0.8f)
        val palette = ColorHarmonyEngine.generatePalette(ColorHarmonyRule.COMPLEMENTARY, base)
        assertEquals(30f, palette[2].hue, 0.01f)
        assertEquals(210f, palette[3].hue, 0.01f)
        assertEquals(210f, palette[4].hue, 0.01f)
    }

    @Test
    fun splitComplementaryPlacesSatellitesAt150And210Degrees() {
        val base = HsvColor.of(60f, 0.8f, 0.8f)
        val palette = ColorHarmonyEngine.generatePalette(ColorHarmonyRule.SPLIT_COMPLEMENTARY, base)
        assertEquals(210f, palette[1].hue, 0.01f)
        assertEquals(60f, palette[2].hue, 0.01f)
        assertEquals(270f, palette[3].hue, 0.01f)
    }

    @Test
    fun triadAndSquareUse120And90DegreeSteps() {
        val base = HsvColor.of(10f, 0.8f, 0.8f)
        val triad = ColorHarmonyEngine.generatePalette(ColorHarmonyRule.TRIAD, base)
        assertEquals(130f, triad[1].hue, 0.01f)
        assertEquals(10f, triad[2].hue, 0.01f)
        assertEquals(250f, triad[3].hue, 0.01f)

        val square = ColorHarmonyEngine.generatePalette(ColorHarmonyRule.SQUARE, base)
        assertEquals(100f, square[0].hue, 0.01f)
        assertEquals(190f, square[1].hue, 0.01f)
        assertEquals(10f, square[2].hue, 0.01f)
        assertEquals(280f, square[3].hue, 0.01f)
    }

    @Test
    fun shadesSharesHueAndSaturationWithDistinctValues() {
        val base = HsvColor.of(162f, 0.65f, 0.70f)
        val shades = ColorHarmonyEngine.generatePalette(ColorHarmonyRule.SHADES, base)
        shades.forEach { swatch ->
            assertEquals(162f, swatch.hue, 0.01f)
            assertEquals(0.65f, swatch.saturation, 0.01f)
        }
        assertEquals(5, shades.map { it.value }.distinct().size)
    }

    @Test
    fun monochromaticSharesHueWithVariedSaturationAndValue() {
        val base = HsvColor.of(220f, 0.80f, 0.75f)
        val mono = ColorHarmonyEngine.generatePalette(ColorHarmonyRule.MONOCHROMATIC, base)
        mono.forEach { swatch ->
            assertEquals(220f, swatch.hue, 0.01f)
        }
        assertTrue(mono.map { it.saturation }.distinct().size >= 4)
        assertTrue(mono.map { it.value }.distinct().size >= 4)
    }

    @Test
    fun customAllowsIndependentHandleMovement() {
        val initial = ColorHarmonyEngine.defaultSnapshot().swatches
        val updated = ColorHarmonyEngine.updateSwatchOnWheel(
            rule = ColorHarmonyRule.CUSTOM,
            currentSwatches = initial,
            swatchIndex = 0,
            newHue = 310f,
            newSaturation = 0.42f,
        )
        assertEquals(310f, updated[0].hue, 0.01f)
        assertEquals(0.42f, updated[0].saturation, 0.01f)
        assertEquals(initial[2], updated[2])
        assertEquals(initial[4], updated[4])
    }

    @Test
    fun allNineHarmonyRulesSupportTwoToFiveColors() {
        val base = HsvColor.of(210f, 0.80f, 0.78f)
        for (count in 2..5) {
            ColorHarmonyRule.entries.forEach { rule ->
                val palette = ColorHarmonyEngine.generatePalette(
                    rule = rule,
                    baseColor = base,
                    colorCount = count,
                )
                assertEquals("Rule $rule with count=$count", count, palette.size)
                val baseIdx = ColorHarmonyEngine.baseSwatchIndex(count)
                assertEquals(base.hue, palette[baseIdx].hue, 0.01f)
            }
        }
    }
}
