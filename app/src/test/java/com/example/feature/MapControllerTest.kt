package com.example.feature

import com.example.domain.model.DeviceLocationResult
import com.example.domain.model.GeoPoint
import com.example.domain.model.GeocodingResult
import com.example.fakes.FakeDeviceLocationProvider
import com.example.fakes.FakeEventRepository
import com.example.fakes.FakeGeocodingService
import com.example.feature.map.MapController
import com.example.feature.map.MapFeedbackEvent
import com.example.feature.map.PlaceSearchStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MapControllerTest {

    @Test
    fun placeSearchTransitionsThroughResultsEmptyAndErrorStates() = runTest {
        val controllerScope = CoroutineScope(coroutineContext + SupervisorJob())
        try {
            val wawPoint = requireNotNull(GeoPoint.createOrNull(52.2297, 21.0122))
            val geocoder = FakeGeocodingService(
                searchResult = Result.success(
                    listOf(
                        GeocodingResult(
                            displayName = "Warszawa, mazowieckie, Polska",
                            shortName = "Warszawa",
                            point = wawPoint,
                        )
                    )
                )
            )
            val controller = MapController(
                eventRepository = FakeEventRepository(),
                geocodingService = geocoder,
                locationProvider = FakeDeviceLocationProvider(),
                scope = controllerScope,
            )
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                controller.uiState.collect()
            }
            advanceUntilIdle()

            controller.onSearchQueryChanged("Warszawa")
            controller.submitSearch()
            advanceUntilIdle()

            val resultsStatus = controller.uiState.value.searchState.status
            assertTrue(resultsStatus is PlaceSearchStatus.Results)
            assertEquals(1, (resultsStatus as PlaceSearchStatus.Results).items.size)

            geocoder.searchResult = Result.success(emptyList())
            controller.submitSearch()
            advanceUntilIdle()
            assertEquals(PlaceSearchStatus.Empty, controller.uiState.value.searchState.status)

            geocoder.searchResult = Result.failure(IllegalStateException("Offline"))
            controller.submitSearch()
            advanceUntilIdle()
            assertEquals(PlaceSearchStatus.Error, controller.uiState.value.searchState.status)
        } finally {
            controllerScope.cancel()
        }
    }

    @Test
    fun mapTapResolvesReverseGeocodedAddressAndLocationRequestEmitsFocusEvent() = runTest {
        val controllerScope = CoroutineScope(coroutineContext + SupervisorJob())
        try {
            val krakow = requireNotNull(GeoPoint.createOrNull(50.06194, 19.93685))
            val geocoder = FakeGeocodingService(reverseResult = Result.success("Kraków, Rynek Główny"))
            val locationProvider = FakeDeviceLocationProvider(
                locationResult = DeviceLocationResult.Success(krakow),
            )
            val controller = MapController(
                eventRepository = FakeEventRepository(),
                geocodingService = geocoder,
                locationProvider = locationProvider,
                scope = controllerScope,
            )
            val emittedEvents = mutableListOf<MapFeedbackEvent>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                controller.uiState.collect()
            }
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                controller.events.collect { emittedEvents.add(it) }
            }
            advanceUntilIdle()

            controller.onMapTapped(krakow)
            advanceUntilIdle()

            assertFalse(controller.uiState.value.isResolvingDraftAddress)
            assertEquals("Kraków, Rynek Główny", controller.uiState.value.draftPoint?.sanitizedLabel)

            controller.requestCurrentLocation()
            advanceUntilIdle()

            assertTrue(controller.uiState.value.centeredOnUser)
            assertTrue(emittedEvents.any { it == MapFeedbackEvent.FocusUserLocation(krakow) })
        } finally {
            controllerScope.cancel()
        }
    }
}
