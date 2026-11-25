package com.example.feature.map.leaflet

import android.annotation.SuppressLint
import android.graphics.Color as AndroidColor
import android.os.Build
import android.view.MotionEvent
import android.view.ViewGroup
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.R
import com.example.domain.model.EventId
import com.example.domain.model.GeoPoint

private const val DEFAULT_ZOOM = 6
private const val MAP_BASE_URL = "https://kalendarzreinmar.app/"
private const val JS_BRIDGE_NAME = "ReinmarMapBridge"
private const val MAX_AUTO_RELOADS = 2

@SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
@Composable
fun LeafletMapView(
    modifier: Modifier = Modifier,
    controller: LeafletMapController = remember { LeafletMapController() },
    markers: List<LeafletMapMarker> = emptyList(),
    fitBoundsOnMarkersChange: Boolean = false,
    initialCenter: GeoPoint = GeoPoint.DEFAULT_POLAND_CENTER,
    initialZoom: Int = DEFAULT_ZOOM,
    enableTapSelection: Boolean = true,
    isPaused: Boolean = false,
    onMapTap: (GeoPoint) -> Unit = {},
    onMarkerTap: (EventId) -> Unit = {},
) {
    val context = LocalContext.current
    val latestOnMapTap by rememberUpdatedState(onMapTap)
    val latestOnMarkerTap by rememberUpdatedState(onMarkerTap)
    val lifecycleOwner = LocalLifecycleOwner.current
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var sessionKey by remember { mutableIntStateOf(0) }
    var autoReloadCount by remember { mutableIntStateOf(0) }
    var rendererCrashed by remember { mutableStateOf(false) }

    val html = remember(initialCenter, initialZoom, enableTapSelection) {
        LeafletHtmlProvider.loadHtml(
            context = context,
            initialCenter = initialCenter,
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
        if (isPaused) runCatching { view.onPause() } else runCatching { view.onResume() }
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
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (rendererCrashed) {
        MapCrashFallback(
            onReload = {
                autoReloadCount = 0
                rendererCrashed = false
                sessionKey++
            },
            modifier = modifier,
        )
        return
    }

    key(sessionKey) {
        AndroidView(
            factory = { ctx ->
                createLeafletWebView(
                    context = ctx,
                    html = html,
                    controller = controller,
                    onMapTap = { latestOnMapTap(it) },
                    onMarkerTap = { latestOnMarkerTap(it) },
                    onRendererGone = { crashedView ->
                        controller.detach()
                        if (webViewRef === crashedView) webViewRef = null
                        safelyDestroyWebView(crashedView)
                        if (autoReloadCount < MAX_AUTO_RELOADS) {
                            autoReloadCount++
                            sessionKey++
                        } else {
                            rendererCrashed = true
                        }
                    },
                ).also { webViewRef = it }
            },
            onRelease = { view ->
                controller.detach()
                if (webViewRef === view) webViewRef = null
                safelyDestroyWebView(view)
            },
            modifier = modifier,
        )
    }
}

@Composable
private fun MapCrashFallback(
    onReload: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
                    onClick = onReload,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("map_reload_button"),
                ) {
                    Text(stringResource(R.string.map_reload_action))
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
private fun createLeafletWebView(
    context: android.content.Context,
    html: String,
    controller: LeafletMapController,
    onMapTap: (GeoPoint) -> Unit,
    onMarkerTap: (EventId) -> Unit,
    onRendererGone: (WebView) -> Unit,
): WebView = WebView(context).apply {
    layoutParams = ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT,
    )
    setBackgroundColor(AndroidColor.parseColor("#EFECE4"))
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        setRendererPriorityPolicy(WebView.RENDERER_PRIORITY_BOUND, true)
    }
    configureLeafletSettings(settings)
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
            if (view != null) onRendererGone(view)
            return true
        }
    }
    setOnTouchListener { view, event ->
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                view.parent?.requestDisallowInterceptTouchEvent(true)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                view.parent?.requestDisallowInterceptTouchEvent(false)
            }
        }
        false
    }
    addJavascriptInterface(
        LeafletBridge(
            onReady = { controller.onMapReady() },
            onMapTap = onMapTap,
            onMarkerTap = onMarkerTap,
        ),
        JS_BRIDGE_NAME,
    )
    loadDataWithBaseURL(MAP_BASE_URL, html, "text/html", "UTF-8", null)
}

@SuppressLint("SetJavaScriptEnabled")
private fun configureLeafletSettings(settings: WebSettings) {
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
}

private fun safelyDestroyWebView(view: WebView) {
    runCatching {
        view.stopLoading()
        view.removeJavascriptInterface(JS_BRIDGE_NAME)
        view.setOnTouchListener(null)
        view.webChromeClient = null
        view.onPause()
        (view.parent as? ViewGroup)?.removeView(view)
        view.removeAllViews()
        view.destroy()
    }
}
