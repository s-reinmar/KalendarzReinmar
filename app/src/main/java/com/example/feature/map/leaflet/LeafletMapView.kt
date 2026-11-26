package com.example.feature.map.leaflet

import android.webkit.WebView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
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
import kotlinx.coroutines.delay

private const val DEFAULT_ZOOM = 6
private const val MAX_AUTO_RELOADS = 2
private const val RENDERER_RECOVERY_BASE_DELAY_MS = 600L

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
    val sessionsByView = remember { mutableMapOf<WebView, LeafletWebViewSession>() }
    var activeSession by remember { mutableStateOf<LeafletWebViewSession?>(null) }
    var sessionKey by remember { mutableIntStateOf(0) }
    var autoReloadCount by remember { mutableIntStateOf(0) }
    var recoveringRenderer by remember { mutableStateOf(false) }
    var rendererCrashed by remember { mutableStateOf(false) }

    val html = remember(initialCenter, initialZoom, enableTapSelection) {
        LeafletHtmlProvider.loadHtml(
            context = context,
            initialCenter = initialCenter,
            initialZoom = initialZoom,
            enableTapSelection = enableTapSelection,
        )
    }

    LaunchedEffect(recoveringRenderer, autoReloadCount) {
        if (recoveringRenderer) {
            delay(RENDERER_RECOVERY_BASE_DELAY_MS * autoReloadCount.coerceAtLeast(1))
            sessionKey++
            recoveringRenderer = false
        }
    }

    LaunchedEffect(sessionKey, markers, fitBoundsOnMarkersChange, recoveringRenderer, rendererCrashed) {
        if (!recoveringRenderer && !rendererCrashed) {
            controller.setMarkers(markers, fitBounds = fitBoundsOnMarkersChange)
        }
    }

    var lastPausedState by remember(sessionKey) { mutableStateOf(false) }
    LaunchedEffect(isPaused, activeSession) {
        val session = activeSession ?: return@LaunchedEffect
        if (isPaused != lastPausedState) {
            lastPausedState = isPaused
            if (isPaused) session.pauseIfActive() else session.resumeIfActive()
        }
    }

    DisposableEffect(lifecycleOwner, activeSession, isPaused) {
        val observer = LifecycleEventObserver { _, event ->
            val session = activeSession ?: return@LifecycleEventObserver
            when (event) {
                Lifecycle.Event.ON_PAUSE -> session.pauseIfActive()
                Lifecycle.Event.ON_RESUME -> if (!isPaused) session.resumeIfActive()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    DisposableEffect(Unit) {
        onDispose {
            val remaining = sessionsByView.values.toList()
            sessionsByView.clear()
            activeSession = null
            remaining.forEach { session ->
                controller.detach(session.webView)
                session.destroyOnce(rendererAlreadyGone = false)
            }
        }
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

    if (recoveringRenderer) {
        MapRecoveringPlaceholder(modifier = modifier)
        return
    }

    key(sessionKey) {
        AndroidView(
            factory = { ctx ->
                val createdSession = LeafletWebViewSession.create(
                    context = ctx,
                    html = html,
                    controller = controller,
                    onReady = { autoReloadCount = 0 },
                    onMapTap = { latestOnMapTap(it) },
                    onMarkerTap = { latestOnMarkerTap(it) },
                    onRendererGone = { goneSession ->
                        controller.detach(goneSession.webView)
                        sessionsByView.remove(goneSession.webView)
                        if (activeSession === goneSession) {
                            activeSession = null
                        }
                        goneSession.destroyOnce(rendererAlreadyGone = true)
                        if (autoReloadCount < MAX_AUTO_RELOADS) {
                            autoReloadCount++
                            recoveringRenderer = true
                        } else {
                            rendererCrashed = true
                        }
                    },
                )
                sessionsByView[createdSession.webView] = createdSession
                activeSession = createdSession
                createdSession.webView
            },
            onRelease = { view ->
                controller.detach(view)
                val session = sessionsByView.remove(view)
                if (activeSession?.webView === view) {
                    activeSession = null
                }
                session?.destroyOnce(rendererAlreadyGone = false)
            },
            modifier = modifier,
        )
    }
}

@Composable
private fun MapRecoveringPlaceholder(modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }
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
