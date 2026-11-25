package com.example.feature.map

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.model.CalendarEvent
import com.example.domain.model.EventLocation
import com.example.domain.service.PolishDateFormatter
import com.example.ui.components.MetadataBadge

@Composable
internal fun EmptyMapBanner(modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        tonalElevation = 4.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
            .fillMaxWidth()
            .testTag("map_empty_banner"),
    ) {
        Text(
            text = stringResource(R.string.map_empty_banner),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(14.dp),
        )
    }
}

@Composable
internal fun SelectedEventMapCard(
    event: CalendarEvent,
    onClose: () -> Unit,
    onOpenDetails: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val locationSummary = event.location?.formatSummary()
    val palette = com.example.feature.calendar.CalendarEntryVisuals.palette(event.colorTag)
    val scheduleSummary = buildString {
        append(PolishDateFormatter.formatDateKeyDisplay(event.dateKey))
        if (!event.isAllDay && event.timeRange != null) {
            append(" • ")
            append(event.timeRange.formatDisplay())
        }
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        shadowElevation = 6.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
            .fillMaxWidth()
            .testTag("map_selected_event_card"),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = palette.container,
                    ) {
                        Text(
                            text = stringResource(
                                com.example.feature.calendar.CalendarEntryVisuals.labelRes(event.entryType)
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.onContainer,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = event.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = scheduleSummary,
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.accent,
                    )
                }
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.minimumInteractiveComponentSize(),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.cd_close_selection),
                    )
                }
            }

            event.reminderTime?.let { reminder ->
                MetadataBadge(
                    text = stringResource(R.string.reminder_badge, reminder.formatHHmm()),
                    icon = Icons.Filled.Notifications,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }

            if (locationSummary != null) {
                Text(
                    text = locationSummary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                FilledTonalButton(
                    onClick = onOpenDetails,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("map_open_event_details_button"),
                ) {
                    Text(stringResource(R.string.map_open_details))
                }
            }
        }
    }
}

@Composable
internal fun DraftPointMapCard(
    point: EventLocation,
    isResolvingAddress: Boolean,
    onClose: () -> Unit,
    onAddEventHere: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pointText = point.sanitizedLabel ?: point.point.formatCoordinates()

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        shadowElevation = 6.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
            .fillMaxWidth()
            .testTag("map_draft_point_card"),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (isResolvingAddress) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Text(
                    text = if (isResolvingAddress) {
                        stringResource(R.string.map_resolving_address)
                    } else {
                        stringResource(R.string.map_selected_point, pointText)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.minimumInteractiveComponentSize(),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.cd_close_selection),
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                FilledTonalButton(
                    onClick = onAddEventHere,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("map_add_event_here_button"),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.map_add_event_here))
                }
            }
        }
    }
}
