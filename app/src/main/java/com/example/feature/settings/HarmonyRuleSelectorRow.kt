package com.example.feature.settings

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.domain.model.ColorHarmonyRule
import com.example.ui.theme.KalendarzReinmarTheme

@Composable
fun HarmonyRuleSelectorRow(
    activeRule: ColorHarmonyRule,
    onSelectRule: (ColorHarmonyRule) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .testTag("harmony_rule_selector_row"),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 2.dp),
    ) {
        items(
            items = ColorHarmonyRule.entries,
            key = { it.name },
        ) { rule ->
            val selected = rule == activeRule
            FilterChip(
                selected = selected,
                onClick = { onSelectRule(rule) },
                label = {
                    Text(
                        text = stringResource(rule.labelRes()),
                        style = MaterialTheme.typography.labelMedium,
                    )
                },
                leadingIcon = if (selected) {
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
                modifier = Modifier.testTag("harmony_rule_chip_${rule.name.lowercase()}"),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HarmonyRuleSelectorRowPreview() {
    KalendarzReinmarTheme {
        Surface {
            HarmonyRuleSelectorRow(
                activeRule = ColorHarmonyRule.TRIAD,
                onSelectRule = {},
            )
        }
    }
}
