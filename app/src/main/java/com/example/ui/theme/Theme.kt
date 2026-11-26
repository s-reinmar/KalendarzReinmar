package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.domain.model.HsvColor
import com.example.domain.model.ThemeSurfaceMode
import com.example.domain.service.ColorHarmonyEngine

private val LightColorScheme = lightColorScheme(
    primary = PineGreen,
    onPrimary = Color.White,
    primaryContainer = PineGreenContainer,
    onPrimaryContainer = OnPineGreenContainer,
    secondary = Terracotta,
    onSecondary = Color.White,
    secondaryContainer = TerracottaContainer,
    onSecondaryContainer = OnTerracottaContainer,
    tertiary = PineGreenLight,
    onTertiary = Color.White,
    background = WarmPaper,
    onBackground = InkDark,
    surface = WarmPaper,
    onSurface = InkDark,
    surfaceVariant = WarmPaperSurface,
    onSurfaceVariant = InkMuted,
    surfaceContainerLow = WarmPaperSurface,
    surfaceContainer = WarmPaperSurface,
    surfaceContainerHigh = WarmPaperSurfaceHigh,
    outline = WarmOutline,
    outlineVariant = WarmPaperSurfaceHigh,
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkPinePrimary,
    onPrimary = DarkOnPinePrimary,
    primaryContainer = DarkPineContainer,
    onPrimaryContainer = DarkInk,
    secondary = DarkTerracotta,
    onSecondary = DarkOnTerracotta,
    secondaryContainer = DarkTerracottaContainer,
    onSecondaryContainer = DarkInk,
    background = DarkBackground,
    onBackground = DarkInk,
    surface = DarkBackground,
    onSurface = DarkInk,
    surfaceVariant = DarkSurface,
    onSurfaceVariant = DarkInkMuted,
    surfaceContainerLow = DarkSurface,
    surfaceContainer = DarkSurface,
    surfaceContainerHigh = DarkSurfaceHigh,
    outline = DarkOutline,
    outlineVariant = DarkSurfaceHigh,
)

@Composable
fun KalendarzReinmarTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    customPaletteSwatches: List<HsvColor>? = null,
    surfaceMode: ThemeSurfaceMode = ThemeSurfaceMode.COLORFUL,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        customPaletteSwatches != null &&
            ColorHarmonyEngine.isValidSwatchCount(customPaletteSwatches.size) -> {
            AppColorSchemeBuilder.build(
                swatches = customPaletteSwatches,
                surfaceMode = surfaceMode,
            )
        }
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content,
    )
}
