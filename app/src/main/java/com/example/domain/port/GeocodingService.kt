package com.example.domain.port

import com.example.domain.model.GeoPoint
import com.example.domain.model.GeocodingResult

interface GeocodingService {
    suspend fun search(query: String): Result<List<GeocodingResult>>
    suspend fun reverseGeocode(point: GeoPoint): Result<String?>
}
