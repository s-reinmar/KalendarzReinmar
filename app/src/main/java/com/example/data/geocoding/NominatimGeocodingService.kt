package com.example.data.geocoding

import com.example.domain.model.GeoPoint
import com.example.domain.model.GeocodingResult
import com.example.domain.port.GeocodingService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale

class NominatimGeocodingService : GeocodingService {

    override suspend fun search(query: String): Result<List<GeocodingResult>> =
        withContext(Dispatchers.IO) {
            val trimmed = query.trim()
            if (trimmed.length < 2) {
                return@withContext Result.success(emptyList())
            }

            var connection: HttpURLConnection? = null
            try {
                val encodedQuery = URLEncoder.encode(trimmed, Charsets.UTF_8.name())
                val endpoint = URL(
                    "$SEARCH_BASE_URL?format=jsonv2&limit=5&addressdetails=1&q=$encodedQuery"
                )
                connection = openConnection(endpoint)

                val statusCode = connection.responseCode
                if (statusCode !in 200..299) {
                    return@withContext Result.failure(IllegalStateException("HTTP $statusCode"))
                }

                val body = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                Result.success(parseSearchResults(body))
            } catch (ex: Exception) {
                Result.failure(ex)
            } finally {
                connection?.disconnect()
            }
        }

    override suspend fun reverseGeocode(point: GeoPoint): Result<String?> =
        withContext(Dispatchers.IO) {
            var connection: HttpURLConnection? = null
            try {
                val latParam = String.format(Locale.US, "%.6f", point.lat)
                val lngParam = String.format(Locale.US, "%.6f", point.lng)
                val endpoint = URL(
                    "$REVERSE_BASE_URL?format=jsonv2&addressdetails=1&zoom=18&lat=$latParam&lon=$lngParam"
                )
                connection = openConnection(endpoint)

                val statusCode = connection.responseCode
                if (statusCode !in 200..299) {
                    return@withContext Result.failure(IllegalStateException("HTTP $statusCode"))
                }

                val body = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                val jsonObject = JSONObject(body)
                val addressObj = jsonObject.optJSONObject("address")
                val displayName = jsonObject.optString("display_name").trim().takeIf { it.isNotEmpty() }
                Result.success(NominatimAddressFormatter.formatAddress(addressObj, displayName))
            } catch (ex: Exception) {
                Result.failure(ex)
            } finally {
                connection?.disconnect()
            }
        }

    private fun parseSearchResults(body: String): List<GeocodingResult> {
        val jsonArray = JSONArray(body)
        return buildList {
            for (index in 0 until jsonArray.length()) {
                val item = jsonArray.optJSONObject(index) ?: continue
                val point = GeoPoint.createOrNull(
                    lat = item.optString("lat").toDoubleOrNull(),
                    lng = item.optString("lon").toDoubleOrNull(),
                ) ?: continue

                val displayName = item.optString("display_name").trim()
                if (displayName.isEmpty()) continue
                val addressObj = item.optJSONObject("address")
                val shortName = NominatimAddressFormatter.formatAddress(addressObj, displayName)
                    ?: NominatimAddressFormatter.buildShortLabel(displayName)
                add(
                    GeocodingResult(
                        displayName = displayName,
                        shortName = shortName,
                        point = point,
                    )
                )
            }
        }
    }

    private fun openConnection(endpoint: URL): HttpURLConnection {
        return (endpoint.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            setRequestProperty("User-Agent", USER_AGENT)
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Accept-Language", "pl,en;q=0.8")
        }
    }

    private companion object {
        const val SEARCH_BASE_URL = "https://nominatim.openstreetmap.org/search"
        const val REVERSE_BASE_URL = "https://nominatim.openstreetmap.org/reverse"
        const val USER_AGENT = "KalendarzReinmar/1.0 (Android; OpenStreetMap Leaflet)"
        const val CONNECT_TIMEOUT_MS = 8_000
        const val READ_TIMEOUT_MS = 8_000
    }
}
