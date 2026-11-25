package com.example.feature.calendar

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.domain.model.CalendarEvent
import com.example.domain.model.EventTypeDetails
import com.example.domain.service.PolishDateFormatter
import com.example.feature.map.leaflet.LeafletMapController
import com.example.feature.map.leaflet.LeafletMapMarker
import com.example.feature.map.leaflet.LeafletMapView
import com.example.ui.theme.AppIcons

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EventDetailDialog(
    event: CalendarEvent,
    onDismiss: () -> Unit,
    onToggleTaskCompleted: () -> Unit,
    onEditClick: () -> Unit,
    onReminderClick: () -> Unit,
    onShowOnMainMap: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    val palette = CalendarEntryVisuals.palette(event.colorTag)
    val location = event.location
    val locationSummary = location?.formatSummary()
    val detailMapController = remember(event.id) { LeafletMapController() }
    val isTask = event.details is EventTypeDetails.Task
    val isCompleted = event.isCompletedTask

    val detailSubtitle = remember(event) {
        buildString {
            append(PolishDateFormatter.formatDateKeyDisplay(event.dateKey))
            event.timeRange?.let { range ->
                if (!event.isAllDay) {
                    append(" • ")
                    append(range.formatDisplay())
                }
            }
            event.reminderTime?.let { reminder ->
                append(" • ")
                append(reminder.formatHHmm())
            }
            location?.sanitizedLabel?.let { label ->
                append(" • ")
                append(label)
            }
        }
    }

    val markerList = remember(event, detailSubtitle) {
        if (location != null) {
            listOf(
                LeafletMapMarker(
                    id = event.id,
                    title = event.title,
                    subtitle = detailSubtitle,
                    point = location.point,
                    hasReminder = event.hasReminder,
                )
            )
        } else {
            emptyList()
        }
    }

    LaunchedEffect(event.id, location) {
        if (location != null) {
            detailMapController.focusEvent(event.id)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .widthIn(max = 580.dp)
                .testTag("event_detail_dialog"),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = palette.container,
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(
                                painter = painterResource(CalendarEntryVisuals.iconRes(event.entryType)),
                                contentDescription = null,
                                tint = palette.onContainer,
                                modifier = Modifier.size(16.dp),
                            )
                            Text(
                                text = stringResource(CalendarEntryVisuals.labelRes(event.entryType)),
                                style = MaterialTheme.typography.labelLarge,
                                color = palette.onContainer,
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("event_detail_close_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = stringResource(R.string.event_detail_close),
                        )
                    }
                }

                Text(
                    text = event.title,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.testTag("event_detail_event_title"),
                )

                EventDetailInfoCard(
                    event = event,
                    palette = palette,
                    locationSummary = locationSummary,
                )

                EventDetailTypeSection(event = event)

                if (location != null) {
                    LeafletMapView(
                        controller = detailMapController,
                        markers = markerList,
                        fitBoundsOnMarkersChange = true,
                        initialCenter = location.point,
                        initialZoom = 14,
                        enableTapSelection = false,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .testTag("event_detail_leaflet_map"),
                    )
                } else {
                    NoLocationPlaceholderCard(onAddLocationClick = onEditClick)
                }

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (isTask) {
                        FilledTonalButton(
                            onClick = onToggleTaskCompleted,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("event_detail_toggle_task_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                stringResource(
                                    if (isCompleted) R.string.task_status_pending else R.string.task_status_completed
                                )
                            )
                        }
                    }

                    FilledTonalButton(
                        onClick = onEditClick,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("event_detail_edit_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.cd_edit_event))
                    }

                    OutlinedButton(
                        onClick = onReminderClick,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("event_detail_reminder_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Notifications,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.reminder_toggle_label))
                    }

                    if (location != null) {
                        OutlinedButton(
                            onClick = onShowOnMainMap,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("event_detail_show_on_map_button"),
                        ) {
                            Icon(
                                painter = painterResource(AppIcons.map),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.map_show_on_map))
                        }
                    }

                    TextButton(
                        onClick = onDeleteClick,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("event_detail_delete_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.action_delete),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EventDetailInfoCard(
    event: CalendarEvent,
    palette: EntryColorPalette,
    locationSummary: String?,
) {
    val startDateText = PolishDateFormatter.formatDateKeyDisplay(event.dateKey)
    val fullDateText = if (event.isMultiDay) {
        "$startDateText – ${PolishDateFormatter.formatDateKeyDisplay(event.effectiveEndDateKey)}"
    } else {
        startDateText
    }
    val scheduleText = buildString {
        append(fullDateText)
        if (event.isAllDay) {
            append(" • ")
            append(stringResource(R.string.entry_all_day_label))
        } else if (event.timeRange != null) {
            append(" • ")
            append(event.timeRange.formatDisplay())
        }
        if (event.recurrence.isRepeating) {
            append(" • ")
            append(stringResource(CalendarEntryVisuals.recurrenceLabelRes(event.recurrence)))
        }
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    painter = painterResource(AppIcons.calendar),
                    contentDescription = null,
                    tint = palette.accent,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = scheduleText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    imageVector = if (event.hasReminder) {
                        Icons.Filled.Notifications
                    } else {
                        Icons.Outlined.Notifications
                    },
                    contentDescription = null,
                    tint = if (event.hasReminder) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = event.reminderTime?.let {
                        stringResource(R.string.reminder_badge, it.formatHHmm())
                    } ?: stringResource(R.string.event_detail_no_reminder),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (event.hasReminder) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.LocationOn,
                    contentDescription = null,
                    tint = if (locationSummary != null) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = locationSummary ?: stringResource(R.string.event_detail_no_location),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (locationSummary != null) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun NoLocationPlaceholderCard(
    onAddLocationClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 18.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(28.dp),
                )
                Text(
                    text = stringResource(R.string.event_detail_no_location),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FilledTonalButton(
                    onClick = onAddLocationClick,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("event_detail_add_location_button"),
                ) {
                    Text(stringResource(R.string.event_detail_add_location))
                }
            }
        }
    }
}
