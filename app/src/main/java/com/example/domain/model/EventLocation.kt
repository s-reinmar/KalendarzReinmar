package com.example.domain.model

data class EventLocation(
    val point: GeoPoint,
    val label: String? = null,
) {
    val sanitizedLabel: String?
        get() = label?.trim()?.takeIf { it.isNotEmpty() }

    fun formatSummary(): String {
        val coords = point.formatCoordinates()
        val cleanLabel = sanitizedLabel
        return if (cleanLabel != null) "$cleanLabel ($coords)" else coords
    }

    companion object {
        fun fromRawOrNull(lat: Double?, lng: Double?, label: String?): EventLocation? {
            val geoPoint = GeoPoint.createOrNull(lat, lng) ?: return null
            return EventLocation(
                point = geoPoint,
                label = label?.trim()?.takeIf { it.isNotEmpty() },
            )
        }
    }
}
