package com.example.ui

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.CalendarEventEntity
import com.example.data.NominatimGeocoder
import com.example.reminder.ReminderScheduler
import java.util.Locale

@Composable
fun EventsMapScreen(
    eventsWithLocation: List<CalendarEventEntity>,
    focusedEventId: Long?,
    onFocusedEventConsumed: () -> Unit,
    onOpenEventDetails: (CalendarEventEntity) -> Unit,
    onAddEventAtLocation: (EventLocation) -> Unit,
    onShowMessage: (String) -> Unit,
    isPaused: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val mapController = remember { LeafletMapController() }
    var selectedEventId by remember { mutableStateOf<Long?>(null) }
    var draftPoint by remember { mutableStateOf<EventLocation?>(null) }
    var isResolvingDraftAddress by remember { mutableStateOf(false) }
    var centeredOnUser by remember { mutableStateOf(false) }

    LaunchedEffect(draftPoint?.lat, draftPoint?.lng, draftPoint?.label) {
        val current = draftPoint
        if (current != null && current.label == null) {
            isResolvingDraftAddress = true
            val resolved = NominatimGeocoder.reverseGeocode(current.lat, current.lng).getOrNull()
            isResolvingDraftAddress = false
            if (resolved != null && draftPoint?.lat == current.lat && draftPoint?.lng == current.lng) {
                draftPoint = current.copy(label = resolved)
            }
        } else {
            isResolvingDraftAddress = false
        }
    }

    val selectedEvent = remember(eventsWithLocation, selectedEventId) {
        eventsWithLocation.firstOrNull { it.id == selectedEventId }
    }

    val markers = remember(eventsWithLocation) {
        eventsWithLocation.mapNotNull { event ->
            val lat = event.locationLat ?: return@mapNotNull null
            val lng = event.locationLng ?: return@mapNotNull null
            val subtitle = buildString {
                append(DateUtils.formatDateKeyDisplay(event.dateKey))
                if (event.reminderTimeMinutes != null) {
                    append(" • ")
                    append(ReminderScheduler.formatMinutesOfDay(event.reminderTimeMinutes))
                }
            }
            LeafletMapMarker(
                id = event.id,
                title = event.title,
                subtitle = subtitle,
                lat = lat,
                lng = lng,
                hasReminder = event.reminderTimeMinutes != null,
            )
        }
    }

    LaunchedEffect(focusedEventId, markers) {
        if (focusedEventId != null && markers.any { it.id == focusedEventId }) {
            selectedEventId = focusedEventId
            draftPoint = null
            centeredOnUser = false
            mapController.clearDraftPin()
            mapController.focusEvent(focusedEventId)
            onFocusedEventConsumed()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("events_map_screen"),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OsmSearchBar(
            onResultSelected = { result ->
                selectedEventId = null
                centeredOnUser = false
                draftPoint = EventLocation(
                    lat = result.lat,
                    lng = result.lng,
                    label = result.shortName,
                )
                mapController.setDraftPin(
                    lat = result.lat,
                    lng = result.lng,
                    panTo = true,
                    zoom = 14,
                )
            },
            testTagPrefix = "map_tab_osm_search",
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(bottom = 8.dp),
        ) {
            LeafletMapView(
                controller = mapController,
                markers = markers,
                fitBoundsOnMarkersChange = selectedEventId == null &&
                    focusedEventId == null &&
                    draftPoint == null &&
                    !centeredOnUser,
                enableTapSelection = true,
                isPaused = isPaused,
                onMapTap = { lat, lng ->
                    selectedEventId = null
                    draftPoint = EventLocation(lat = lat, lng = lng, label = null)
                },
                onMarkerTap = { eventId ->
                    draftPoint = null
                    mapController.clearDraftPin()
                    selectedEventId = eventId
                },
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(20.dp))
                    .testTag("events_overview_leaflet_map"),
            )

            MyLocationMapButton(
                onLocationFound = { lat, lng ->
                    centeredOnUser = true
                    mapController.setUserLocation(lat = lat, lng = lng, zoom = 15)
                },
                onFeedbackMessage = onShowMessage,
                testTag = "map_my_location_button",
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp),
            )

            if (eventsWithLocation.isEmpty() && draftPoint == null) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                    tonalElevation = 4.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 12.dp, bottom = 76.dp)
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

            androidx.compose.animation.AnimatedVisibility(
                visible = selectedEvent != null,
                enter = fadeIn() + slideInVertically { it / 2 },
                exit = fadeOut() + slideOutVertically { it / 2 },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, bottom = 76.dp),
            ) {
                selectedEvent?.let { event ->
                    val locSummary = ReminderScheduler.formatLocationSummary(
                        event.locationLat,
                        event.locationLng,
                        event.locationLabel,
                    )
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 6.dp,
                        shadowElevation = 6.dp,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
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
                                    Text(
                                        text = event.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = DateUtils.formatDateKeyDisplay(event.dateKey),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.secondary,
                                    )
                                }
                                IconButton(
                                    onClick = { selectedEventId = null },
                                    modifier = Modifier.minimumInteractiveComponentSize(),
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = stringResource(R.string.cd_close_selection),
                                    )
                                }
                            }

                            if (event.reminderTimeMinutes != null) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Notifications,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Text(
                                        text = stringResource(
                                            R.string.reminder_badge,
                                            ReminderScheduler.formatMinutesOfDay(event.reminderTimeMinutes),
                                        ),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }

                            if (locSummary != null) {
                                Text(
                                    text = locSummary,
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
                                    onClick = { onOpenEventDetails(event) },
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
            }

            androidx.compose.animation.AnimatedVisibility(
                visible = draftPoint != null && selectedEvent == null,
                enter = fadeIn() + slideInVertically { it / 2 },
                exit = fadeOut() + slideOutVertically { it / 2 },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, bottom = 76.dp),
            ) {
                draftPoint?.let { point ->
                    val pointText = point.label ?: String.format(
                        Locale.US,
                        "%.5f, %.5f",
                        point.lat,
                        point.lng,
                    )
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 6.dp,
                        shadowElevation = 6.dp,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
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
                                if (isResolvingDraftAddress) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                                Text(
                                    text = if (isResolvingDraftAddress) {
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
                                    onClick = {
                                        draftPoint = null
                                        mapController.clearDraftPin()
                                    },
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
                                    onClick = {
                                        onAddEventAtLocation(point)
                                        draftPoint = null
                                        mapController.clearDraftPin()
                                    },
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
            }
        }
    }
}
