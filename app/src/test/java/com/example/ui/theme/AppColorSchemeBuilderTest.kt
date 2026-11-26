package com.example.ui.theme

import androidx.compose.ui.graphics.luminance
import com.example.domain.model.ColorHarmonyRule
import com.example.domain.model.HsvColor
import com.example.domain.model.ThemeSurfaceMode
import com.example.domain.service.ColorHarmonyEngine
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppColorSchemeBuilderTest {

    @Test
    fun colorfulModeProducesLuminousNonBlackChromaticSurfacesForTwoToFiveSwatches() {
        val blueBase = HsvColor.of(hue = 215f, saturation = 0.80f, value = 0.80f)
        val warmBase = HsvColor.of(hue = 24f, saturation = 0.82f, value = 0.82f)

        for (count in 2..5) {
            val bluePalette = ColorHarmonyEngine.generatePalette(
                rule = ColorHarmonyRule.COMPLEMENTARY,
                baseColor = blueBase,
                colorCount = count,
            )
            val warmPalette = ColorHarmonyEngine.generatePalette(
                rule = ColorHarmonyRule.COMPLEMENTARY,
                baseColor = warmBase,
                colorCount = count,
            )

            val blueScheme = AppColorSchemeBuilder.build(bluePalette, ThemeSurfaceMode.COLORFUL)
            val warmScheme = AppColorSchemeBuilder.build(warmPalette, ThemeSurfaceMode.COLORFUL)

            // Must NOT be dominated by black!
            assertTrue(
                "Background luminance must be bright in COLORFUL mode for count=$count",
                blueScheme.background.luminance() > 0.50f,
            )
            assertTrue(
                "Top bar primaryContainer must carry strong blue channel for blue base",
                blueScheme.primaryContainer.blue > blueScheme.primaryContainer.red + 0.12f,
            )
            assertTrue(
                "Background must visibly shift when hue changes from blue to warm orange",
                blueScheme.background.blue > blueScheme.background.red + 0.08f &&
                    warmScheme.background.red > warmScheme.background.blue + 0.08f,
            )
            assertNotEquals(blueScheme.primary, blueScheme.secondary)
        }
    }

    @Test
    fun darkChromaticModeUsesColoredDeepTonesInsteadOfNeutralBlack() {
        val indigoBase = HsvColor.of(hue = 235f, saturation = 0.85f, value = 0.80f)
        val palette = ColorHarmonyEngine.generatePalette(
            rule = ColorHarmonyRule.TRIAD,
            baseColor = indigoBase,
            colorCount = 3,
        )

        val darkScheme = AppColorSchemeBuilder.build(palette, ThemeSurfaceMode.DARK)

        assertTrue(
            "Dark chromatic background should visibly favor blue over red",
            darkScheme.background.blue > darkScheme.background.red + 0.04f,
        )
        assertTrue(
            "Dark chromatic primaryContainer should visibly favor blue over red",
            darkScheme.primaryContainer.blue > darkScheme.primaryContainer.red + 0.10f,
        )
    }
}
