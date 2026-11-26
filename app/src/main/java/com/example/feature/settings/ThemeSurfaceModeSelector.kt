package com.example.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.model.ThemeSurfaceMode
import com.example.ui.theme.KalendarzReinmarTheme

@Composable
fun ThemeSurfaceModeSelector(
    selectedMode: ThemeSurfaceMode,
    onSelectMode: (ThemeSurfaceMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_surface_mode_label),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("surface_mode_selector_row"),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 2.dp),
        ) {
            items(
                items = ThemeSurfaceMode.entries,
                key = { it.name },
            ) { mode ->
                val isSelected = mode == selectedMode
                FilterChip(
                    selected = isSelected,
                    onClick = { onSelectMode(mode) },
                    label = {
                        Text(
                            text = stringResource(mode.labelRes()),
                            style = MaterialTheme.typography.labelMedium,
                        )
                    },
                    leadingIcon = if (isSelected) {
                        {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    } else {
                        null
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("surface_mode_chip_${mode.name.lowercase()}"),
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ThemeSurfaceModeSelectorPreview() {
    KalendarzReinmarTheme {
        Surface {
            ThemeSurfaceModeSelector(
                selectedMode = ThemeSurfaceMode.COLORFUL,
                onSelectMode = {},
            )
        }
    }
}
