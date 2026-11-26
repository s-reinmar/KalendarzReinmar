package com.example.feature.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.model.ColorHarmonyRule
import com.example.domain.model.PaletteId
import com.example.domain.model.SavedColorPalette
import com.example.domain.model.ThemeSurfaceMode
import com.example.ui.theme.AppIcons
import com.example.ui.theme.KalendarzReinmarTheme

@Composable
fun ColorWheelSettingsCard(
    uiState: SettingsUiState,
    onToggleExpanded: () -> Unit,
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
    onRestoreSavedPalette: (SavedColorPalette) -> Unit,
    onDeleteSavedPalette: (PaletteId) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("color_wheel_settings_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.28f),
        ),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Collapsible header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleExpanded)
                    .padding(start = 16.dp, top = 14.dp, bottom = 14.dp, end = 6.dp)
                    .testTag("toggle_color_wheel_header"),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(40.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(AppIcons.palette),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = stringResource(R.string.settings_color_wheel_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(
                            R.string.settings_color_wheel_subtitle_full,
                            stringResource(uiState.activeRule.labelRes()),
                            formatHarmonyColorCount(uiState.colorCount),
                            stringResource(uiState.surfaceMode.labelRes()),
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (!uiState.isColorWheelExpanded) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 2.dp)
                                .height(10.dp)
                                .clip(RoundedCornerShape(6.dp)),
                        ) {
                            uiState.swatches.forEach { swatch ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(10.dp)
                                        .background(swatch.toComposeColor()),
                                )
                            }
                        }
                    }
                }

                IconButton(
                    onClick = onToggleExpanded,
                    modifier = Modifier.testTag("toggle_color_wheel_button"),
                ) {
                    Icon(
                        imageVector = if (uiState.isColorWheelExpanded) {
                            Icons.Filled.KeyboardArrowUp
                        } else {
                            Icons.Filled.KeyboardArrowDown
                        },
                        contentDescription = stringResource(
                            if (uiState.isColorWheelExpanded) {
                                R.string.settings_color_wheel_collapse_cd
                            } else {
                                R.string.settings_color_wheel_expand_cd
                            },
                        ),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            AnimatedVisibility(
                visible = uiState.isColorWheelExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.22f),
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.settings_apply_theme_title),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = stringResource(R.string.settings_apply_theme_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = uiState.applyToAppTheme,
                            onCheckedChange = onToggleApplyToAppTheme,
                            modifier = Modifier.testTag("apply_color_wheel_theme_switch"),
                        )
                    }

                    ThemeSurfaceModeSelector(
                        selectedMode = uiState.surfaceMode,
                        onSelectMode = onSelectSurfaceMode,
                    )

                    HarmonyColorCountSelector(
                        selectedCount = uiState.colorCount,
                        onSelectCount = onSelectColorCount,
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = stringResource(R.string.settings_harmony_section_label),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            TextButton(
                                onClick = onResetToDefaultPalette,
                                modifier = Modifier.testTag("reset_color_wheel_button"),
                            ) {
                                Text(
                                    text = stringResource(R.string.settings_reset_palette_action),
                                    style = MaterialTheme.typography.labelMedium,
                                )
                            }
                        }
                        HarmonyRuleSelectorRow(
                            activeRule = uiState.activeRule,
                            onSelectRule = onSelectHarmonyRule,
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        HsvColorWheelCanvas(
                            swatches = uiState.swatches,
                            selectedSwatchIndex = uiState.selectedSwatchIndex,
                            onSwatchDragged = onSwatchDragged,
                            onGestureFinished = onCommitContinuousAdjustment,
                        )
                        Text(
                            text = stringResource(R.string.settings_wheel_hint),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }

                    ColorSwatchStrip(
                        swatches = uiState.swatches,
                        selectedSwatchIndex = uiState.selectedSwatchIndex,
                        onSelectSwatch = onSelectSwatch,
                    )

                    SwatchValueSlider(
                        activeSwatch = uiState.activeSwatch,
                        selectedSwatchIndex = uiState.selectedSwatchIndex,
                        onValueChange = onActiveSwatchValueChange,
                        onValueChangeFinished = onCommitContinuousAdjustment,
                        baseSwatchIndex = uiState.baseSwatchIndex,
                    )

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.22f),
                    )

                    SavedPalettesSection(
                        savedPalettes = uiState.savedPalettes,
                        onOpenSaveDialog = onOpenSavePaletteDialog,
                        onRestorePalette = onRestoreSavedPalette,
                        onDeletePalette = onDeleteSavedPalette,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ColorWheelSettingsCardPreview() {
    KalendarzReinmarTheme {
        Surface {
            ColorWheelSettingsCard(
                uiState = SettingsUiState(),
                onToggleExpanded = {},
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
                onRestoreSavedPalette = {},
                onDeleteSavedPalette = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
