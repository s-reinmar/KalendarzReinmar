package com.example.domain.model

import java.util.Locale

data class GeoPoint private constructor(
    val lat: Double,
    val lng: Double,
) {
    fun formatCoordinates(): String = String.format(Locale.US, "%.5f, %.5f", lat, lng)

    companion object {
        val DEFAULT_POLAND_CENTER = GeoPoint(lat = 52.2297, lng = 21.0122)

        fun createOrNull(lat: Double?, lng: Double?): GeoPoint? {
            if (lat == null || lng == null) return null
            if (!lat.isFinite() || !lng.isFinite()) return null
            if (lat !in -90.0..90.0 || lng !in -180.0..180.0) return null
            return GeoPoint(lat = lat, lng = lng)
        }
    }
}
