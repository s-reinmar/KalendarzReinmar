package com.example.ui

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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.CalendarEventEntity
import com.example.reminder.ReminderScheduler
import com.example.ui.theme.AppIcons

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EventDetailDialog(
    event: CalendarEventEntity,
    onDismiss: () -> Unit,
    onEditClick: () -> Unit,
    onReminderClick: () -> Unit,
    onShowOnMainMap: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    val hasValidLocation = event.locationLat != null && event.locationLng != null &&
        event.locationLat.isFinite() && event.locationLng.isFinite() &&
        event.locationLat in -90.0..90.0 && event.locationLng in -180.0..180.0
    val locationSummary = ReminderScheduler.formatLocationSummary(
        event.locationLat,
        event.locationLng,
        event.locationLabel,
    )
    val detailMapController = remember(event.id) { LeafletMapController() }
    val detailSubtitle = remember(event) {
        buildString {
            append(DateUtils.formatDateKeyDisplay(event.dateKey))
            if (event.reminderTimeMinutes != null) {
                append(" • ")
                append(ReminderScheduler.formatMinutesOfDay(event.reminderTimeMinutes))
            }
            if (!event.locationLabel.isNullOrBlank()) {
                append(" • ")
                append(event.locationLabel)
            }
        }
    }
    val markerList = remember(event, hasValidLocation, detailSubtitle) {
        if (hasValidLocation) {
            listOf(
                LeafletMapMarker(
                    id = event.id,
                    title = event.title,
                    subtitle = detailSubtitle,
                    lat = event.locationLat!!,
                    lng = event.locationLng!!,
                    hasReminder = event.reminderTimeMinutes != null,
                )
            )
        } else {
            emptyList()
        }
    }

    LaunchedEffect(event.id, hasValidLocation) {
        if (hasValidLocation) {
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
                    Text(
                        text = stringResource(R.string.event_detail_title),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
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
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.testTag("event_detail_event_title"),
                )

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
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                text = DateUtils.formatDateKeyDisplay(event.dateKey),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Icon(
                                imageVector = if (event.reminderTimeMinutes != null) {
                                    Icons.Filled.Notifications
                                } else {
                                    Icons.Outlined.Notifications
                                },
                                contentDescription = null,
                                tint = if (event.reminderTimeMinutes != null) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                text = if (event.reminderTimeMinutes != null) {
                                    stringResource(
                                        R.string.reminder_badge,
                                        ReminderScheduler.formatMinutesOfDay(event.reminderTimeMinutes),
                                    )
                                } else {
                                    stringResource(R.string.event_detail_no_reminder)
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (event.reminderTimeMinutes != null) {
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

                if (hasValidLocation) {
                    LeafletMapView(
                        controller = detailMapController,
                        markers = markerList,
                        fitBoundsOnMarkersChange = true,
                        initialCenterLat = event.locationLat!!,
                        initialCenterLng = event.locationLng!!,
                        initialZoom = 14,
                        enableTapSelection = false,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .testTag("event_detail_leaflet_map"),
                    )
                } else {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 20.dp),
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
                                    onClick = onEditClick,
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

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
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

                    if (hasValidLocation) {
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
