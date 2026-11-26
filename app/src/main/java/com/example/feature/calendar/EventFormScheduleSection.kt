package com.example.feature.calendar

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
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
import com.example.domain.model.CalendarEntryType
import com.example.domain.model.CustomRecurrenceUnit
import com.example.domain.model.DateKey
import com.example.domain.model.EventLocation
import com.example.domain.model.EventRecurrence
import com.example.domain.model.ReminderTime
import com.example.domain.model.TimeOfDay
import com.example.domain.service.PolishDateFormatter
import com.example.ui.theme.AppIcons
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun EventFormScheduleSection(
    entryType: CalendarEntryType,
    startDateKey: DateKey,
    endDateKey: DateKey?,
    isAllDay: Boolean,
    startTime: TimeOfDay,
    endTime: TimeOfDay,
    recurrence: EventRecurrence,
    reminderEnabled: Boolean,
    reminderTime: ReminderTime,
    selectedLocation: EventLocation?,
    onPickStartDate: () -> Unit,
    onPickEndDate: () -> Unit,
    onClearEndDate: () -> Unit,
    onAllDayChange: (Boolean) -> Unit,
    onPickStartTime: () -> Unit,
    onPickEndTime: () -> Unit,
    onRecurrenceChange: (EventRecurrence) -> Unit,
    onReminderToggle: (Boolean) -> Unit,
    onPickReminderTime: () -> Unit,
    onPickLocation: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = stringResource(R.string.entry_form_schedule_section),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        OutlinedCard(
            onClick = onPickStartDate,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .minimumInteractiveComponentSize()
                .testTag("event_date_picker_button"),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        text = stringResource(
                            if (entryType.supportsEndDate) {
                                R.string.entry_start_date_label
                            } else {
                                R.string.calendar_event_date_label
                            }
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = PolishDateFormatter.formatDateKeyDisplay(startDateKey),
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

        if (entryType.supportsEndDate) {
            OutlinedCard(
                onClick = onPickEndDate,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumInteractiveComponentSize()
                    .testTag("event_end_date_picker_button"),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 14.dp, end = 6.dp, top = 10.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.entry_end_date_label),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = endDateKey?.let(PolishDateFormatter::formatDateKeyDisplay)
                                ?: stringResource(R.string.entry_end_date_same_day),
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (endDateKey != null) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                    if (endDateKey != null) {
                        IconButton(
                            onClick = onClearEndDate,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("event_clear_end_date_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = stringResource(R.string.entry_end_date_clear),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    } else {
                        Icon(
                            painter = painterResource(AppIcons.calendar),
                            contentDescription = stringResource(R.string.entry_end_date_label),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .size(20.dp),
                        )
                    }
                }
            }
        }

        if (entryType.supportsTimeRange) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.entry_all_day_label),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Switch(
                    checked = isAllDay,
                    onCheckedChange = onAllDayChange,
                    modifier = Modifier.testTag("event_all_day_switch"),
                )
            }

            if (!isAllDay) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TimeFieldCard(
                        label = stringResource(R.string.entry_start_time_label),
                        timeText = startTime.formatHHmm(),
                        onClick = onPickStartTime,
                        testTag = "event_start_time_button",
                        modifier = Modifier.weight(1f),
                    )
                    TimeFieldCard(
                        label = stringResource(R.string.entry_end_time_label),
                        timeText = endTime.formatHHmm(),
                        onClick = onPickEndTime,
                        testTag = "event_end_time_button",
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        var lastCustomRecurrence by remember {
            mutableStateOf(
                (recurrence as? EventRecurrence.Custom)
                    ?: EventRecurrence.custom(
                        interval = EventRecurrence.DEFAULT_CUSTOM_INTERVAL,
                        unit = CustomRecurrenceUnit.DAYS,
                    )
            )
        }
        if (recurrence is EventRecurrence.Custom) {
            lastCustomRecurrence = recurrence
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.entry_recurrence_label),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                EventRecurrence.standardOptions.forEach { option ->
                    FilterChip(
                        selected = recurrence == option,
                        onClick = { onRecurrenceChange(option) },
                        label = {
                            Text(
                                text = CalendarEntryVisuals.recurrenceLabel(option),
                                style = MaterialTheme.typography.labelMedium,
                            )
                        },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("form_recurrence_${option.code.lowercase(Locale.US)}"),
                    )
                }
                FilterChip(
                    selected = recurrence is EventRecurrence.Custom,
                    onClick = {
                        if (recurrence !is EventRecurrence.Custom) {
                            onRecurrenceChange(lastCustomRecurrence)
                        }
                    },
                    label = {
                        Text(
                            text = stringResource(R.string.recurrence_custom),
                            style = MaterialTheme.typography.labelMedium,
                        )
                    },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("form_recurrence_custom"),
                )
            }

            AnimatedVisibility(visible = recurrence is EventRecurrence.Custom) {
                CustomRecurrenceEditor(
                    customRecurrence = (recurrence as? EventRecurrence.Custom) ?: lastCustomRecurrence,
                    onChange = { updated ->
                        lastCustomRecurrence = updated
                        onRecurrenceChange(updated)
                    },
                )
            }
        }

        ReminderLocationFieldCard(
            location = selectedLocation,
            onClick = onPickLocation,
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
                onCheckedChange = onReminderToggle,
                modifier = Modifier.testTag("event_reminder_switch"),
            )
        }

        if (reminderEnabled) {
            OutlinedCard(
                onClick = onPickReminderTime,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumInteractiveComponentSize()
                    .testTag("event_reminder_time_button"),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
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
}

@Composable
private fun TimeFieldCard(
    label: String,
    timeText: String,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier,
) {
    OutlinedCard(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .minimumInteractiveComponentSize()
            .testTag(testTag),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = timeText,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
