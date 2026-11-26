package com.example.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import com.example.domain.model.HsvColor
import com.example.domain.model.ThemeSurfaceMode
import com.example.domain.service.ColorHarmonyEngine

object AppColorSchemeBuilder {

    fun build(
        swatches: List<HsvColor>,
        surfaceMode: ThemeSurfaceMode = ThemeSurfaceMode.COLORFUL,
    ): ColorScheme {
        val roles = resolveHarmonyRoles(swatches)
        return when (surfaceMode) {
            ThemeSurfaceMode.COLORFUL -> {
                if (roles.base.value < CHROMATIC_DARK_VALUE_THRESHOLD) {
                    buildChromaticDarkScheme(roles)
                } else {
                    buildColorfulScheme(roles)
                }
            }
            ThemeSurfaceMode.LIGHT -> buildTintedLightScheme(roles)
            ThemeSurfaceMode.DARK -> buildChromaticDarkScheme(roles)
        }
    }

    fun build(
        swatches: List<HsvColor>,
        darkTheme: Boolean,
    ): ColorScheme = build(
        swatches = swatches,
        surfaceMode = if (darkTheme) ThemeSurfaceMode.DARK else ThemeSurfaceMode.COLORFUL,
    )

    private fun resolveHarmonyRoles(swatches: List<HsvColor>): HarmonyColorRoles {
        val safe = ColorHarmonyEngine.normalizeSwatches(swatches)
        return when (safe.size) {
            2 -> HarmonyColorRoles(
                base = safe[0],
                secondary = safe[1],
                tertiary = blendHsv(safe[0], safe[1], 0.5f),
                softSupport = safe[1],
                deepSupport = safe[0],
            )
            3 -> HarmonyColorRoles(
                base = safe[1],
                secondary = safe[2],
                tertiary = safe[0],
                softSupport = safe[0],
                deepSupport = safe[2],
            )
            4 -> HarmonyColorRoles(
                base = safe[1],
                secondary = safe[3],
                tertiary = safe[0],
                softSupport = safe[2],
                deepSupport = safe[3],
            )
            else -> HarmonyColorRoles(
                base = safe[ColorHarmonyEngine.BASE_SWATCH_INDEX],
                secondary = safe[4],
                tertiary = safe[0],
                softSupport = safe[1],
                deepSupport = safe[3],
            )
        }
    }

    private fun buildColorfulScheme(roles: HarmonyColorRoles): ColorScheme {
        val primary = ensureLightModeAccentReadability(roles.base)
        val secondary = ensureLightModeAccentReadability(roles.secondary)
        val tertiary = ensureLightModeAccentReadability(roles.tertiary)

        val background = chromaticSurface(
            source = roles.base,
            satScale = 0.34f,
            minSat = 0.14f,
            maxSat = 0.36f,
            value = (0.86f + roles.base.value * 0.11f).coerceIn(0.87f, 0.97f),
        )
        val surfaceContainerLow = chromaticSurface(
            source = roles.softSupport,
            satScale = 0.22f,
            minSat = 0.08f,
            maxSat = 0.24f,
            value = (0.93f + roles.softSupport.value * 0.06f).coerceIn(0.94f, 0.99f),
        )
        val surfaceContainer = chromaticSurface(
            source = roles.softSupport,
            satScale = 0.32f,
            minSat = 0.12f,
            maxSat = 0.32f,
            value = (0.88f + roles.softSupport.value * 0.08f).coerceIn(0.89f, 0.96f),
        )
        val surfaceContainerHigh = chromaticSurface(
            source = roles.deepSupport,
            satScale = 0.40f,
            minSat = 0.16f,
            maxSat = 0.42f,
            value = (0.83f + roles.deepSupport.value * 0.09f).coerceIn(0.84f, 0.92f),
        )
        val surfaceVariant = lerp(surfaceContainer, roles.tertiary.toThemeColor(), 0.22f)

        val primaryContainer = chromaticSurface(
            source = roles.base,
            satScale = 0.56f,
            minSat = 0.22f,
            maxSat = 0.58f,
            value = (0.76f + roles.base.value * 0.18f).coerceIn(0.78f, 0.94f),
        )
        val secondaryContainer = chromaticSurface(
            source = roles.secondary,
            satScale = 0.54f,
            minSat = 0.22f,
            maxSat = 0.56f,
            value = (0.78f + roles.secondary.value * 0.16f).coerceIn(0.80f, 0.94f),
        )
        val tertiaryContainer = chromaticSurface(
            source = roles.tertiary,
            satScale = 0.52f,
            minSat = 0.20f,
            maxSat = 0.54f,
            value = (0.78f + roles.tertiary.value * 0.16f).coerceIn(0.80f, 0.94f),
        )

        val outline = chromaticSurface(
            source = roles.deepSupport,
            satScale = 0.45f,
            minSat = 0.16f,
            maxSat = 0.48f,
            value = 0.52f,
        )
        val outlineVariant = chromaticSurface(
            source = roles.softSupport,
            satScale = 0.36f,
            minSat = 0.14f,
            maxSat = 0.38f,
            value = 0.72f,
        )

        val onBackground = deepChromaticInk(roles.base, LIGHT_INK_PRIMARY)
        val onSurfaceVariant = lerp(deepChromaticInk(roles.base, LIGHT_INK_MUTED), primary, 0.22f)

        return lightColorScheme(
            primary = primary,
            onPrimary = contrastingOnColor(primary, roles.base),
            primaryContainer = primaryContainer,
            onPrimaryContainer = contrastingOnColor(primaryContainer, roles.base),
            secondary = secondary,
            onSecondary = contrastingOnColor(secondary, roles.secondary),
            secondaryContainer = secondaryContainer,
            onSecondaryContainer = contrastingOnColor(secondaryContainer, roles.secondary),
            tertiary = tertiary,
            onTertiary = contrastingOnColor(tertiary, roles.tertiary),
            tertiaryContainer = tertiaryContainer,
            onTertiaryContainer = contrastingOnColor(tertiaryContainer, roles.tertiary),
            background = background,
            onBackground = onBackground,
            surface = background,
            onSurface = onBackground,
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = onSurfaceVariant,
            surfaceContainerLow = surfaceContainerLow,
            surfaceContainer = surfaceContainer,
            surfaceContainerHigh = surfaceContainerHigh,
            outline = outline,
            outlineVariant = outlineVariant,
        )
    }

    private fun buildTintedLightScheme(roles: HarmonyColorRoles): ColorScheme {
        val primary = ensureLightModeAccentReadability(roles.base)
        val secondary = ensureLightModeAccentReadability(roles.secondary)
        val tertiary = ensureLightModeAccentReadability(roles.tertiary)

        val background = chromaticSurface(
            source = roles.base,
            satScale = 0.16f,
            minSat = 0.06f,
            maxSat = 0.18f,
            value = 0.97f,
        )
        val surfaceContainerLow = chromaticSurface(
            source = roles.softSupport,
            satScale = 0.12f,
            minSat = 0.05f,
            maxSat = 0.14f,
            value = 0.99f,
        )
        val surfaceContainer = chromaticSurface(
            source = roles.softSupport,
            satScale = 0.20f,
            minSat = 0.08f,
            maxSat = 0.22f,
            value = 0.95f,
        )
        val surfaceContainerHigh = chromaticSurface(
            source = roles.deepSupport,
            satScale = 0.26f,
            minSat = 0.10f,
            maxSat = 0.28f,
            value = 0.91f,
        )
        val surfaceVariant = lerp(surfaceContainer, roles.tertiary.toThemeColor(), 0.16f)

        val primaryContainer = chromaticSurface(
            source = roles.base,
            satScale = 0.36f,
            minSat = 0.14f,
            maxSat = 0.40f,
            value = 0.92f,
        )
        val secondaryContainer = chromaticSurface(
            source = roles.secondary,
            satScale = 0.36f,
            minSat = 0.14f,
            maxSat = 0.40f,
            value = 0.92f,
        )
        val tertiaryContainer = chromaticSurface(
            source = roles.tertiary,
            satScale = 0.34f,
            minSat = 0.12f,
            maxSat = 0.38f,
            value = 0.92f,
        )

        val outline = chromaticSurface(
            source = roles.deepSupport,
            satScale = 0.34f,
            minSat = 0.12f,
            maxSat = 0.36f,
            value = 0.58f,
        )
        val outlineVariant = chromaticSurface(
            source = roles.softSupport,
            satScale = 0.24f,
            minSat = 0.08f,
            maxSat = 0.28f,
            value = 0.80f,
        )

        val onBackground = deepChromaticInk(roles.base, LIGHT_INK_PRIMARY)
        val onSurfaceVariant = lerp(deepChromaticInk(roles.base, LIGHT_INK_MUTED), primary, 0.20f)

        return lightColorScheme(
            primary = primary,
            onPrimary = contrastingOnColor(primary, roles.base),
            primaryContainer = primaryContainer,
            onPrimaryContainer = contrastingOnColor(primaryContainer, roles.base),
            secondary = secondary,
            onSecondary = contrastingOnColor(secondary, roles.secondary),
            secondaryContainer = secondaryContainer,
            onSecondaryContainer = contrastingOnColor(secondaryContainer, roles.secondary),
            tertiary = tertiary,
            onTertiary = contrastingOnColor(tertiary, roles.tertiary),
            tertiaryContainer = tertiaryContainer,
            onTertiaryContainer = contrastingOnColor(tertiaryContainer, roles.tertiary),
            background = background,
            onBackground = onBackground,
            surface = background,
            onSurface = onBackground,
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = onSurfaceVariant,
            surfaceContainerLow = surfaceContainerLow,
            surfaceContainer = surfaceContainer,
            surfaceContainerHigh = surfaceContainerHigh,
            outline = outline,
            outlineVariant = outlineVariant,
        )
    }

    private fun buildChromaticDarkScheme(roles: HarmonyColorRoles): ColorScheme {
        val primary = ensureDarkModeAccentReadability(roles.base)
        val secondary = ensureDarkModeAccentReadability(roles.secondary)
        val tertiary = ensureDarkModeAccentReadability(roles.tertiary)

        // Rich jewel-toned dark surfaces derived directly from the harmony swatches (never flat neutral black)
        val background = chromaticSurface(
            source = roles.base,
            satScale = 0.52f,
            minSat = 0.28f,
            maxSat = 0.56f,
            value = (0.16f + roles.base.value * 0.06f).coerceIn(0.16f, 0.22f),
        )
        val surfaceContainerLow = chromaticSurface(
            source = roles.softSupport,
            satScale = 0.48f,
            minSat = 0.24f,
            maxSat = 0.50f,
            value = (0.21f + roles.softSupport.value * 0.06f).coerceIn(0.21f, 0.27f),
        )
        val surfaceContainer = chromaticSurface(
            source = roles.base,
            satScale = 0.50f,
            minSat = 0.26f,
            maxSat = 0.52f,
            value = (0.25f + roles.base.value * 0.06f).coerceIn(0.25f, 0.31f),
        )
        val surfaceContainerHigh = chromaticSurface(
            source = roles.deepSupport,
            satScale = 0.54f,
            minSat = 0.28f,
            maxSat = 0.56f,
            value = (0.30f + roles.deepSupport.value * 0.06f).coerceIn(0.30f, 0.36f),
        )
        val surfaceVariant = lerp(surfaceContainer, roles.tertiary.toThemeColor(), 0.24f)

        val primaryContainer = chromaticSurface(
            source = roles.base,
            satScale = 0.68f,
            minSat = 0.36f,
            maxSat = 0.72f,
            value = (0.34f + roles.base.value * 0.12f).coerceIn(0.34f, 0.46f),
        )
        val secondaryContainer = chromaticSurface(
            source = roles.secondary,
            satScale = 0.66f,
            minSat = 0.34f,
            maxSat = 0.70f,
            value = (0.34f + roles.secondary.value * 0.12f).coerceIn(0.34f, 0.46f),
        )
        val tertiaryContainer = chromaticSurface(
            source = roles.tertiary,
            satScale = 0.64f,
            minSat = 0.32f,
            maxSat = 0.68f,
            value = (0.34f + roles.tertiary.value * 0.12f).coerceIn(0.34f, 0.46f),
        )

        val outline = chromaticSurface(
            source = roles.deepSupport,
            satScale = 0.46f,
            minSat = 0.22f,
            maxSat = 0.50f,
            value = 0.62f,
        )
        val outlineVariant = chromaticSurface(
            source = roles.softSupport,
            satScale = 0.44f,
            minSat = 0.20f,
            maxSat = 0.48f,
            value = 0.42f,
        )

        val onBackground = lightChromaticInk(roles.base, DARK_INK_PRIMARY)
        val onSurfaceVariant = lerp(lightChromaticInk(roles.base, DARK_INK_MUTED), primary, 0.24f)

        return darkColorScheme(
            primary = primary,
            onPrimary = contrastingOnColor(primary, roles.base),
            primaryContainer = primaryContainer,
            onPrimaryContainer = contrastingOnColor(primaryContainer, roles.base),
            secondary = secondary,
            onSecondary = contrastingOnColor(secondary, roles.secondary),
            secondaryContainer = secondaryContainer,
            onSecondaryContainer = contrastingOnColor(secondaryContainer, roles.secondary),
            tertiary = tertiary,
            onTertiary = contrastingOnColor(tertiary, roles.tertiary),
            tertiaryContainer = tertiaryContainer,
            onTertiaryContainer = contrastingOnColor(tertiaryContainer, roles.tertiary),
            background = background,
            onBackground = onBackground,
            surface = background,
            onSurface = onBackground,
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = onSurfaceVariant,
            surfaceContainerLow = surfaceContainerLow,
            surfaceContainer = surfaceContainer,
            surfaceContainerHigh = surfaceContainerHigh,
            outline = outline,
            outlineVariant = outlineVariant,
        )
    }

    private fun chromaticSurface(
        source: HsvColor,
        satScale: Float,
        minSat: Float,
        maxSat: Float,
        value: Float,
    ): Color {
        val chromaPresence = (source.saturation / 0.12f).coerceIn(0f, 1f)
        val targetSat = if (chromaPresence <= 0f) {
            0f
        } else {
            (source.saturation * satScale).coerceIn(minSat * chromaPresence, maxSat)
        }
        return HsvColor.of(
            hue = source.hue,
            saturation = targetSat,
            value = value.coerceIn(0.12f, 0.99f),
        ).toThemeColor()
    }

    private fun blendHsv(first: HsvColor, second: HsvColor, fraction: Float): HsvColor {
        val f = fraction.coerceIn(0f, 1f)
        val delta = ((second.hue - first.hue + 540f) % 360f) - 180f
        val blendedHue = HsvColor.normalizeHue(first.hue + delta * f)
        return HsvColor.of(
            hue = blendedHue,
            saturation = first.saturation + (second.saturation - first.saturation) * f,
            value = first.value + (second.value - first.value) * f,
        )
    }

    private fun ensureLightModeAccentReadability(hsv: HsvColor): Color {
        val raw = hsv.toThemeColor()
        if (raw.luminance() <= 0.78f) return raw
        return hsv.withValue(hsv.value.coerceAtMost(0.70f)).toThemeColor()
    }

    private fun ensureDarkModeAccentReadability(hsv: HsvColor): Color {
        val raw = hsv.toThemeColor()
        if (raw.luminance() >= 0.24f) return raw
        return hsv.copy(
            saturation = hsv.saturation.coerceAtMost(0.76f),
            value = hsv.value.coerceAtLeast(0.78f),
        ).toThemeColor()
    }

    private fun deepChromaticInk(source: HsvColor, baseInk: Color): Color {
        val tint = HsvColor.of(source.hue, (source.saturation * 0.45f).coerceIn(0f, 0.45f), 0.18f).toThemeColor()
        return lerp(baseInk, tint, 0.35f)
    }

    private fun lightChromaticInk(source: HsvColor, baseInk: Color): Color {
        val tint = HsvColor.of(source.hue, (source.saturation * 0.20f).coerceIn(0f, 0.20f), 0.96f).toThemeColor()
        return lerp(baseInk, tint, 0.35f)
    }

    private fun contrastingOnColor(background: Color, hint: HsvColor): Color =
        if (background.luminance() >= 0.40f) {
            deepChromaticInk(hint, Color(0xFF101412))
        } else {
            lightChromaticInk(hint, Color.White)
        }

    private fun HsvColor.toThemeColor(): Color {
        val rgb = toRgb()
        return Color(red = rgb.red, green = rgb.green, blue = rgb.blue)
    }

    private data class HarmonyColorRoles(
        val base: HsvColor,
        val secondary: HsvColor,
        val tertiary: HsvColor,
        val softSupport: HsvColor,
        val deepSupport: HsvColor,
    )

    private const val CHROMATIC_DARK_VALUE_THRESHOLD = 0.24f

    private val LIGHT_INK_PRIMARY = Color(0xFF141816)
    private val LIGHT_INK_MUTED = Color(0xFF3E4742)
    private val DARK_INK_PRIMARY = Color(0xFFF3F5F4)
    private val DARK_INK_MUTED = Color(0xFFC4CEC9)
}
