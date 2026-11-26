package com.example.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.service.ColorHarmonyEngine
import com.example.ui.theme.KalendarzReinmarTheme

@Composable
fun HarmonyColorCountSelector(
    selectedCount: Int,
    onSelectCount: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_color_count_label),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("harmony_color_count_row"),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ColorHarmonyEngine.supportedColorCounts.forEach { count ->
                val isSelected = count == selectedCount
                FilterChip(
                    selected = isSelected,
                    onClick = { onSelectCount(count) },
                    label = {
                        Text(
                            text = formatHarmonyColorCount(count),
                            style = MaterialTheme.typography.labelMedium,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("harmony_count_chip_$count"),
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HarmonyColorCountSelectorPreview() {
    KalendarzReinmarTheme {
        Surface {
            HarmonyColorCountSelector(
                selectedCount = 3,
                onSelectCount = {},
            )
        }
    }
}
