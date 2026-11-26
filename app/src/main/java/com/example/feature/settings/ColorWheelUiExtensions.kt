package com.example.feature.settings

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.example.R
import com.example.domain.model.ColorHarmonyRule
import com.example.domain.model.HsvColor
import com.example.domain.model.ThemeSurfaceMode

fun HsvColor.toComposeColor(): Color {
    val rgb = toRgb()
    return Color(red = rgb.red, green = rgb.green, blue = rgb.blue)
}

fun HsvColor.contrastingContentColor(): Color =
    if (isLightColor) Color(0xFF141816) else Color(0xFFFAF9F6)

@StringRes
fun ColorHarmonyRule.labelRes(): Int = when (this) {
    ColorHarmonyRule.CUSTOM -> R.string.settings_harmony_custom
    ColorHarmonyRule.ANALOGOUS -> R.string.settings_harmony_analogous
    ColorHarmonyRule.COMPLEMENTARY -> R.string.settings_harmony_complementary
    ColorHarmonyRule.SPLIT_COMPLEMENTARY -> R.string.settings_harmony_split_complementary
    ColorHarmonyRule.TRIAD -> R.string.settings_harmony_triad
    ColorHarmonyRule.SQUARE -> R.string.settings_harmony_square
    ColorHarmonyRule.COMPOUND -> R.string.settings_harmony_compound
    ColorHarmonyRule.SHADES -> R.string.settings_harmony_shades
    ColorHarmonyRule.MONOCHROMATIC -> R.string.settings_harmony_monochromatic
}

@StringRes
fun ThemeSurfaceMode.labelRes(): Int = when (this) {
    ThemeSurfaceMode.COLORFUL -> R.string.settings_surface_mode_colorful
    ThemeSurfaceMode.LIGHT -> R.string.settings_surface_mode_light
    ThemeSurfaceMode.DARK -> R.string.settings_surface_mode_dark
}

@Composable
fun formatHarmonyColorCount(count: Int): String = if (count in 2..4) {
    stringResource(R.string.settings_color_count_few, count)
} else {
    stringResource(R.string.settings_color_count_many, count)
}
