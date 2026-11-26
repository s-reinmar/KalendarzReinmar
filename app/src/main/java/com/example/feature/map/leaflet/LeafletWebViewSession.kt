package com.example.feature.map.leaflet

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color as AndroidColor
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import com.example.domain.model.EventId
import com.example.domain.model.GeoPoint

private const val MAP_BASE_URL = "https://kalendarzreinmar.app/"
private const val JS_BRIDGE_NAME = "ReinmarMapBridge"

/**
 * Fallback WebViewClient kept during teardown so any late renderer termination
 * is always handled safely instead of crashing the host process.
 */
private object TeardownSafeWebViewClient : WebViewClient() {
    override fun shouldOverrideUrlLoading(
        view: WebView?,
        request: WebResourceRequest?,
    ): Boolean = true

    override fun onRenderProcessGone(
        view: WebView?,
        detail: RenderProcessGoneDetail?,
    ): Boolean = true
}

internal class LeafletWebViewSession private constructor(
    val webView: WebView,
    val bridge: LeafletBridge,
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var destroyed: Boolean = false

    val isActive: Boolean
        get() = !destroyed

    fun pauseIfActive() {
        if (!destroyed) runCatching { webView.onPause() }
    }

    fun resumeIfActive() {
        if (!destroyed) runCatching { webView.onResume() }
    }

    fun destroyOnce(rendererAlreadyGone: Boolean) {
        if (destroyed) return
        destroyed = true
        bridge.deactivate()

        runCatching {
            if (!rendererAlreadyGone) {
                webView.stopLoading()
                webView.onPause()
            }
            webView.webViewClient = TeardownSafeWebViewClient
            webView.webChromeClient = null
            webView.setOnTouchListener(null)
            webView.removeJavascriptInterface(JS_BRIDGE_NAME)
            (webView.parent as? ViewGroup)?.removeView(webView)
        }

        val finalizeDestroy = Runnable {
            runCatching {
                webView.removeAllViews()
                webView.destroy()
            }
        }
        if (Looper.myLooper() == Looper.getMainLooper() && !rendererAlreadyGone) {
            finalizeDestroy.run()
        } else {
            mainHandler.post(finalizeDestroy)
        }
    }

    companion object {
        @SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
        fun create(
            context: Context,
            html: String,
            controller: LeafletMapController,
            onReady: () -> Unit,
            onMapTap: (GeoPoint) -> Unit,
            onMarkerTap: (EventId) -> Unit,
            onRendererGone: (LeafletWebViewSession) -> Unit,
        ): LeafletWebViewSession {
            val webView = WebView(context)
            val bridge = LeafletBridge(
                onReady = {
                    controller.onMapReady(webView)
                    onReady()
                },
                onMapTap = onMapTap,
                onMarkerTap = onMarkerTap,
            )
            val session = LeafletWebViewSession(webView = webView, bridge = bridge)

            webView.apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
                setBackgroundColor(AndroidColor.parseColor("#EFECE4"))
                // Use default hardware-accelerated pipeline; forcing LAYER_TYPE_SOFTWARE on
                // full-screen Chromium WebView can exhaust renderer heap and trigger SIGKILL (-1).
                setLayerType(View.LAYER_TYPE_NONE, null)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    setRendererPriorityPolicy(WebView.RENDERER_PRIORITY_IMPORTANT, true)
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
                        if (session.isActive) {
                            onRendererGone(session)
                        }
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
                addJavascriptInterface(bridge, JS_BRIDGE_NAME)
                loadDataWithBaseURL(MAP_BASE_URL, html, "text/html", "UTF-8", null)
            }

            return session
        }

        @SuppressLint("SetJavaScriptEnabled")
        private fun configureLeafletSettings(settings: WebSettings) {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = false
            settings.databaseEnabled = false
            settings.setGeolocationEnabled(false)
            settings.setSupportZoom(false)
            settings.builtInZoomControls = false
            settings.displayZoomControls = false
            settings.loadsImagesAutomatically = true
            settings.blockNetworkImage = false
            settings.blockNetworkLoads = false
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = true
            settings.cacheMode = WebSettings.LOAD_DEFAULT
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            settings.mediaPlaybackRequiresUserGesture = true
            val sanitizedUa = settings.userAgentString
                .replace("; wv)", ")")
                .replace("; wv;", ";")
            settings.userAgentString = "$sanitizedUa KalendarzReinmar/1.0"
        }
    }
}
