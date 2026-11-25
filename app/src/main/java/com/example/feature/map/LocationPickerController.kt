package com.example.feature.map

import com.example.domain.model.DeviceLocationResult
import com.example.domain.model.EventLocation
import com.example.domain.model.GeoPoint
import com.example.domain.model.GeocodingResult
import com.example.domain.port.DeviceLocationProvider
import com.example.domain.port.GeocodingService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LocationPickerUiState(
    val pickedPoint: GeoPoint? = null,
    val label: String = "",
    val userEditedLabel: Boolean = false,
    val resolvedAddress: String? = null,
    val isResolvingAddress: Boolean = false,
    val isLocatingUser: Boolean = false,
    val locationError: DeviceLocationResult? = null,
    val searchState: PlaceSearchUiState = PlaceSearchUiState(),
) {
    val canConfirm: Boolean
        get() = pickedPoint != null

    fun buildResult(): EventLocation? = pickedPoint?.let { point ->
        EventLocation(
            point = point,
            label = label.trim().takeIf { it.isNotEmpty() },
        )
    }
}

sealed interface LocationPickerMapCommand {
    data class MoveDraftPin(val point: GeoPoint, val zoom: Int = 14) : LocationPickerMapCommand
    data class MoveUserLocation(val point: GeoPoint, val zoom: Int = 15) : LocationPickerMapCommand
}

class LocationPickerController(
    initialLocation: EventLocation?,
    private val geocodingService: GeocodingService,
    private val locationProvider: DeviceLocationProvider,
    private val scope: CoroutineScope,
) {
    private val _uiState = MutableStateFlow(
        LocationPickerUiState(
            pickedPoint = initialLocation?.point,
            label = initialLocation?.sanitizedLabel.orEmpty(),
            userEditedLabel = initialLocation?.sanitizedLabel != null,
            resolvedAddress = initialLocation?.sanitizedLabel,
        )
    )
    val uiState: StateFlow<LocationPickerUiState> = _uiState.asStateFlow()

    private val _commands = MutableSharedFlow<LocationPickerMapCommand>(extraBufferCapacity = 4)
    val commands: SharedFlow<LocationPickerMapCommand> = _commands.asSharedFlow()

    private var searchJob: Job? = null
    private var reverseGeocodeJob: Job? = null

    fun hasLocationPermission(): Boolean = locationProvider.hasLocationPermission()

    fun onLabelChanged(value: String) {
        _uiState.update {
            it.copy(
                label = value,
                userEditedLabel = value.isNotBlank(),
            )
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { state ->
            val nextStatus = if (query.isBlank()) {
                PlaceSearchStatus.Idle
            } else {
                state.searchState.status
            }
            state.copy(searchState = PlaceSearchUiState(query = query, status = nextStatus))
        }
    }

    fun clearSearch() {
        searchJob?.cancel()
        _uiState.update { it.copy(searchState = PlaceSearchUiState()) }
    }

    fun submitSearch() {
        val trimmed = _uiState.value.searchState.query.trim()
        if (trimmed.length < 2) return

        searchJob?.cancel()
        _uiState.update {
            it.copy(searchState = it.searchState.copy(status = PlaceSearchStatus.Loading))
        }
        searchJob = scope.launch {
            val outcome = geocodingService.search(trimmed)
            _uiState.update { state ->
                val nextStatus = outcome.fold(
                    onSuccess = { list ->
                        if (list.isEmpty()) PlaceSearchStatus.Empty else PlaceSearchStatus.Results(list)
                    },
                    onFailure = { PlaceSearchStatus.Error },
                )
                state.copy(searchState = state.searchState.copy(status = nextStatus))
            }
        }
    }

    fun selectSearchResult(result: GeocodingResult) {
        reverseGeocodeJob?.cancel()
        _uiState.update { state ->
            val nextLabel = if (!state.userEditedLabel || state.label.isBlank()) {
                result.shortName
            } else {
                state.label
            }
            state.copy(
                pickedPoint = result.point,
                resolvedAddress = result.shortName,
                label = nextLabel,
                isResolvingAddress = false,
                locationError = null,
                searchState = PlaceSearchUiState(
                    query = result.shortName,
                    status = PlaceSearchStatus.Idle,
                ),
            )
        }
        _commands.tryEmit(LocationPickerMapCommand.MoveDraftPin(result.point, zoom = 14))
    }

    fun onMapTapped(point: GeoPoint) {
        reverseGeocodeJob?.cancel()
        _uiState.update { state ->
            state.copy(
                pickedPoint = point,
                resolvedAddress = null,
                isResolvingAddress = true,
                locationError = null,
            )
        }
        reverseGeocodeJob = scope.launch {
            val address = geocodingService.reverseGeocode(point).getOrNull()
            _uiState.update { state ->
                if (state.pickedPoint == point) {
                    val nextLabel = if (!state.userEditedLabel && address != null) {
                        address
                    } else {
                        state.label
                    }
                    state.copy(
                        resolvedAddress = address,
                        label = nextLabel,
                        isResolvingAddress = false,
                    )
                } else {
                    state.copy(isResolvingAddress = false)
                }
            }
        }
    }

    fun requestCurrentLocation() {
        if (_uiState.value.isLocatingUser) return
        _uiState.update { it.copy(isLocatingUser = true, locationError = null) }
        scope.launch {
            val result = locationProvider.fetchCurrentLocation()
            _uiState.update { it.copy(isLocatingUser = false) }
            when (result) {
                is DeviceLocationResult.Success -> {
                    _commands.tryEmit(LocationPickerMapCommand.MoveUserLocation(result.point, zoom = 15))
                }
                else -> {
                    _uiState.update { it.copy(locationError = result) }
                }
            }
        }
    }

    fun onLocationPermissionDenied() {
        _uiState.update { it.copy(locationError = DeviceLocationResult.PermissionDenied) }
    }
}
