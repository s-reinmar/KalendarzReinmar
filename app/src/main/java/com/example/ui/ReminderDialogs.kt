package com.example.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.data.CalendarEventEntity
import com.example.reminder.ReminderScheduler

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderTimePickerDialog(
    initialMinutes: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    val clamped = initialMinutes.coerceIn(0, 23 * 60 + 59)
    val timePickerState = rememberTimePickerState(
        initialHour = clamped / 60,
        initialMinute = clamped % 60,
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
                onClick = { onConfirm(timePickerState.hour * 60 + timePickerState.minute) },
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventReminderDialog(
    event: CalendarEventEntity,
    onDismiss: () -> Unit,
    onSave: (reminderTimeMinutes: Int?, location: EventLocation?) -> Unit,
) {
    val initial = (event.reminderTimeMinutes ?: ReminderScheduler.DEFAULT_REMINDER_MINUTES)
        .coerceIn(0, 23 * 60 + 59)
    val timePickerState = rememberTimePickerState(
        initialHour = initial / 60,
        initialMinute = initial % 60,
        is24Hour = true,
    )
    var selectedLocation by remember(event.id) {
        mutableStateOf(
            if (event.locationLat != null && event.locationLng != null) {
                EventLocation(
                    lat = event.locationLat,
                    lng = event.locationLng,
                    label = event.locationLabel,
                )
            } else {
                null
            }
        )
    }
    var showMapPicker by remember { mutableStateOf(false) }

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
                    text = "${event.title} • ${DateUtils.formatDateKeyDisplay(event.dateKey)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                )
                TimePicker(state = timePickerState)
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
                    onSave(
                        timePickerState.hour * 60 + timePickerState.minute,
                        selectedLocation,
                    )
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
                if (event.reminderTimeMinutes != null) {
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
            onDismiss = { showMapPicker = false },
            onConfirm = { picked ->
                selectedLocation = picked
                showMapPicker = false
            },
        )
    }
}

@Composable
internal fun ReminderLocationFieldCard(
    location: EventLocation?,
    onClick: () -> Unit,
    testTag: String,
) {
    val summary = location?.let {
        ReminderScheduler.formatLocationSummary(it.lat, it.lng, it.label)
    }
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
