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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.example.ui.theme.AppIcons
import java.util.Locale

private val RGB_WHEEL_COLORS = listOf(
    Color(0xFFE53935),
    Color(0xFFFB8C00),
    Color(0xFFFDD835),
    Color(0xFF43A047),
    Color(0xFF1E88E5),
    Color(0xFF8E24AA),
    Color(0xFFE53935),
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun EventFormTypeSection(
    selectedType: CalendarEntryType,
    selectedColorTag: EventColorTag,
    onSelectType: (CalendarEntryType) -> Unit,
    onSelectColorTag: (EventColorTag) -> Unit,
) {
    var showRgbPicker by remember { mutableStateOf(false) }

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
                        .testTag("form_entry_type_${entryType.code.lowercase(Locale.US)}"),
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.entry_form_color_section),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = CalendarEntryVisuals.palette(selectedColorTag).container,
            ) {
                Text(
                    text = CalendarEntryVisuals.colorLabel(selectedColorTag),
                    style = MaterialTheme.typography.labelSmall,
                    color = CalendarEntryVisuals.palette(selectedColorTag).onContainer,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                )
            }
        }

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            EventColorTag.presets.forEach { colorTag ->
                val palette = CalendarEntryVisuals.palette(colorTag)
                val isSelected = selectedColorTag == colorTag
                val label = CalendarEntryVisuals.colorLabel(colorTag)
                val checkTint = CalendarEntryVisuals.contrastingOnColor(
                    colorTag.red,
                    colorTag.green,
                    colorTag.blue,
                )

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
                        .testTag("form_color_${colorTag.code.lowercase(Locale.US)}"),
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
                                tint = checkTint,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }

            val isCustomSelected = selectedColorTag is EventColorTag.CustomRgb
            val customPickerCd = stringResource(R.string.entry_color_picker_open_cd)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .clip(CircleShape)
                    .clickable(
                        role = Role.Button,
                        onClick = { showRgbPicker = true },
                    )
                    .semantics { contentDescription = customPickerCd }
                    .testTag("form_color_custom_rgb_swatch"),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(
                            if (isCustomSelected) {
                                Color(selectedColorTag.red, selectedColorTag.green, selectedColorTag.blue)
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHigh
                            }
                        )
                        .border(
                            width = if (isCustomSelected) 3.dp else 2.5.dp,
                            brush = if (isCustomSelected) {
                                Brush.linearGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.onSurface,
                                        MaterialTheme.colorScheme.onSurface,
                                    )
                                )
                            } else {
                                Brush.sweepGradient(RGB_WHEEL_COLORS)
                            },
                            shape = CircleShape,
                        ),
                ) {
                    if (isCustomSelected) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            tint = CalendarEntryVisuals.contrastingOnColor(
                                selectedColorTag.red,
                                selectedColorTag.green,
                                selectedColorTag.blue,
                            ),
                            modifier = Modifier.size(18.dp),
                        )
                    } else {
                        Icon(
                            painter = painterResource(AppIcons.palette),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(17.dp),
                        )
                    }
                }
            }
        }

        OutlinedButton(
            onClick = { showRgbPicker = true },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .minimumInteractiveComponentSize()
                .testTag("form_color_open_rgb_picker_button"),
        ) {
            Icon(
                painter = painterResource(AppIcons.palette),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (selectedColorTag is EventColorTag.CustomRgb) {
                    "${stringResource(R.string.entry_color_custom_rgb)}: ${selectedColorTag.hexString}"
                } else {
                    stringResource(R.string.entry_color_custom_rgb)
                },
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }

    if (showRgbPicker) {
        RgbColorPickerDialog(
            initialColor = selectedColorTag,
            onDismiss = { showRgbPicker = false },
            onConfirm = { picked ->
                onSelectColorTag(picked)
                showRgbPicker = false
            },
        )
    }
}
