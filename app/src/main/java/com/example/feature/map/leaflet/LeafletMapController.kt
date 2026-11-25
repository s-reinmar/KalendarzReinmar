package com.example.feature.map.leaflet

import android.webkit.WebView
import com.example.domain.model.EventId
import com.example.domain.model.GeoPoint
import java.util.Locale

class LeafletMapController {
    private var webView: WebView? = null
    private var mapReady: Boolean = false
    private val pendingScripts = mutableListOf<String>()

    internal fun attach(view: WebView) {
        webView = view
        mapReady = false
    }

    internal fun onMapReady(view: WebView) {
        if (webView !== view || mapReady) return
        mapReady = true
        val queued = pendingScripts.toList()
        pendingScripts.clear()
        queued.forEach { script ->
            runCatching { view.evaluateJavascript(script, null) }
        }
    }

    internal fun detach(view: WebView? = null) {
        if (view != null && webView !== view) return
        pendingScripts.clear()
        mapReady = false
        webView = null
    }

    fun setMarkers(markers: List<LeafletMapMarker>, fitBounds: Boolean) {
        val json = LeafletMarkerJsonEncoder.encode(markers)
        runJs("window.ReinmarMap && window.ReinmarMap.setMarkers($json, $fitBounds);")
    }

    fun setDraftPin(point: GeoPoint, panTo: Boolean = true, zoom: Int = DEFAULT_FOCUS_ZOOM) {
        val latStr = formatCoord(point.lat)
        val lngStr = formatCoord(point.lng)
        runJs("window.ReinmarMap && window.ReinmarMap.setDraftPin($latStr, $lngStr, $panTo, $zoom);")
    }

    fun dropPinAtCenter() {
        runJs("window.ReinmarMap && window.ReinmarMap.dropPinAtCenter();")
    }

    fun clearDraftPin() {
        runJs("window.ReinmarMap && window.ReinmarMap.clearDraftPin();")
    }

    fun focusLocation(point: GeoPoint, zoom: Int = DEFAULT_FOCUS_ZOOM) {
        val latStr = formatCoord(point.lat)
        val lngStr = formatCoord(point.lng)
        runJs("window.ReinmarMap && window.ReinmarMap.focusLocation($latStr, $lngStr, $zoom);")
    }

    fun setUserLocation(point: GeoPoint, zoom: Int = DEFAULT_USER_ZOOM) {
        val latStr = formatCoord(point.lat)
        val lngStr = formatCoord(point.lng)
        runJs("window.ReinmarMap && window.ReinmarMap.setUserLocation($latStr, $lngStr, $zoom);")
    }

    fun focusEvent(eventId: EventId) {
        if (!eventId.isValid) return
        runJs("window.ReinmarMap && window.ReinmarMap.focusEvent(${eventId.value});")
    }

    fun fitAllMarkers() {
        runJs("window.ReinmarMap && window.ReinmarMap.fitAllMarkers();")
    }

    private fun runJs(script: String) {
        val view = webView
        if (view != null && mapReady) {
            runCatching { view.evaluateJavascript(script, null) }
        } else {
            if (pendingScripts.size >= MAX_PENDING_SCRIPTS) {
                pendingScripts.removeAt(0)
            }
            pendingScripts.add(script)
        }
    }

    private fun formatCoord(value: Double): String = String.format(Locale.US, "%.6f", value)

    private companion object {
        const val DEFAULT_FOCUS_ZOOM = 14
        const val DEFAULT_USER_ZOOM = 15
        const val MAX_PENDING_SCRIPTS = 32
    }
}
