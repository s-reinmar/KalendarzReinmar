package com.example.ui

import android.annotation.SuppressLint
import android.graphics.Color as AndroidColor
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.R
import com.example.data.GeocodingResult
import com.example.data.NominatimGeocoder
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

data class LeafletMapMarker(
    val id: Long,
    val title: String,
    val subtitle: String,
    val lat: Double,
    val lng: Double,
    val hasReminder: Boolean,
)

private const val DEFAULT_CENTER_LAT = 52.2297
private const val DEFAULT_CENTER_LNG = 21.0122
private const val DEFAULT_ZOOM = 6
private const val FOCUS_ZOOM = 14

private class LeafletBridge(
    private val onReady: () -> Unit,
    private val onMapTap: (Double, Double) -> Unit,
    private val onMarkerTap: (Long) -> Unit,
) {
    private val mainHandler = Handler(Looper.getMainLooper())

    @JavascriptInterface
    fun onMapReady() {
        mainHandler.post { onReady() }
    }

    @JavascriptInterface
    fun onMapClicked(lat: Double, lng: Double) {
        if (!lat.isFinite() || !lng.isFinite()) return
        if (lat !in -90.0..90.0 || lng !in -180.0..180.0) return
        mainHandler.post { onMapTap(lat, lng) }
    }

    @JavascriptInterface
    fun onEventMarkerClicked(eventId: Long) {
        if (eventId <= 0L) return
        mainHandler.post { onMarkerTap(eventId) }
    }
}

class LeafletMapController {
    private var webView: WebView? = null
    private var mapReady: Boolean = false
    private val pendingScripts = mutableListOf<String>()

    internal fun attach(view: WebView) {
        webView = view
        mapReady = false
    }

    internal fun onMapReady() {
        if (mapReady) return
        mapReady = true
        val view = webView ?: return
        val queued = pendingScripts.toList()
        pendingScripts.clear()
        queued.forEach { script ->
            view.evaluateJavascript(script, null)
        }
    }

    internal fun detach() {
        pendingScripts.clear()
        mapReady = false
        webView = null
    }

    fun setMarkers(markers: List<LeafletMapMarker>, fitBounds: Boolean) {
        val json = encodeMarkersJson(markers)
        runJs("window.ReinmarMap && window.ReinmarMap.setMarkers($json, $fitBounds);")
    }

    fun setDraftPin(lat: Double, lng: Double, panTo: Boolean = true, zoom: Int = FOCUS_ZOOM) {
        if (!lat.isFinite() || !lng.isFinite()) return
        if (lat !in -90.0..90.0 || lng !in -180.0..180.0) return
        val latStr = String.format(Locale.US, "%.6f", lat)
        val lngStr = String.format(Locale.US, "%.6f", lng)
        runJs("window.ReinmarMap && window.ReinmarMap.setDraftPin($latStr, $lngStr, $panTo, $zoom);")
    }

    fun dropPinAtCenter() {
        runJs("window.ReinmarMap && window.ReinmarMap.dropPinAtCenter();")
    }

    fun clearDraftPin() {
        runJs("window.ReinmarMap && window.ReinmarMap.clearDraftPin();")
    }

    fun focusLocation(lat: Double, lng: Double, zoom: Int = FOCUS_ZOOM) {
        if (!lat.isFinite() || !lng.isFinite()) return
        if (lat !in -90.0..90.0 || lng !in -180.0..180.0) return
        val latStr = String.format(Locale.US, "%.6f", lat)
        val lngStr = String.format(Locale.US, "%.6f", lng)
        runJs("window.ReinmarMap && window.ReinmarMap.focusLocation($latStr, $lngStr, $zoom);")
    }

    fun setUserLocation(lat: Double, lng: Double, zoom: Int = 15) {
        if (!lat.isFinite() || !lng.isFinite()) return
        if (lat !in -90.0..90.0 || lng !in -180.0..180.0) return
        val latStr = String.format(Locale.US, "%.6f", lat)
        val lngStr = String.format(Locale.US, "%.6f", lng)
        runJs("window.ReinmarMap && window.ReinmarMap.setUserLocation($latStr, $lngStr, $zoom);")
    }

    fun focusEvent(eventId: Long) {
        if (eventId <= 0L) return
        runJs("window.ReinmarMap && window.ReinmarMap.focusEvent($eventId);")
    }

    fun fitAllMarkers() {
        runJs("window.ReinmarMap && window.ReinmarMap.fitAllMarkers();")
    }

    private fun runJs(script: String) {
        val view = webView
        if (view != null && mapReady) {
            view.evaluateJavascript(script, null)
        } else {
            pendingScripts.add(script)
        }
    }
}

private fun encodeMarkersJson(markers: List<LeafletMapMarker>): String {
    val array = JSONArray()
    for (marker in markers) {
        if (!marker.lat.isFinite() || !marker.lng.isFinite()) continue
        if (marker.lat !in -90.0..90.0 || marker.lng !in -180.0..180.0) continue
        val obj = JSONObject().apply {
            put("id", marker.id)
            put("title", marker.title)
            put("subtitle", marker.subtitle)
            put("lat", marker.lat)
            put("lng", marker.lng)
            put("hasReminder", marker.hasReminder)
        }
        array.put(obj)
    }
    return array.toString()
        .replace("</", "<\\/")
        .replace("\u2028", "\\u2028")
        .replace("\u2029", "\\u2029")
}

private fun buildLeafletHtml(
    initialLat: Double,
    initialLng: Double,
    initialZoom: Int,
    enableTapSelection: Boolean,
): String {
    val latStr = String.format(Locale.US, "%.6f", initialLat)
    val lngStr = String.format(Locale.US, "%.6f", initialLng)
    val tapFlag = if (enableTapSelection) "true" else "false"

    return """
<!DOCTYPE html>
<html lang="pl">
<head>
  <meta charset="utf-8" />
  <meta name="referrer" content="origin" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
  <style>
    * { box-sizing: border-box; }
    html, body {
      position: fixed;
      inset: 0;
      width: 100%;
      height: 100%;
      margin: 0;
      padding: 0;
      overflow: hidden;
      background: #EFECE4;
      font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
    }
    #map {
      position: fixed;
      inset: 0;
      width: 100%;
      height: 100%;
      background: #EFECE4;
    }
    #map-status {
      position: fixed;
      inset: 0;
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 20px;
      text-align: center;
      color: #525B56;
      font-size: 14px;
      background: #EFECE4;
      z-index: 1000;
    }
    .reinmar-pin-wrap {
      background: transparent;
      border: none;
    }
    .reinmar-popup-title {
      font-weight: 600;
      font-size: 13px;
      color: #1A1E1C;
      margin-bottom: 2px;
    }
    .reinmar-popup-sub {
      font-size: 11px;
      color: #525B56;
    }
  </style>
</head>
<body>
  <div id="map"></div>
  <div id="map-status">Ładowanie mapy OpenStreetMap…</div>
  <script>
    (function() {
      var SOURCES = [
        {
          css: 'https://unpkg.com/leaflet@1.9.4/dist/leaflet.css',
          js: 'https://unpkg.com/leaflet@1.9.4/dist/leaflet.js'
        },
        {
          css: 'https://cdn.jsdelivr.net/npm/leaflet@1.9.4/dist/leaflet.css',
          js: 'https://cdn.jsdelivr.net/npm/leaflet@1.9.4/dist/leaflet.js'
        },
        {
          css: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/leaflet.css',
          js: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/leaflet.js'
        }
      ];

      function hideStatus() {
        var el = document.getElementById('map-status');
        if (el) el.style.display = 'none';
      }

      function showError(msg) {
        var el = document.getElementById('map-status');
        if (el) {
          el.style.display = 'flex';
          el.textContent = msg;
        }
      }

      function loadSource(index) {
        if (index >= SOURCES.length) {
          showError('Nie udało się załadować biblioteki mapy Leaflet. Sprawdź połączenie z internetem.');
          return;
        }
        var src = SOURCES[index];
        var settled = false;
        var link = document.createElement('link');
        link.rel = 'stylesheet';
        link.href = src.css;
        document.head.appendChild(link);

        var script = document.createElement('script');
        script.src = src.js;
        script.async = true;

        var fallbackTimer = setTimeout(function() {
          if (!settled) {
            settled = true;
            if (link.parentNode) link.parentNode.removeChild(link);
            if (script.parentNode) script.parentNode.removeChild(script);
            loadSource(index + 1);
          }
        }, 5000);

        script.onload = function() {
          if (settled) return;
          settled = true;
          clearTimeout(fallbackTimer);
          if (typeof L !== 'undefined') {
            initMap();
          } else {
            loadSource(index + 1);
          }
        };
        script.onerror = function() {
          if (settled) return;
          settled = true;
          clearTimeout(fallbackTimer);
          if (link.parentNode) link.parentNode.removeChild(link);
          if (script.parentNode) script.parentNode.removeChild(script);
          loadSource(index + 1);
        };
        document.head.appendChild(script);
      }

      function createSvgPinIcon(fillColor, ringColor, sizeScale) {
        var w = Math.round(32 * (sizeScale || 1));
        var h = Math.round(42 * (sizeScale || 1));
        var svg =
          '<svg xmlns="http://www.w3.org/2000/svg" width="' + w + '" height="' + h + '" viewBox="0 0 32 42">' +
            '<ellipse cx="16" cy="39" rx="7" ry="2.6" fill="rgba(0,0,0,0.25)"/>' +
            '<path d="M16 2C8.82 2 3 7.82 3 15c0 9.5 13 24 13 24s13-14.5 13-24C29 7.82 23.18 2 16 2z" ' +
              'fill="' + fillColor + '" stroke="#FFFFFF" stroke-width="2.5"/>' +
            '<circle cx="16" cy="15" r="5.2" fill="' + ringColor + '"/>' +
          '</svg>';
        return L.divIcon({
          className: 'reinmar-pin-wrap',
          html: svg,
          iconSize: [w, h],
          iconAnchor: [Math.round(w / 2), h - 2],
          popupAnchor: [0, -(h - 6)]
        });
      }

      function buildSafePopupNode(title, subtitle) {
        var wrapper = document.createElement('div');
        var titleEl = document.createElement('div');
        titleEl.className = 'reinmar-popup-title';
        titleEl.textContent = title || '';
        wrapper.appendChild(titleEl);
        if (subtitle) {
          var subEl = document.createElement('div');
          subEl.className = 'reinmar-popup-sub';
          subEl.textContent = subtitle;
          wrapper.appendChild(subEl);
        }
        return wrapper;
      }

      function initMap() {
        hideStatus();

        var map = L.map('map', {
          zoomControl: true,
          attributionControl: true,
          preferCanvas: true,
          fadeAnimation: false,
          zoomAnimation: true
        }).setView([$latStr, $lngStr], $initialZoom);

        var osmLayer = L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
          maxZoom: 19,
          keepBuffer: 1,
          updateWhenIdle: true,
          updateWhenZooming: false,
          crossOrigin: false,
          attribution: '&copy; OpenStreetMap contributors'
        });

        osmLayer.addTo(map);

        var defaultIcon = createSvgPinIcon('#1B3C33', '#FFFFFF', 1.0);
        var reminderIcon = createSvgPinIcon('#C85A32', '#FFFFFF', 1.05);
        var draftIcon = createSvgPinIcon('#C85A32', '#FFF3EC', 1.15);

        var eventMarkerLayer = L.layerGroup().addTo(map);
        var markerMap = {};
        var draftMarker = null;
        var userLocationHalo = null;
        var userLocationDot = null;

        function notifyPicked(lat, lng) {
          if (window.ReinmarMapBridge && typeof window.ReinmarMapBridge.onMapClicked === 'function') {
            window.ReinmarMapBridge.onMapClicked(lat, lng);
          }
        }

        function attachDraftMarker(lat, lng) {
          if (draftMarker) {
            draftMarker.setLatLng([lat, lng]);
          } else {
            draftMarker = L.marker([lat, lng], {
              icon: draftIcon,
              draggable: $tapFlag,
              zIndexOffset: 500
            }).addTo(map);
            if ($tapFlag) {
              draftMarker.on('dragend', function(ev) {
                var pos = ev.target.getLatLng();
                notifyPicked(pos.lat, pos.lng);
              });
            }
          }
        }

        function fitAll() {
          var latLngs = [];
          Object.keys(markerMap).forEach(function(key) {
            latLngs.push(markerMap[key].getLatLng());
          });
          if (latLngs.length === 1) {
            map.setView(latLngs[0], 14);
          } else if (latLngs.length > 1) {
            map.fitBounds(L.latLngBounds(latLngs), { padding: [48, 48], maxZoom: 15 });
          }
        }

        window.ReinmarMap = {
          setMarkers: function(items, shouldFit) {
            eventMarkerLayer.clearLayers();
            markerMap = {};
            if (!Array.isArray(items)) return;
            items.forEach(function(item) {
              if (typeof item.lat !== 'number' || typeof item.lng !== 'number') return;
              var icon = item.hasReminder ? reminderIcon : defaultIcon;
              var marker = L.marker([item.lat, item.lng], { icon: icon });
              marker.bindPopup(buildSafePopupNode(item.title, item.subtitle));
              marker.on('click', function() {
                if (window.ReinmarMapBridge && typeof window.ReinmarMapBridge.onEventMarkerClicked === 'function') {
                  window.ReinmarMapBridge.onEventMarkerClicked(item.id);
                }
              });
              marker.addTo(eventMarkerLayer);
              markerMap[String(item.id)] = marker;
            });
            if (shouldFit) {
              fitAll();
            }
          },
          setDraftPin: function(lat, lng, panTo, zoom) {
            attachDraftMarker(lat, lng);
            if (panTo) {
              map.setView([lat, lng], zoom || 14);
            }
          },
          dropPinAtCenter: function() {
            var center = map.getCenter();
            attachDraftMarker(center.lat, center.lng);
            notifyPicked(center.lat, center.lng);
          },
          clearDraftPin: function() {
            if (draftMarker) {
              map.removeLayer(draftMarker);
              draftMarker = null;
            }
          },
          focusLocation: function(lat, lng, zoom) {
            map.setView([lat, lng], zoom || 14);
          },
          setUserLocation: function(lat, lng, zoom) {
            if (userLocationHalo) {
              userLocationHalo.setLatLng([lat, lng]);
            } else {
              userLocationHalo = L.circleMarker([lat, lng], {
                radius: 16,
                color: '#1A73E8',
                weight: 1,
                opacity: 0.35,
                fillColor: '#1A73E8',
                fillOpacity: 0.16,
                interactive: false
              }).addTo(map);
            }
            if (userLocationDot) {
              userLocationDot.setLatLng([lat, lng]);
            } else {
              userLocationDot = L.circleMarker([lat, lng], {
                radius: 7,
                color: '#FFFFFF',
                weight: 2.5,
                opacity: 1,
                fillColor: '#1A73E8',
                fillOpacity: 1,
                interactive: false
              }).addTo(map);
            }
            map.setView([lat, lng], zoom || 15);
          },
          focusEvent: function(eventId) {
            var marker = markerMap[String(eventId)];
            if (marker) {
              map.setView(marker.getLatLng(), 15);
              marker.openPopup();
            }
          },
          fitAllMarkers: function() {
            fitAll();
          }
        };

        if ($tapFlag) {
          map.on('click', function(e) {
            var lat = e.latlng.lat;
            var lng = e.latlng.lng;
            attachDraftMarker(lat, lng);
            notifyPicked(lat, lng);
          });
        }

        var resizeTimer = null;
        function refreshSize() {
          if (resizeTimer) clearTimeout(resizeTimer);
          resizeTimer = setTimeout(function() {
            map.invalidateSize({ animate: false, pan: false });
          }, 120);
        }
        window.addEventListener('resize', refreshSize);
        if (typeof ResizeObserver !== 'undefined') {
          new ResizeObserver(refreshSize).observe(document.getElementById('map'));
        }
        setTimeout(refreshSize, 150);
        setTimeout(refreshSize, 500);

        if (window.ReinmarMapBridge && typeof window.ReinmarMapBridge.onMapReady === 'function') {
          window.ReinmarMapBridge.onMapReady();
        }
      }

      loadSource(0);
    })();
  </script>
</body>
</html>
""".trimIndent()
}

private fun safelyDestroyWebView(view: WebView) {
    runCatching {
        view.stopLoading()
        view.removeJavascriptInterface("ReinmarMapBridge")
        view.setOnTouchListener(null)
        view.webChromeClient = null
        view.onPause()
        (view.parent as? ViewGroup)?.removeView(view)
        view.removeAllViews()
        view.destroy()
    }
}

@SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
@Composable
fun LeafletMapView(
    modifier: Modifier = Modifier,
    controller: LeafletMapController = remember { LeafletMapController() },
    markers: List<LeafletMapMarker> = emptyList(),
    fitBoundsOnMarkersChange: Boolean = false,
    initialCenterLat: Double = DEFAULT_CENTER_LAT,
    initialCenterLng: Double = DEFAULT_CENTER_LNG,
    initialZoom: Int = DEFAULT_ZOOM,
    enableTapSelection: Boolean = true,
    isPaused: Boolean = false,
    onMapTap: (Double, Double) -> Unit = { _, _ -> },
    onMarkerTap: (Long) -> Unit = {},
) {
    val latestOnMapTap by rememberUpdatedState(onMapTap)
    val latestOnMarkerTap by rememberUpdatedState(onMarkerTap)
    val lifecycleOwner = LocalLifecycleOwner.current
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var sessionKey by remember { mutableIntStateOf(0) }
    var autoReloadCount by remember { mutableIntStateOf(0) }
    var rendererCrashed by remember { mutableStateOf(false) }

    val safeCenterLat = if (initialCenterLat.isFinite() && initialCenterLat in -90.0..90.0) {
        initialCenterLat
    } else {
        DEFAULT_CENTER_LAT
    }
    val safeCenterLng = if (initialCenterLng.isFinite() && initialCenterLng in -180.0..180.0) {
        initialCenterLng
    } else {
        DEFAULT_CENTER_LNG
    }

    val html = remember(safeCenterLat, safeCenterLng, initialZoom, enableTapSelection) {
        buildLeafletHtml(
            initialLat = safeCenterLat,
            initialLng = safeCenterLng,
            initialZoom = initialZoom,
            enableTapSelection = enableTapSelection,
        )
    }

    LaunchedEffect(sessionKey, markers, fitBoundsOnMarkersChange) {
        if (!rendererCrashed) {
            controller.setMarkers(markers, fitBounds = fitBoundsOnMarkersChange)
        }
    }

    LaunchedEffect(isPaused, webViewRef) {
        val view = webViewRef ?: return@LaunchedEffect
        if (isPaused) {
            runCatching { view.onPause() }
        } else {
            runCatching { view.onResume() }
        }
    }

    DisposableEffect(lifecycleOwner, webViewRef, isPaused) {
        val observer = LifecycleEventObserver { _, event ->
            val view = webViewRef ?: return@LifecycleEventObserver
            when (event) {
                Lifecycle.Event.ON_PAUSE -> runCatching { view.onPause() }
                Lifecycle.Event.ON_RESUME -> if (!isPaused) runCatching { view.onResume() }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    if (rendererCrashed) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = modifier,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = stringResource(R.string.map_renderer_crashed),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    FilledTonalButton(
                        onClick = {
                            autoReloadCount = 0
                            rendererCrashed = false
                            sessionKey++
                        },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("map_reload_button"),
                    ) {
                        Text(stringResource(R.string.map_reload_action))
                    }
                }
            }
        }
        return
    }

    key(sessionKey) {
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                    setBackgroundColor(AndroidColor.parseColor("#EFECE4"))
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        setRendererPriorityPolicy(WebView.RENDERER_PRIORITY_BOUND, true)
                    }
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.loadsImagesAutomatically = true
                    settings.blockNetworkImage = false
                    settings.blockNetworkLoads = false
                    settings.useWideViewPort = true
                    settings.loadWithOverviewMode = true
                    settings.cacheMode = WebSettings.LOAD_DEFAULT
                    settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                    settings.allowFileAccess = false
                    settings.allowContentAccess = false
                    settings.databaseEnabled = false
                    val sanitizedUa = settings.userAgentString
                        .replace("; wv)", ")")
                        .replace("; wv;", ";")
                    settings.userAgentString = "$sanitizedUa KalendarzReinmar/1.0"
                    webChromeClient = WebChromeClient()
                    controller.attach(this)
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(
                            view: WebView?,
                            request: WebResourceRequest?,
                        ): Boolean = true

                        override fun onRenderProcessGone(
                            view: WebView?,
                            detail: RenderProcessGoneDetail?,
                        ): Boolean {
                            controller.detach()
                            if (webViewRef === view) {
                                webViewRef = null
                            }
                            if (view != null) {
                                safelyDestroyWebView(view)
                            }
                            if (autoReloadCount < 2) {
                                autoReloadCount++
                                sessionKey++
                            } else {
                                rendererCrashed = true
                            }
                            return true
                        }
                    }
                    setOnTouchListener { v, event ->
                        when (event.actionMasked) {
                            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                                v.parent?.requestDisallowInterceptTouchEvent(true)
                            }
                            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                                v.parent?.requestDisallowInterceptTouchEvent(false)
                            }
                        }
                        false
                    }
                    addJavascriptInterface(
                        LeafletBridge(
                            onReady = { controller.onMapReady() },
                            onMapTap = { lat, lng -> latestOnMapTap(lat, lng) },
                            onMarkerTap = { eventId -> latestOnMarkerTap(eventId) },
                        ),
                        "ReinmarMapBridge",
                    )
                    loadDataWithBaseURL(
                        "https://kalendarzreinmar.app/",
                        html,
                        "text/html",
                        "UTF-8",
                        null,
                    )
                    webViewRef = this
                }
            },
            onRelease = { view ->
                controller.detach()
                if (webViewRef === view) {
                    webViewRef = null
                }
                safelyDestroyWebView(view)
            },
            modifier = modifier,
        )
    }
}

@Composable
fun OsmSearchBar(
    onResultSelected: (GeocodingResult) -> Unit,
    modifier: Modifier = Modifier,
    testTagPrefix: String = "osm_search",
) {
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    var query by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var results by remember { mutableStateOf<List<GeocodingResult>>(emptyList()) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    val noResultsText = stringResource(R.string.map_search_no_results)
    val errorText = stringResource(R.string.map_search_error)

    fun performSearch() {
        val trimmed = query.trim()
        if (trimmed.length < 2) return
        keyboardController?.hide()
        isSearching = true
        statusMessage = null
        scope.launch {
            val outcome = NominatimGeocoder.search(trimmed)
            isSearching = false
            outcome
                .onSuccess { list ->
                    results = list
                    statusMessage = if (list.isEmpty()) noResultsText else null
                }
                .onFailure {
                    results = emptyList()
                    statusMessage = errorText
                }
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = {
                query = it
                if (it.isBlank()) {
                    results = emptyList()
                    statusMessage = null
                }
            },
            placeholder = {
                Text(
                    text = stringResource(R.string.map_search_placeholder),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = stringResource(R.string.map_search_action),
                    tint = MaterialTheme.colorScheme.primary,
                )
            },
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isSearching) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .size(20.dp),
                            strokeWidth = 2.dp,
                        )
                    } else if (query.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                query = ""
                                results = emptyList()
                                statusMessage = null
                            },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("${testTagPrefix}_clear_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = stringResource(R.string.map_search_clear),
                            )
                        }
                        IconButton(
                            onClick = { performSearch() },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("${testTagPrefix}_submit_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = stringResource(R.string.map_search_action),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { performSearch() }),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("${testTagPrefix}_input"),
        )

        if (results.isNotEmpty()) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp,
                shadowElevation = 4.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("${testTagPrefix}_results_card"),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    results.forEachIndexed { idx, item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    results = emptyList()
                                    statusMessage = null
                                    query = item.shortName
                                    onResultSelected(item)
                                }
                                .padding(horizontal = 14.dp, vertical = 12.dp)
                                .testTag("${testTagPrefix}_result_$idx"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp),
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.shortName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = item.displayName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                        if (idx < results.lastIndex) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                }
            }
        } else if (statusMessage != null) {
            Text(
                text = statusMessage.orEmpty(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .testTag("${testTagPrefix}_status_text"),
            )
        }
    }
}
