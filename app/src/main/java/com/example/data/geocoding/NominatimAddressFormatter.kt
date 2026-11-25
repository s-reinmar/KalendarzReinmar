package com.example.data.geocoding

import org.json.JSONObject

object NominatimAddressFormatter {

    fun formatAddress(address: JSONObject?, fallbackDisplayName: String?): String? {
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

        return fallbackDisplayName?.let(::buildShortLabel)?.takeIf { it.isNotBlank() }
    }

    fun buildShortLabel(displayName: String): String {
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

    private fun firstNonBlank(obj: JSONObject, vararg keys: String): String? {
        for (key in keys) {
            val value = obj.optString(key).trim()
            if (value.isNotEmpty()) return value
        }
        return null
    }
}
