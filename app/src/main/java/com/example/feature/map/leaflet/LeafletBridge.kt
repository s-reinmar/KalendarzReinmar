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

    @Volatile
    private var active: Boolean = true

    fun deactivate() {
        active = false
        mainHandler.removeCallbacksAndMessages(null)
    }

    @JavascriptInterface
    fun onMapReady() {
        if (!active) return
        mainHandler.post {
            if (active) onReady()
        }
    }

    @JavascriptInterface
    fun onMapClicked(lat: Double, lng: Double) {
        if (!active) return
        val point = GeoPoint.createOrNull(lat, lng) ?: return
        mainHandler.post {
            if (active) onMapTap(point)
        }
    }

    @JavascriptInterface
    fun onEventMarkerClicked(eventId: Long) {
        if (!active) return
        val id = EventId(eventId)
        if (!id.isValid) return
        mainHandler.post {
            if (active) onMarkerTap(id)
        }
    }
}
