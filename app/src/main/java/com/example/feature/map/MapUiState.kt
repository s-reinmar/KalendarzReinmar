package com.example.feature.map

import com.example.domain.model.CalendarEvent
import com.example.domain.model.DeviceLocationResult
import com.example.domain.model.EventId
import com.example.domain.model.EventLocation
import com.example.domain.model.GeoPoint
import com.example.domain.model.GeocodingResult

sealed interface PlaceSearchStatus {
    data object Idle : PlaceSearchStatus
    data object Loading : PlaceSearchStatus
    data class Results(val items: List<GeocodingResult>) : PlaceSearchStatus
    data object Empty : PlaceSearchStatus
    data object Error : PlaceSearchStatus
}

data class PlaceSearchUiState(
    val query: String = "",
    val status: PlaceSearchStatus = PlaceSearchStatus.Idle,
) {
    val isLoading: Boolean
        get() = status is PlaceSearchStatus.Loading

    val results: List<GeocodingResult>
        get() = (status as? PlaceSearchStatus.Results)?.items.orEmpty()
}

sealed interface MapFeedbackEvent {
    data class FocusUserLocation(val point: GeoPoint) : MapFeedbackEvent
    data class FocusDraftLocation(val point: GeoPoint, val zoom: Int = 14) : MapFeedbackEvent
    data object ClearDraftPin : MapFeedbackEvent
    data class FocusEventMarker(val eventId: EventId) : MapFeedbackEvent
    data class LocationError(val result: DeviceLocationResult) : MapFeedbackEvent
}

data class MapUiState(
    val eventsWithLocation: List<CalendarEvent> = emptyList(),
    val selectedEventId: EventId? = null,
    val draftPoint: EventLocation? = null,
    val isResolvingDraftAddress: Boolean = false,
    val centeredOnUser: Boolean = false,
    val isLocatingUser: Boolean = false,
    val searchState: PlaceSearchUiState = PlaceSearchUiState(),
) {
    val selectedEvent: CalendarEvent?
        get() = selectedEventId?.let { id -> eventsWithLocation.firstOrNull { it.id == id } }

    val shouldFitBoundsOnMarkersChange: Boolean
        get() = selectedEventId == null && draftPoint == null && !centeredOnUser
}
