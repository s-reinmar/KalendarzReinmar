package com.example.domain.model

data class GeocodingResult(
    val displayName: String,
    val shortName: String,
    val point: GeoPoint,
)
