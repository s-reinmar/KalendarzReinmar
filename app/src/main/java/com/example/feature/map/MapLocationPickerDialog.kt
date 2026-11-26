package com.example.feature.map

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.domain.model.DeviceLocationResult
import com.example.domain.model.EventLocation
import com.example.domain.model.GeoPoint
import com.example.domain.port.DeviceLocationProvider
import com.example.domain.port.GeocodingService
import com.example.feature.map.leaflet.LeafletMapController
import com.example.feature.map.leaflet.LeafletMapView

private const val PICKER_DEFAULT_ZOOM = 6
private const val PICKER_PIN_ZOOM = 14

@Composable
fun MapLocationPickerDialog(
    initialLocation: EventLocation?,
    geocodingService: GeocodingService,
    locationProvider: DeviceLocationProvider,
    onDismiss: () -> Unit,
    onConfirm: (EventLocation?) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val controller = remember(initialLocation, geocodingService, locationProvider) {
        LocationPickerController(
            initialLocation = initialLocation,
            geocodingService = geocodingService,
            locationProvider = locationProvider,
            scope = scope,
        )
    }
    val uiState by controller.uiState.collectAsStateWithLifecycle()
    val leafletController = remember { LeafletMapController() }

    LaunchedEffect(initialLocation) {
        initialLocation?.let { loc ->
            leafletController.setDraftPin(loc.point, panTo = true, zoom = PICKER_PIN_ZOOM)
        }
    }

    LaunchedEffect(controller) {
        controller.commands.collect { command ->
            when (command) {
                is LocationPickerMapCommand.MoveDraftPin -> {
                    leafletController.setDraftPin(command.point, panTo = true, zoom = command.zoom)
                }
                is LocationPickerMapCommand.MoveUserLocation -> {
                    leafletController.setUserLocation(command.point, zoom = command.zoom)
                }
            }
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
                .testTag("map_location_picker_dialog"),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = stringResource(R.string.location_picker_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                OsmSearchBar(
                    searchState = uiState.searchState,
                    onQueryChanged = controller::onSearchQueryChanged,
                    onSearchSubmit = controller::submitSearch,
                    onClear = controller::clearSearch,
                    onResultSelected = controller::selectSearchResult,
                    testTagPrefix = "picker_osm_search",
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    val pickedPoint = uiState.pickedPoint
                    val statusText = when {
                        uiState.isResolvingAddress -> stringResource(R.string.map_resolving_address)
                        !uiState.resolvedAddress.isNullOrBlank() -> stringResource(
                            R.string.map_selected_point,
                            uiState.resolvedAddress.orEmpty(),
                        )
                        pickedPoint != null -> stringResource(
                            R.string.location_coordinates_label,
                            pickedPoint.formatCoordinates(),
                        )
                        else -> stringResource(R.string.location_picker_hint)
                    }
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("map_coordinates_text"),
                    )
                    TextButton(
                        onClick = { leafletController.dropPinAtCenter() },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("location_drop_pin_button"),
                    ) {
                        Text(stringResource(R.string.location_drop_pin))
                    }
                }

                uiState.locationError?.let { error ->
                    val errorRes = when (error) {
                        DeviceLocationResult.PermissionDenied -> R.string.map_my_location_permission_denied
                        DeviceLocationResult.LocationDisabled -> R.string.map_my_location_disabled
                        DeviceLocationResult.Unavailable -> R.string.map_my_location_unavailable
                        is DeviceLocationResult.Success -> null
                    }
                    if (errorRes != null) {
                        Text(
                            text = stringResource(errorRes),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .clip(RoundedCornerShape(16.dp)),
                ) {
                    LeafletMapView(
                        controller = leafletController,
                        initialCenter = initialLocation?.point ?: GeoPoint.DEFAULT_POLAND_CENTER,
                        initialZoom = if (initialLocation != null) PICKER_PIN_ZOOM else PICKER_DEFAULT_ZOOM,
                        enableTapSelection = true,
                        onMapTap = controller::onMapTapped,
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("leaflet_map_webview"),
                    )

                    MyLocationMapButton(
                        isLocating = uiState.isLocatingUser,
                        hasPermission = controller::hasLocationPermission,
                        onRequestLocation = controller::requestCurrentLocation,
                        onPermissionDenied = controller::onLocationPermissionDenied,
                        testTag = "picker_my_location_button",
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(10.dp),
                    )
                }

                OutlinedTextField(
                    value = uiState.label,
                    onValueChange = controller::onLabelChanged,
                    label = { Text(stringResource(R.string.location_name_label)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("location_label_input"),
                )

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    if (initialLocation != null || uiState.pickedPoint != null) {
                        TextButton(
                            onClick = { onConfirm(null) },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("location_remove_button"),
                        ) {
                            Text(
                                text = stringResource(R.string.location_remove),
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.height(1.dp))
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("location_cancel_button"),
                        ) {
                            Text(stringResource(R.string.action_cancel))
                        }
                        TextButton(
                            onClick = {
                                uiState.buildResult()?.let(onConfirm)
                            },
                            enabled = uiState.canConfirm,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("location_save_button"),
                        ) {
                            Text(stringResource(R.string.action_save))
                        }
                    }
                }
            }
        }
    }
}
