package com.example.feature.map

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.domain.model.CalendarEvent
import com.example.domain.model.DeviceLocationResult
import com.example.domain.model.EventId
import com.example.domain.model.EventLocation
import com.example.domain.service.PolishDateFormatter
import com.example.feature.map.leaflet.LeafletMapController
import com.example.feature.map.leaflet.LeafletMapMarker
import com.example.feature.map.leaflet.LeafletMapView

@Composable
fun EventsMapScreen(
    controller: MapController,
    focusedEventId: EventId?,
    onFocusedEventConsumed: () -> Unit,
    onOpenEventDetails: (CalendarEvent) -> Unit,
    onAddEventAtLocation: (EventLocation) -> Unit,
    onShowMessage: (String) -> Unit,
    isPaused: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val uiState by controller.uiState.collectAsStateWithLifecycle()
    val leafletController = remember { LeafletMapController() }

    val markers = remember(uiState.eventsWithLocation) {
        uiState.eventsWithLocation.mapNotNull { event ->
            val location = event.location ?: return@mapNotNull null
            val subtitle = buildString {
                append(PolishDateFormatter.formatDateKeyDisplay(event.dateKey))
                event.reminderTime?.let { reminder ->
                    append(" • ")
                    append(reminder.formatHHmm())
                }
            }
            LeafletMapMarker(
                id = event.id,
                title = event.title,
                subtitle = subtitle,
                point = location.point,
                hasReminder = event.hasReminder,
            )
        }
    }

    LaunchedEffect(controller) {
        controller.events.collect { event ->
            when (event) {
                is MapFeedbackEvent.FocusUserLocation -> {
                    leafletController.setUserLocation(event.point, zoom = 15)
                }
                is MapFeedbackEvent.FocusDraftLocation -> {
                    leafletController.setDraftPin(event.point, panTo = true, zoom = event.zoom)
                }
                MapFeedbackEvent.ClearDraftPin -> {
                    leafletController.clearDraftPin()
                }
                is MapFeedbackEvent.FocusEventMarker -> {
                    leafletController.focusEvent(event.eventId)
                }
                is MapFeedbackEvent.LocationError -> {
                    val resId = when (event.result) {
                        DeviceLocationResult.PermissionDenied -> R.string.map_my_location_permission_denied
                        DeviceLocationResult.LocationDisabled -> R.string.map_my_location_disabled
                        DeviceLocationResult.Unavailable -> R.string.map_my_location_unavailable
                        is DeviceLocationResult.Success -> return@collect
                    }
                    onShowMessage(context.getString(resId))
                }
            }
        }
    }

    LaunchedEffect(focusedEventId, markers) {
        if (focusedEventId != null && markers.any { it.id == focusedEventId }) {
            controller.focusExternalEvent(focusedEventId)
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
            searchState = uiState.searchState,
            onQueryChanged = controller::onSearchQueryChanged,
            onSearchSubmit = controller::submitSearch,
            onClear = controller::clearSearch,
            onResultSelected = controller::selectSearchResult,
            testTagPrefix = "map_tab_osm_search",
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(bottom = 8.dp),
        ) {
            if (isPaused) {
                androidx.compose.material3.Surface(
                    color = androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {}
            } else {
                LeafletMapView(
                    controller = leafletController,
                    markers = markers,
                    fitBoundsOnMarkersChange = uiState.shouldFitBoundsOnMarkersChange && focusedEventId == null,
                    enableTapSelection = true,
                    isPaused = false,
                    onMapTap = controller::onMapTapped,
                    onMarkerTap = controller::onMarkerTapped,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(20.dp))
                        .testTag("events_overview_leaflet_map"),
                )
            }

            MyLocationMapButton(
                isLocating = uiState.isLocatingUser,
                hasPermission = controller::hasLocationPermission,
                onRequestLocation = controller::requestCurrentLocation,
                onPermissionDenied = controller::onLocationPermissionDenied,
                testTag = "map_my_location_button",
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp),
            )

            if (uiState.eventsWithLocation.isEmpty() && uiState.draftPoint == null) {
                EmptyMapBanner(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(start = 12.dp, end = 12.dp, bottom = 76.dp),
                )
            }

            androidx.compose.animation.AnimatedVisibility(
                visible = uiState.selectedEvent != null,
                enter = fadeIn() + slideInVertically { it / 2 },
                exit = fadeOut() + slideOutVertically { it / 2 },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, bottom = 76.dp),
            ) {
                uiState.selectedEvent?.let { event ->
                    SelectedEventMapCard(
                        event = event,
                        onClose = controller::clearSelectedEvent,
                        onOpenDetails = { onOpenEventDetails(event) },
                    )
                }
            }

            androidx.compose.animation.AnimatedVisibility(
                visible = uiState.draftPoint != null && uiState.selectedEvent == null,
                enter = fadeIn() + slideInVertically { it / 2 },
                exit = fadeOut() + slideOutVertically { it / 2 },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, bottom = 76.dp),
            ) {
                uiState.draftPoint?.let { point ->
                    DraftPointMapCard(
                        point = point,
                        isResolvingAddress = uiState.isResolvingDraftAddress,
                        onClose = controller::clearDraftPoint,
                        onAddEventHere = {
                            onAddEventAtLocation(point)
                            controller.clearDraftPoint()
                        },
                    )
                }
            }
        }
    }
}
