package com.example.feature.map.leaflet

import com.example.domain.model.EventId
import com.example.domain.model.GeoPoint
import org.json.JSONArray
import org.json.JSONObject

data class LeafletMapMarker(
    val id: EventId,
    val title: String,
    val subtitle: String,
    val point: GeoPoint,
    val hasReminder: Boolean,
)

object LeafletMarkerJsonEncoder {
    fun encode(markers: List<LeafletMapMarker>): String {
        val array = JSONArray()
        for (marker in markers) {
            val obj = JSONObject().apply {
                put("id", marker.id.value)
                put("title", marker.title)
                put("subtitle", marker.subtitle)
                put("lat", marker.point.lat)
                put("lng", marker.point.lng)
                put("hasReminder", marker.hasReminder)
            }
            array.put(obj)
        }
        return array.toString()
            .replace("</", "<\\/")
            .replace("\u2028", "\\u2028")
            .replace("\u2029", "\\u2029")
    }
}
