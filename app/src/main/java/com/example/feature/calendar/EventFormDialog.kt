package com.example.feature.calendar

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
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.model.CalendarDay
import com.example.domain.model.CalendarEvent
import com.example.domain.model.DateKey
import com.example.domain.model.EventLocation
import com.example.domain.model.ReminderTime
import com.example.domain.port.DeviceLocationProvider
import com.example.domain.port.GeocodingService
import com.example.domain.service.CalendarGridBuilder
import com.example.domain.service.PolishDateFormatter
import com.example.feature.map.MapLocationPickerDialog
import com.example.ui.theme.AppIcons

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventFormDialog(
    initialDateKey: DateKey,
    todayDay: CalendarDay,
    geocodingService: GeocodingService,
    locationProvider: DeviceLocationProvider,
    existingEvent: CalendarEvent? = null,
    initialLocation: EventLocation? = null,
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        dateKey: DateKey,
        reminderTime: ReminderTime?,
        location: EventLocation?,
    ) -> Unit,
) {
    var title by remember(existingEvent?.id) { mutableStateOf(existingEvent?.title.orEmpty()) }
    var selectedDateKey by remember(existingEvent?.id, initialDateKey) {
        mutableStateOf(existingEvent?.dateKey ?: initialDateKey)
    }
    var showDatePicker by remember { mutableStateOf(false) }
    var reminderEnabled by remember(existingEvent?.id) {
        mutableStateOf(existingEvent?.hasReminder == true)
    }
    var reminderTime by remember(existingEvent?.id) {
        mutableStateOf(existingEvent?.reminderTime ?: ReminderTime.DEFAULT)
    }
    var showTimePicker by remember { mutableStateOf(false) }
    var selectedLocation by remember(existingEvent?.id, initialLocation) {
        mutableStateOf(existingEvent?.location ?: initialLocation)
    }
    var showMapPicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(
                    if (existingEvent == null) R.string.calendar_add_event else R.string.calendar_edit_event,
                ),
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.calendar_event_title_label)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("event_title_input"),
                )

                OutlinedCard(
                    onClick = { showDatePicker = true },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .testTag("event_date_picker_button"),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text(
                                text = stringResource(R.string.calendar_event_date_label),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = PolishDateFormatter.formatDateKeyDisplay(selectedDateKey),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        Icon(
                            painter = painterResource(AppIcons.calendar),
                            contentDescription = stringResource(R.string.calendar_select_date),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }

                ReminderLocationFieldCard(
                    location = selectedLocation,
                    onClick = { showMapPicker = true },
                    testTag = "event_reminder_location_button",
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = stringResource(R.string.reminder_toggle_label),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Switch(
                        checked = reminderEnabled,
                        onCheckedChange = { reminderEnabled = it },
                        modifier = Modifier.testTag("event_reminder_switch"),
                    )
                }

                if (reminderEnabled) {
                    OutlinedCard(
                        onClick = { showTimePicker = true },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .testTag("event_reminder_time_button"),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column {
                                Text(
                                    text = stringResource(R.string.reminder_time_label),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = reminderTime.formatHHmm(),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                            Icon(
                                imageVector = Icons.Filled.Notifications,
                                contentDescription = stringResource(R.string.reminder_select_time),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (title.trim().isNotEmpty()) {
                        onSave(
                            title,
                            selectedDateKey,
                            if (reminderEnabled) reminderTime else null,
                            selectedLocation,
                        )
                    }
                },
                enabled = title.trim().isNotEmpty(),
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("event_save_button"),
            ) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("event_cancel_button"),
            ) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = CalendarGridBuilder.dateKeyToUtcMillis(selectedDateKey, todayDay),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            selectedDateKey = CalendarGridBuilder.utcMillisToDateKey(millis)
                        }
                        showDatePicker = false
                    },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("date_picker_confirm_button"),
                ) {
                    Text(stringResource(R.string.action_save))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDatePicker = false },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("date_picker_cancel_button"),
                ) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        ReminderTimePickerDialog(
            initialTime = reminderTime,
            onDismiss = { showTimePicker = false },
            onConfirm = { picked ->
                reminderTime = picked
                showTimePicker = false
            },
        )
    }

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
