package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale

data class GeocodingResult(
    val displayName: String,
    val shortName: String,
    val lat: Double,
    val lng: Double,
)

object NominatimGeocoder {
    private const val SEARCH_BASE_URL = "https://nominatim.openstreetmap.org/search"
    private const val REVERSE_BASE_URL = "https://nominatim.openstreetmap.org/reverse"
    private const val USER_AGENT = "KalendarzReinmar/1.0 (Android; OpenStreetMap Leaflet)"
    private const val CONNECT_TIMEOUT_MS = 8_000
    private const val READ_TIMEOUT_MS = 8_000

    suspend fun search(rawQuery: String): Result<List<GeocodingResult>> = withContext(Dispatchers.IO) {
        val query = rawQuery.trim()
        if (query.length < 2) {
            return@withContext Result.success(emptyList())
        }

        var connection: HttpURLConnection? = null
        try {
            val encodedQuery = URLEncoder.encode(query, Charsets.UTF_8.name())
            val endpoint = URL(
                "$SEARCH_BASE_URL?format=jsonv2&limit=5&addressdetails=1&q=$encodedQuery"
            )
            connection = openNominatimConnection(endpoint)

            val statusCode = connection.responseCode
            if (statusCode !in 200..299) {
                return@withContext Result.failure(IllegalStateException("HTTP $statusCode"))
            }

            val body = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            val jsonArray = JSONArray(body)
            val results = buildList {
                for (index in 0 until jsonArray.length()) {
                    val item = jsonArray.optJSONObject(index) ?: continue
                    val lat = item.optString("lat").toDoubleOrNull() ?: continue
                    val lng = item.optString("lon").toDoubleOrNull() ?: continue
                    if (!lat.isFinite() || !lng.isFinite()) continue
                    if (lat !in -90.0..90.0 || lng !in -180.0..180.0) continue

                    val displayName = item.optString("display_name").trim()
                    if (displayName.isEmpty()) continue
                    val addressObj = item.optJSONObject("address")
                    val shortName = formatAddress(addressObj, displayName)
                        ?: buildShortLabel(displayName)
                    add(
                        GeocodingResult(
                            displayName = displayName,
                            shortName = shortName,
                            lat = lat,
                            lng = lng,
                        )
                    )
                }
            }
            Result.success(results)
        } catch (ex: Exception) {
            Result.failure(ex)
        } finally {
            connection?.disconnect()
        }
    }

    suspend fun reverseGeocode(lat: Double, lng: Double): Result<String?> = withContext(Dispatchers.IO) {
        if (!lat.isFinite() || !lng.isFinite() || lat !in -90.0..90.0 || lng !in -180.0..180.0) {
            return@withContext Result.success(null)
        }

        var connection: HttpURLConnection? = null
        try {
            val latParam = String.format(Locale.US, "%.6f", lat)
            val lngParam = String.format(Locale.US, "%.6f", lng)
            val endpoint = URL(
                "$REVERSE_BASE_URL?format=jsonv2&addressdetails=1&zoom=18&lat=$latParam&lon=$lngParam"
            )
            connection = openNominatimConnection(endpoint)

            val statusCode = connection.responseCode
            if (statusCode !in 200..299) {
                return@withContext Result.failure(IllegalStateException("HTTP $statusCode"))
            }

            val body = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            val jsonObject = JSONObject(body)
            val addressObj = jsonObject.optJSONObject("address")
            val displayName = jsonObject.optString("display_name").trim().takeIf { it.isNotEmpty() }
            Result.success(formatAddress(addressObj, displayName))
        } catch (ex: Exception) {
            Result.failure(ex)
        } finally {
            connection?.disconnect()
        }
    }

    private fun openNominatimConnection(endpoint: URL): HttpURLConnection {
        return (endpoint.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            setRequestProperty("User-Agent", USER_AGENT)
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Accept-Language", "pl,en;q=0.8")
        }
    }

    internal fun formatAddress(address: JSONObject?, fallbackDisplayName: String?): String? {
        if (address != null) {
            val locality = firstNonBlank(
                address,
                "city",
                "town",
                "village",
                "hamlet",
                "municipality",
                "suburb",
                "city_district",
                "county",
            )
            val postcode = firstNonBlank(address, "postcode")
            val street = firstNonBlank(
                address,
                "road",
                "pedestrian",
                "footway",
                "residential",
                "cycleway",
                "path",
                "square",
                "neighbourhood",
            )
            val houseNumber = firstNonBlank(address, "house_number")
            val unit = firstNonBlank(address, "unit", "flat")

            val fullNumber = when {
                houseNumber != null && unit != null && !houseNumber.contains("/") -> "$houseNumber/$unit"
                houseNumber != null -> houseNumber
                else -> null
            }

            val streetWithNumber = when {
                street != null && fullNumber != null -> "$street $fullNumber"
                street != null -> street
                fullNumber != null -> fullNumber
                else -> null
            }

            val parts = buildList {
                if (locality != null) add(locality)
                if (postcode != null) add(postcode)
                if (streetWithNumber != null) add(streetWithNumber)
            }

            if (parts.isNotEmpty()) {
                return parts.joinToString(", ")
            }
        }

        return fallbackDisplayName?.let { buildShortLabel(it) }?.takeIf { it.isNotBlank() }
    }

    private fun firstNonBlank(obj: JSONObject, vararg keys: String): String? {
        for (key in keys) {
            val value = obj.optString(key).trim()
            if (value.isNotEmpty()) return value
        }
        return null
    }

    internal fun buildShortLabel(displayName: String): String {
        val parts = displayName
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        return when {
            parts.isEmpty() -> displayName.trim()
            parts.size == 1 -> parts[0]
            else -> "${parts[0]}, ${parts[1]}"
        }
    }
}
