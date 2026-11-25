package com.example.feature.map.leaflet

import android.content.Context
import com.example.domain.model.GeoPoint
import java.util.Locale

object LeafletHtmlProvider {
    private const val ASSET_FILE_NAME = "leaflet_map.html"

    fun loadHtml(
        context: Context,
        initialCenter: GeoPoint,
        initialZoom: Int,
        enableTapSelection: Boolean,
    ): String {
        val template = context.assets.open(ASSET_FILE_NAME)
            .bufferedReader(Charsets.UTF_8)
            .use { it.readText() }
        val latStr = String.format(Locale.US, "%.6f", initialCenter.lat)
        val lngStr = String.format(Locale.US, "%.6f", initialCenter.lng)
        val tapFlag = if (enableTapSelection) "true" else "false"
        return template
            .replace("{{INITIAL_LAT}}", latStr)
            .replace("{{INITIAL_LNG}}", lngStr)
            .replace("{{INITIAL_ZOOM}}", initialZoom.toString())
            .replace("{{ENABLE_TAP}}", tapFlag)
    }
}
