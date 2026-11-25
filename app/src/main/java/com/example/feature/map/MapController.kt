package com.example.feature.map

import com.example.domain.model.DeviceLocationResult
import com.example.domain.model.EventId
import com.example.domain.model.EventLocation
import com.example.domain.model.GeoPoint
import com.example.domain.model.GeocodingResult
import com.example.domain.port.DeviceLocationProvider
import com.example.domain.port.EventRepository
import com.example.domain.port.GeocodingService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MapController(
    eventRepository: EventRepository,
    private val geocodingService: GeocodingService,
    private val locationProvider: DeviceLocationProvider,
    private val scope: CoroutineScope,
) {
    private data class InternalMapState(
        val selectedEventId: EventId? = null,
        val draftPoint: EventLocation? = null,
        val isResolvingDraftAddress: Boolean = false,
        val centeredOnUser: Boolean = false,
        val isLocatingUser: Boolean = false,
        val searchState: PlaceSearchUiState = PlaceSearchUiState(),
    )

    private val internalState = MutableStateFlow(InternalMapState())
    private val _events = MutableSharedFlow<MapFeedbackEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<MapFeedbackEvent> = _events.asSharedFlow()

    private var searchJob: Job? = null
    private var reverseGeocodeJob: Job? = null

    val uiState: StateFlow<MapUiState> = combine(
        eventRepository.observeEvents().map { list -> list.filter { it.hasLocation } },
        internalState,
    ) { locatedEvents, state ->
        MapUiState(
            eventsWithLocation = locatedEvents,
            selectedEventId = state.selectedEventId,
            draftPoint = state.draftPoint,
            isResolvingDraftAddress = state.isResolvingDraftAddress,
            centeredOnUser = state.centeredOnUser,
            isLocatingUser = state.isLocatingUser,
            searchState = state.searchState,
        )
    }.stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = MapUiState(),
    )

    fun hasLocationPermission(): Boolean = locationProvider.hasLocationPermission()

    fun onSearchQueryChanged(query: String) {
        internalState.update { state ->
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
        internalState.update { it.copy(searchState = PlaceSearchUiState()) }
    }

    fun submitSearch() {
        val trimmed = internalState.value.searchState.query.trim()
        if (trimmed.length < 2) return

        searchJob?.cancel()
        internalState.update {
            it.copy(searchState = it.searchState.copy(status = PlaceSearchStatus.Loading))
        }
        searchJob = scope.launch {
            val outcome = geocodingService.search(trimmed)
            internalState.update { state ->
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
        val pickedLocation = EventLocation(point = result.point, label = result.shortName)
        internalState.update { state ->
            state.copy(
                selectedEventId = null,
                centeredOnUser = false,
                draftPoint = pickedLocation,
                isResolvingDraftAddress = false,
                searchState = PlaceSearchUiState(
                    query = result.shortName,
                    status = PlaceSearchStatus.Idle,
                ),
            )
        }
        _events.tryEmit(MapFeedbackEvent.FocusDraftLocation(result.point, zoom = 14))
    }

    fun onMapTapped(point: GeoPoint) {
        reverseGeocodeJob?.cancel()
        val initialDraft = EventLocation(point = point, label = null)
        internalState.update { state ->
            state.copy(
                selectedEventId = null,
                draftPoint = initialDraft,
                isResolvingDraftAddress = true,
            )
        }
        reverseGeocodeJob = scope.launch {
            val resolved = geocodingService.reverseGeocode(point).getOrNull()
            internalState.update { state ->
                if (state.draftPoint?.point == point) {
                    state.copy(
                        draftPoint = EventLocation(point = point, label = resolved),
                        isResolvingDraftAddress = false,
                    )
                } else {
                    state.copy(isResolvingDraftAddress = false)
                }
            }
        }
    }

    fun onMarkerTapped(eventId: EventId) {
        if (!eventId.isValid) return
        reverseGeocodeJob?.cancel()
        internalState.update { state ->
            state.copy(
                draftPoint = null,
                isResolvingDraftAddress = false,
                selectedEventId = eventId,
            )
        }
        _events.tryEmit(MapFeedbackEvent.ClearDraftPin)
    }

    fun focusExternalEvent(eventId: EventId) {
        if (!eventId.isValid) return
        reverseGeocodeJob?.cancel()
        internalState.update { state ->
            state.copy(
                selectedEventId = eventId,
                draftPoint = null,
                isResolvingDraftAddress = false,
                centeredOnUser = false,
            )
        }
        _events.tryEmit(MapFeedbackEvent.ClearDraftPin)
        _events.tryEmit(MapFeedbackEvent.FocusEventMarker(eventId))
    }

    fun clearSelectedEvent() {
        internalState.update { it.copy(selectedEventId = null) }
    }

    fun clearDraftPoint() {
        reverseGeocodeJob?.cancel()
        internalState.update {
            it.copy(draftPoint = null, isResolvingDraftAddress = false)
        }
        _events.tryEmit(MapFeedbackEvent.ClearDraftPin)
    }

    fun requestCurrentLocation() {
        if (internalState.value.isLocatingUser) return
        internalState.update { it.copy(isLocatingUser = true) }
        scope.launch {
            val result = locationProvider.fetchCurrentLocation()
            internalState.update { it.copy(isLocatingUser = false) }
            when (result) {
                is DeviceLocationResult.Success -> {
                    internalState.update { it.copy(centeredOnUser = true) }
                    _events.tryEmit(MapFeedbackEvent.FocusUserLocation(result.point))
                }
                else -> {
                    _events.tryEmit(MapFeedbackEvent.LocationError(result))
                }
            }
        }
    }

    fun onLocationPermissionDenied() {
        _events.tryEmit(MapFeedbackEvent.LocationError(DeviceLocationResult.PermissionDenied))
    }
}
