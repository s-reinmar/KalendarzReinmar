package com.example.ui

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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.NominatimGeocoder
import java.util.Locale

data class EventLocation(
    val lat: Double,
    val lng: Double,
    val label: String? = null,
)

private const val PICKER_DEFAULT_LAT = 52.2297
private const val PICKER_DEFAULT_LNG = 21.0122
private const val PICKER_DEFAULT_ZOOM = 6
private const val PICKER_PIN_ZOOM = 14

@Composable
fun MapLocationPickerDialog(
    initialLocation: EventLocation?,
    onDismiss: () -> Unit,
    onConfirm: (EventLocation?) -> Unit,
) {
    var pickedLat by remember(initialLocation) { mutableStateOf(initialLocation?.lat) }
    var pickedLng by remember(initialLocation) { mutableStateOf(initialLocation?.lng) }
    var label by remember(initialLocation) { mutableStateOf(initialLocation?.label.orEmpty()) }
    var userEditedLabel by remember(initialLocation) {
        mutableStateOf(!initialLocation?.label.isNullOrBlank())
    }
    var resolvedAddress by remember(initialLocation) { mutableStateOf(initialLocation?.label) }
    var isResolvingAddress by remember { mutableStateOf(false) }
    var locationFeedback by remember { mutableStateOf<String?>(null) }
    val mapController = remember { LeafletMapController() }

    val hasInitialPin = initialLocation != null &&
        initialLocation.lat.isFinite() && initialLocation.lng.isFinite() &&
        initialLocation.lat in -90.0..90.0 && initialLocation.lng in -180.0..180.0

    LaunchedEffect(initialLocation) {
        if (hasInitialPin && initialLocation != null) {
            mapController.setDraftPin(
                lat = initialLocation.lat,
                lng = initialLocation.lng,
                panTo = true,
                zoom = PICKER_PIN_ZOOM,
            )
        }
    }

    LaunchedEffect(pickedLat, pickedLng) {
        val lat = pickedLat
        val lng = pickedLng
        if (lat != null && lng != null && resolvedAddress == null) {
            isResolvingAddress = true
            val address = NominatimGeocoder.reverseGeocode(lat, lng).getOrNull()
            isResolvingAddress = false
            if (pickedLat == lat && pickedLng == lng && address != null) {
                resolvedAddress = address
                if (!userEditedLabel) {
                    label = address
                }
            }
        } else {
            isResolvingAddress = false
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
                    onResultSelected = { result ->
                        pickedLat = result.lat
                        pickedLng = result.lng
                        resolvedAddress = result.shortName
                        if (!userEditedLabel || label.isBlank()) {
                            label = result.shortName
                        }
                        mapController.setDraftPin(
                            lat = result.lat,
                            lng = result.lng,
                            panTo = true,
                            zoom = PICKER_PIN_ZOOM,
                        )
                    },
                    testTagPrefix = "picker_osm_search",
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = when {
                            isResolvingAddress -> stringResource(R.string.map_resolving_address)
                            !resolvedAddress.isNullOrBlank() -> stringResource(
                                R.string.map_selected_point,
                                resolvedAddress.orEmpty(),
                            )
                            pickedLat != null && pickedLng != null -> stringResource(
                                R.string.location_coordinates_label,
                                String.format(Locale.US, "%.5f, %.5f", pickedLat, pickedLng),
                            )
                            else -> stringResource(R.string.location_picker_hint)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("map_coordinates_text"),
                    )
                    TextButton(
                        onClick = { mapController.dropPinAtCenter() },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("location_drop_pin_button"),
                    ) {
                        Text(stringResource(R.string.location_drop_pin))
                    }
                }

                if (locationFeedback != null) {
                    Text(
                        text = locationFeedback.orEmpty(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .clip(RoundedCornerShape(16.dp)),
                ) {
                    LeafletMapView(
                        controller = mapController,
                        initialCenterLat = if (hasInitialPin) initialLocation!!.lat else PICKER_DEFAULT_LAT,
                        initialCenterLng = if (hasInitialPin) initialLocation!!.lng else PICKER_DEFAULT_LNG,
                        initialZoom = if (hasInitialPin) PICKER_PIN_ZOOM else PICKER_DEFAULT_ZOOM,
                        enableTapSelection = true,
                        onMapTap = { lat, lng ->
                            pickedLat = lat
                            pickedLng = lng
                            resolvedAddress = null
                            locationFeedback = null
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("leaflet_map_webview"),
                    )

                    MyLocationMapButton(
                        onLocationFound = { lat, lng ->
                            locationFeedback = null
                            mapController.setUserLocation(lat = lat, lng = lng, zoom = 15)
                        },
                        onFeedbackMessage = { msg -> locationFeedback = msg },
                        testTag = "picker_my_location_button",
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(10.dp),
                    )
                }

                OutlinedTextField(
                    value = label,
                    onValueChange = {
                        label = it
                        userEditedLabel = it.isNotBlank()
                    },
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
                    if (initialLocation != null || (pickedLat != null && pickedLng != null)) {
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
                                val lat = pickedLat
                                val lng = pickedLng
                                if (lat != null && lng != null) {
                                    onConfirm(
                                        EventLocation(
                                            lat = lat,
                                            lng = lng,
                                            label = label.trim().takeIf { it.isNotEmpty() },
                                        )
                                    )
                                }
                            },
                            enabled = pickedLat != null && pickedLng != null,
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
