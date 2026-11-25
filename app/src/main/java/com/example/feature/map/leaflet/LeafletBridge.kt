package com.example.feature.map.leaflet

import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import com.example.domain.model.EventId
import com.example.domain.model.GeoPoint

internal class LeafletBridge(
    private val onReady: () -> Unit,
    private val onMapTap: (GeoPoint) -> Unit,
    private val onMarkerTap: (EventId) -> Unit,
) {
    private val mainHandler = Handler(Looper.getMainLooper())

    @JavascriptInterface
    fun onMapReady() {
        mainHandler.post { onReady() }
    }

    @JavascriptInterface
    fun onMapClicked(lat: Double, lng: Double) {
        val point = GeoPoint.createOrNull(lat, lng) ?: return
        mainHandler.post { onMapTap(point) }
    }

    @JavascriptInterface
    fun onEventMarkerClicked(eventId: Long) {
        val id = EventId(eventId)
        if (!id.isValid) return
        mainHandler.post { onMarkerTap(id) }
    }
}
