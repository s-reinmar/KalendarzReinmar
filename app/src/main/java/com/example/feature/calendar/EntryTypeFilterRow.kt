package com.example.feature.calendar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.model.CalendarEntryType

@Composable
internal fun EntryTypeFilterRow(
    activeTypeFilter: CalendarEntryType?,
    totalCount: Int,
    typeCounts: Map<CalendarEntryType, Int>,
    onSelectTypeFilter: (CalendarEntryType?) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .testTag("calendar_type_filter_row"),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 2.dp),
    ) {
        item(key = "filter_all") {
            val allLabel = if (totalCount > 0) {
                "${stringResource(R.string.entry_type_all)} ($totalCount)"
            } else {
                stringResource(R.string.entry_type_all)
            }
            FilterChip(
                selected = activeTypeFilter == null,
                onClick = { onSelectTypeFilter(null) },
                label = { Text(text = allLabel, style = MaterialTheme.typography.labelLarge) },
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("filter_chip_all"),
            )
        }

        items(
            items = CalendarEntryType.entries,
            key = { it.code },
        ) { entryType ->
            val palette = CalendarEntryVisuals.palette(entryType.defaultColorTag)
            val count = typeCounts[entryType] ?: 0
            val baseLabel = stringResource(CalendarEntryVisuals.labelRes(entryType))
            val label = if (count > 0) "$baseLabel ($count)" else baseLabel

            FilterChip(
                selected = activeTypeFilter == entryType,
                onClick = { onSelectTypeFilter(entryType) },
                leadingIcon = {
                    Icon(
                        painter = painterResource(CalendarEntryVisuals.iconRes(entryType)),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                },
                label = { Text(text = label, style = MaterialTheme.typography.labelLarge) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = palette.container,
                    selectedLabelColor = palette.onContainer,
                    selectedLeadingIconColor = palette.accent,
                ),
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("filter_chip_${entryType.code.lowercase()}"),
            )
        }
    }
}
