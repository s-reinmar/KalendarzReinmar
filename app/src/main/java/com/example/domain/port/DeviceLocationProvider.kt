package com.example.domain.port

import com.example.domain.model.DeviceLocationResult

interface DeviceLocationProvider {
    fun hasLocationPermission(): Boolean
    suspend fun fetchCurrentLocation(): DeviceLocationResult
}
