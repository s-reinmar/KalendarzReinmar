package com.example.feature.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.domain.port.DeviceLocationProvider
import com.example.domain.port.EventRepository
import com.example.domain.port.GeocodingService

class MapViewModel(
    eventRepository: EventRepository,
    geocodingService: GeocodingService,
    locationProvider: DeviceLocationProvider,
) : ViewModel() {

    val controller = MapController(
        eventRepository = eventRepository,
        geocodingService = geocodingService,
        locationProvider = locationProvider,
        scope = viewModelScope,
    )

    companion object {
        fun factory(
            eventRepository: EventRepository,
            geocodingService: GeocodingService,
            locationProvider: DeviceLocationProvider,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                MapViewModel(
                    eventRepository = eventRepository,
                    geocodingService = geocodingService,
                    locationProvider = locationProvider,
                ) as T
        }
    }
}
