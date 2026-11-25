package com.example.feature.calendar

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.model.CalendarEvent
import com.example.domain.model.EventTypeDetails
import com.example.domain.service.PolishDateFormatter
import com.example.ui.components.MetadataBadge

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun EventItemCard(
    event: CalendarEvent,
    onEventClick: () -> Unit,
    onToggleTaskCompleted: () -> Unit,
    onLocationPinClick: () -> Unit,
    onReminderClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = CalendarEntryVisuals.palette(event.colorTag)
    val locationSummary = event.location?.formatSummary()
    val dateLine = buildDateScheduleLine(event)
    val highlightChip = buildTypeHighlightText(event)
    val isTask = event.details is EventTypeDetails.Task
    val isCompleted = event.isCompletedTask

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 1.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onEventClick)
            .testTag("event_item_${event.id.value}"),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
        ) {
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(palette.accent),
            )

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 14.dp, end = 8.dp, top = 14.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    if (isTask) {
                        val toggleCd = stringResource(R.string.cd_toggle_task_completed)
                        Checkbox(
                            checked = isCompleted,
                            onCheckedChange = { onToggleTaskCompleted() },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .semantics { contentDescription = toggleCd }
                                .testTag("task_checkbox_${event.id.value}"),
                        )
                    } else {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = palette.container,
                            modifier = Modifier.size(42.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    painter = painterResource(CalendarEntryVisuals.iconRes(event.entryType)),
                                    contentDescription = stringResource(CalendarEntryVisuals.labelRes(event.entryType)),
                                    tint = palette.onContainer,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = palette.container,
                            ) {
                                Text(
                                    text = stringResource(CalendarEntryVisuals.labelRes(event.entryType)),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.onContainer,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                )
                            }
                            if (event.recurrence.isRepeating) {
                                Text(
                                    text = stringResource(CalendarEntryVisuals.recurrenceLabelRes(event.recurrence)),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = event.title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                            ),
                            color = if (isCompleted) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )

                        Spacer(modifier = Modifier.height(3.dp))

                        Text(
                            text = dateLine,
                            style = MaterialTheme.typography.labelMedium,
                            color = palette.accent,
                        )

                        if (event.hasDescription) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = event.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }

                        if (highlightChip != null || event.hasReminder || locationSummary != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                if (highlightChip != null) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    ) {
                                        Text(
                                            text = highlightChip,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        )
                                    }
                                }
                                event.reminderTime?.let { reminder ->
                                    MetadataBadge(
                                        text = reminder.formatHHmm(),
                                        icon = Icons.Filled.Notifications,
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                    )
                                }
                                if (locationSummary != null) {
                                    MetadataBadge(
                                        text = event.location?.sanitizedLabel ?: locationSummary,
                                        icon = Icons.Filled.LocationOn,
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    )
                                }
                            }
                        }
                    }

                    IconButton(
                        onClick = onReminderClick,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("event_reminder_button_${event.id.value}"),
                    ) {
                        Icon(
                            imageVector = if (event.hasReminder) {
                                Icons.Filled.Notifications
                            } else {
                                Icons.Outlined.Notifications
                            },
                            contentDescription = stringResource(R.string.cd_reminder_button),
                            tint = if (event.hasReminder) {
                                MaterialTheme.colorScheme.secondary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 8.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = onLocationPinClick,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("event_map_button_${event.id.value}"),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LocationOn,
                            contentDescription = stringResource(R.string.cd_event_location_pin),
                            tint = if (event.hasLocation) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    IconButton(
                        onClick = onEditClick,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("event_edit_button_${event.id.value}"),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = stringResource(R.string.cd_edit_event),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("event_delete_button_${event.id.value}"),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = stringResource(R.string.cd_delete_event),
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun buildDateScheduleLine(event: CalendarEvent): String {
    val startText = PolishDateFormatter.formatDateKeyDisplay(event.dateKey)
    val dateRange = if (event.isMultiDay) {
        "$startText – ${PolishDateFormatter.formatDateKeyDisplay(event.effectiveEndDateKey)}"
    } else {
        startText
    }
    val timeSuffix = when {
        event.isAllDay -> stringResource(R.string.entry_all_day_label)
        event.timeRange != null -> event.timeRange.formatDisplay()
        else -> null
    }
    return if (timeSuffix != null) "$dateRange • $timeSuffix" else dateRange
}

@Composable
private fun buildTypeHighlightText(event: CalendarEvent): String? =
    when (val details = event.details) {
        is EventTypeDetails.StandardEvent -> details.guests.takeIf { it.isNotEmpty() }?.let { guests ->
            "${stringResource(R.string.detail_guests_label)}: ${guests.size}"
        }
        is EventTypeDetails.Task -> {
            val priority = stringResource(CalendarEntryVisuals.priorityLabelRes(details.priority))
            if (details.checklist.isNotEmpty()) {
                "$priority • ${details.checklist.size}"
            } else {
                priority
            }
        }
        is EventTypeDetails.Birthday -> {
            val targetYear = event.dateKey.toCalendarDay()?.year
            val ageText = targetYear?.let { details.turningAgeIn(it) }?.let { age ->
                stringResource(R.string.detail_turning_age_badge, age)
            }
            ageText ?: details.celebrantName
        }
        is EventTypeDetails.Appointment -> details.specialistName
        is EventTypeDetails.Trip -> {
            val transport = stringResource(CalendarEntryVisuals.transportLabelRes(details.transportMode))
            if (!details.bookingReference.isNullOrBlank()) {
                "$transport • ${details.bookingReference}"
            } else {
                transport
            }
        }
    }
