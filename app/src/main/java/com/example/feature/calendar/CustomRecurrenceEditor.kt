package com.example.feature.calendar

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.model.CustomRecurrenceUnit
import com.example.domain.model.EventRecurrence
import com.example.ui.theme.AppIcons

@Composable
internal fun CustomRecurrenceEditor(
    customRecurrence: EventRecurrence.Custom,
    onChange: (EventRecurrence.Custom) -> Unit,
    modifier: Modifier = Modifier,
) {
    var intervalText by remember(customRecurrence.interval) {
        mutableStateOf(customRecurrence.interval.toString())
    }

    LaunchedEffect(customRecurrence.interval) {
        val currentInt = intervalText.toIntOrNull()
        if (currentInt != customRecurrence.interval) {
            intervalText = customRecurrence.interval.toString()
        }
    }

    val summaryText = CalendarEntryVisuals.recurrenceLabel(customRecurrence)

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
            .fillMaxWidth()
            .testTag("custom_recurrence_editor"),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = stringResource(R.string.recurrence_custom_summary, summaryText),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.testTag("custom_recurrence_summary_text"),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(R.string.recurrence_custom_interval_label),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )

                FilledTonalIconButton(
                    onClick = {
                        val next = (customRecurrence.interval - 1)
                            .coerceAtLeast(EventRecurrence.MIN_INTERVAL)
                        onChange(customRecurrence.copy(interval = next))
                    },
                    enabled = customRecurrence.interval > EventRecurrence.MIN_INTERVAL,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("custom_recurrence_decrement"),
                ) {
                    Icon(
                        painter = painterResource(AppIcons.remove),
                        contentDescription = stringResource(R.string.recurrence_custom_decrease_cd),
                    )
                }

                OutlinedTextField(
                    value = intervalText,
                    onValueChange = { raw ->
                        val digits = raw.filter(Char::isDigit).take(3)
                        intervalText = digits
                        val parsed = digits.toIntOrNull()
                        if (parsed != null && parsed >= EventRecurrence.MIN_INTERVAL) {
                            onChange(
                                EventRecurrence.custom(
                                    interval = parsed,
                                    unit = customRecurrence.unit,
                                )
                            )
                        }
                    },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.titleMedium.copy(
                        textAlign = TextAlign.Center,
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .width(76.dp)
                        .testTag("custom_recurrence_interval_input"),
                )

                FilledTonalIconButton(
                    onClick = {
                        val next = (customRecurrence.interval + 1)
                            .coerceAtMost(EventRecurrence.MAX_INTERVAL)
                        onChange(customRecurrence.copy(interval = next))
                    },
                    enabled = customRecurrence.interval < EventRecurrence.MAX_INTERVAL,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("custom_recurrence_increment"),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = stringResource(R.string.recurrence_custom_increase_cd),
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = customRecurrence.unit == CustomRecurrenceUnit.HOURS,
                    onClick = {
                        onChange(customRecurrence.copy(unit = CustomRecurrenceUnit.HOURS))
                    },
                    label = {
                        Text(
                            text = stringResource(R.string.recurrence_custom_unit_hours),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("custom_recurrence_unit_hours"),
                )
                FilterChip(
                    selected = customRecurrence.unit == CustomRecurrenceUnit.DAYS,
                    onClick = {
                        onChange(customRecurrence.copy(unit = CustomRecurrenceUnit.DAYS))
                    },
                    label = {
                        Text(
                            text = stringResource(R.string.recurrence_custom_unit_days),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("custom_recurrence_unit_days"),
                )
            }
        }
    }
}
