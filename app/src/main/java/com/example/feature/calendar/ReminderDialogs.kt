package com.example.feature.calendar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.model.CalendarEvent
import com.example.domain.model.EventLocation
import com.example.domain.model.ReminderTime
import com.example.domain.port.DeviceLocationProvider
import com.example.domain.port.GeocodingService
import com.example.domain.service.PolishDateFormatter
import com.example.domain.service.ReminderTriggerCalculator
import com.example.feature.map.MapLocationPickerDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderTimePickerDialog(
    initialTime: ReminderTime,
    onDismiss: () -> Unit,
    onConfirm: (ReminderTime) -> Unit,
) {
    val timePickerState = rememberTimePickerState(
        initialHour = initialTime.hour,
        initialMinute = initialTime.minute,
        is24Hour = true,
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.reminder_select_time),
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                TimePicker(state = timePickerState)
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(ReminderTime.fromHourMinute(timePickerState.hour, timePickerState.minute))
                },
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("time_picker_confirm_button"),
            ) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("time_picker_cancel_button"),
            ) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EventReminderDialog(
    event: CalendarEvent,
    geocodingService: GeocodingService,
    locationProvider: DeviceLocationProvider,
    defaultReminderTime: ReminderTime = ReminderTime.DEFAULT,
    nowMillis: Long = System.currentTimeMillis(),
    onDismiss: () -> Unit,
    onSave: (reminderTime: ReminderTime?, location: EventLocation?) -> Unit,
) {
    val initial = event.reminderTime ?: defaultReminderTime
    var pickerSeedTime by remember(event.id) { mutableStateOf(initial) }
    var pickerGeneration by remember(event.id) { mutableIntStateOf(0) }
    val timePickerState = key(pickerGeneration) {
        rememberTimePickerState(
            initialHour = pickerSeedTime.hour,
            initialMinute = pickerSeedTime.minute,
            is24Hour = true,
        )
    }

    var selectedLocation by remember(event.id) { mutableStateOf(event.location) }
    var showMapPicker by remember { mutableStateOf(false) }

    val selectedReminderTime = ReminderTime.fromHourMinute(
        hour = timePickerState.hour,
        minute = timePickerState.minute,
    )
    val nextTriggerMillis = remember(event.dateKey, event.recurrence, selectedReminderTime, nowMillis) {
        ReminderTriggerCalculator.computeNextTriggerMillis(
            dateKey = event.dateKey,
            recurrence = event.recurrence,
            reminderTime = selectedReminderTime,
            nowMillis = nowMillis,
        )
    }
    val timedStart = if (!event.isAllDay) event.timeRange?.start else null
    val preset15m = timedStart?.let { ReminderTriggerCalculator.computeOffsetReminderTime(it, 15) }
    val preset1h = timedStart?.let { ReminderTriggerCalculator.computeOffsetReminderTime(it, 60) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.reminder_dialog_title),
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = "${event.title} • ${PolishDateFormatter.formatDateKeyDisplay(event.dateKey)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    FilterChip(
                        selected = selectedReminderTime == defaultReminderTime,
                        onClick = {
                            pickerSeedTime = defaultReminderTime
                            pickerGeneration++
                        },
                        label = {
                            Text(
                                stringResource(
                                    R.string.reminder_preset_default,
                                    defaultReminderTime.formatHHmm(),
                                ),
                            )
                        },
                        modifier = Modifier.testTag("reminder_preset_default_chip"),
                    )
                    if (preset15m != null) {
                        FilterChip(
                            selected = selectedReminderTime == preset15m,
                            onClick = {
                                pickerSeedTime = preset15m
                                pickerGeneration++
                            },
                            label = { Text(stringResource(R.string.reminder_preset_15m_before)) },
                            modifier = Modifier.testTag("reminder_preset_15m_chip"),
                        )
                    }
                    if (preset1h != null) {
                        FilterChip(
                            selected = selectedReminderTime == preset1h,
                            onClick = {
                                pickerSeedTime = preset1h
                                pickerGeneration++
                            },
                            label = { Text(stringResource(R.string.reminder_preset_1h_before)) },
                            modifier = Modifier.testTag("reminder_preset_1h_chip"),
                        )
                    }
                }

                TimePicker(state = timePickerState)

                NextTriggerPreviewBanner(nextTriggerMillis = nextTriggerMillis)

                ReminderLocationFieldCard(
                    location = selectedLocation,
                    onClick = { showMapPicker = true },
                    testTag = "reminder_dialog_location_button",
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(selectedReminderTime, selectedLocation)
                },
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("reminder_dialog_save_button"),
            ) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (event.hasReminder) {
                    TextButton(
                        onClick = { onSave(null, selectedLocation) },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("reminder_dialog_remove_button"),
                    ) {
                        Text(
                            text = stringResource(R.string.reminder_remove),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("reminder_dialog_cancel_button"),
                ) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        },
    )

    if (showMapPicker) {
        MapLocationPickerDialog(
            initialLocation = selectedLocation,
            geocodingService = geocodingService,
            locationProvider = locationProvider,
            onDismiss = { showMapPicker = false },
            onConfirm = { picked ->
                selectedLocation = picked
                showMapPicker = false
            },
        )
    }
}

@Composable
private fun NextTriggerPreviewBanner(
    nextTriggerMillis: Long?,
) {
    val isScheduled = nextTriggerMillis != null
    val containerColor = if (isScheduled) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.errorContainer
    }
    val contentColor = if (isScheduled) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onErrorContainer
    }
    val previewText = if (nextTriggerMillis != null) {
        stringResource(
            R.string.reminder_next_trigger_preview,
            PolishDateFormatter.formatDateTimeMillis(nextTriggerMillis),
        )
    } else {
        stringResource(R.string.reminder_past_warning)
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("reminder_next_trigger_preview"),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Notifications,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = previewText,
                style = MaterialTheme.typography.bodySmall,
                color = contentColor,
            )
        }
    }
}

@Composable
internal fun ReminderLocationFieldCard(
    location: EventLocation?,
    onClick: () -> Unit,
    testTag: String,
) {
    val summary = location?.formatSummary()
    OutlinedCard(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .testTag(testTag),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.location_field_label),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = summary ?: stringResource(R.string.location_not_selected),
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (summary != null) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                imageVector = Icons.Filled.LocationOn,
                contentDescription = stringResource(R.string.cd_select_location),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
