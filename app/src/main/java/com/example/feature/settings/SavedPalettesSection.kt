package com.example.feature.settings

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.model.ColorHarmonyRule
import com.example.domain.model.PaletteId
import com.example.domain.model.SavedColorPalette
import com.example.domain.service.ColorHarmonyEngine
import com.example.ui.theme.KalendarzReinmarTheme

@Composable
fun SavedPalettesSection(
    savedPalettes: List<SavedColorPalette>,
    onOpenSaveDialog: () -> Unit,
    onRestorePalette: (SavedColorPalette) -> Unit,
    onDeletePalette: (PaletteId) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.settings_saved_palettes_title, savedPalettes.size),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )

            FilledTonalButton(
                onClick = onOpenSaveDialog,
                modifier = Modifier.testTag("save_palette_button"),
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = stringResource(R.string.settings_save_palette_action),
                    modifier = Modifier.padding(start = 6.dp),
                )
            }
        }

        if (savedPalettes.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(R.string.settings_saved_palettes_empty),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(14.dp),
                )
            }
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("saved_palettes_list"),
            ) {
                savedPalettes.forEach { palette ->
                    SavedPaletteRowCard(
                        palette = palette,
                        onRestore = { onRestorePalette(palette) },
                        onDelete = { onDeletePalette(palette.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SavedPaletteRowCard(
    palette: SavedColorPalette,
    onRestore: () -> Unit,
    onDelete: () -> Unit,
) {
    val restoreDescription = stringResource(R.string.settings_palette_restore_cd, palette.name)
    val deleteDescription = stringResource(R.string.settings_palette_delete_cd, palette.name)

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.30f),
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .semantics { contentDescription = restoreDescription }
            .clickable(onClick = onRestore)
            .testTag("saved_palette_item_${palette.id.value}"),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, top = 10.dp, bottom = 10.dp, end = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = palette.name,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Text(
                        text = stringResource(palette.rule.labelRes()),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(18.dp)
                        .clip(RoundedCornerShape(8.dp)),
                ) {
                    palette.swatches.forEach { swatch ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(18.dp)
                                .background(swatch.toComposeColor()),
                        )
                    }
                }
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.testTag("delete_saved_palette_${palette.id.value}"),
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = deleteDescription,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SavedPalettesSectionPreview() {
    KalendarzReinmarTheme {
        Surface {
            SavedPalettesSection(
                savedPalettes = listOf(
                    SavedColorPalette(
                        id = PaletteId("preview_1"),
                        name = "Leśny poranek",
                        rule = ColorHarmonyRule.ANALOGOUS,
                        swatches = ColorHarmonyEngine.defaultSnapshot().swatches,
                        createdAtMillis = 1_700_000_000_000L,
                    ),
                    SavedColorPalette(
                        id = PaletteId("preview_2"),
                        name = "Terakota i błękit",
                        rule = ColorHarmonyRule.COMPLEMENTARY,
                        swatches = ColorHarmonyEngine.generatePalette(
                            rule = ColorHarmonyRule.COMPLEMENTARY,
                            baseColor = ColorHarmonyEngine.defaultBaseColor,
                        ),
                        createdAtMillis = 1_700_000_100_000L,
                    ),
                ),
                onOpenSaveDialog = {},
                onRestorePalette = {},
                onDeletePalette = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
