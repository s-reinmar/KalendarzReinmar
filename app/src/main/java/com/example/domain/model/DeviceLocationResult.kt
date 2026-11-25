package com.example.domain.model

sealed interface DeviceLocationResult {
    data class Success(val point: GeoPoint) : DeviceLocationResult
    data object PermissionDenied : DeviceLocationResult
    data object LocationDisabled : DeviceLocationResult
    data object Unavailable : DeviceLocationResult
}
