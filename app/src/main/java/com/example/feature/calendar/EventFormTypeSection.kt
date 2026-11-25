package com.example.feature.calendar

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.model.CalendarEntryType
import com.example.domain.model.EventColorTag

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun EventFormTypeSection(
    selectedType: CalendarEntryType,
    selectedColorTag: EventColorTag,
    onSelectType: (CalendarEntryType) -> Unit,
    onSelectColorTag: (EventColorTag) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.entry_form_type_section),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            CalendarEntryType.entries.forEach { entryType ->
                val palette = CalendarEntryVisuals.palette(entryType.defaultColorTag)
                FilterChip(
                    selected = selectedType == entryType,
                    onClick = { onSelectType(entryType) },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(CalendarEntryVisuals.iconRes(entryType)),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                    },
                    label = {
                        Text(
                            text = stringResource(CalendarEntryVisuals.labelRes(entryType)),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = palette.container,
                        selectedLabelColor = palette.onContainer,
                        selectedLeadingIconColor = palette.accent,
                    ),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("form_entry_type_${entryType.code.lowercase()}"),
                )
            }
        }

        Text(
            text = stringResource(R.string.entry_form_color_section),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            EventColorTag.entries.forEach { colorTag ->
                val palette = CalendarEntryVisuals.palette(colorTag)
                val isSelected = selectedColorTag == colorTag
                val label = stringResource(CalendarEntryVisuals.colorLabelRes(colorTag))

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .clip(CircleShape)
                        .clickable(
                            role = Role.RadioButton,
                            onClick = { onSelectColorTag(colorTag) },
                        )
                        .semantics { contentDescription = label }
                        .testTag("form_color_${colorTag.code.lowercase()}"),
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(palette.accent)
                            .then(
                                if (isSelected) {
                                    Modifier.border(
                                        BorderStroke(3.dp, MaterialTheme.colorScheme.onSurface),
                                        CircleShape,
                                    )
                                } else {
                                    Modifier
                                }
                            ),
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.surface,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
