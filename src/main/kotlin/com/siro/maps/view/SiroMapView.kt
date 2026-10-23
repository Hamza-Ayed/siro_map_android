/**
 * وظيفة الملف: واجهة عرض الخريطة SiroMapView لنظام Android (Kotlin) تدعم العرض ثنائي وثلاثي الأبعاد (2D & 3D).
 */
package com.siro.maps.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import com.siro.maps.SiroMapController
import com.siro.maps.SiroMaps
import com.siro.maps.camera.CameraPosition
import com.siro.maps.model.LatLng
import com.siro.maps.styles.SiroMapType
import com.siro.maps.styles.SiroStyles
import org.json.JSONObject

interface OnMapReadyCallback {
    fun onMapReady(controller: SiroMapController)
}

/**
 * High-performance Android Map View for Siro Maps.
 * Supports 2D vector tiles, 3D terrain elevation mesh, 3D building extrusions, and camera pitch.
 * Compatible with Google Maps SDK MapView API conventions.
 */
class SiroMapView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    val controller: SiroMapController = SiroMapController()
    private val webView: WebView = WebView(context)
    private var isMapLoaded = false
    private val pendingCallbacks = mutableListOf<(SiroMapController) -> Unit>()

    var is3DEnabled: Boolean = false
        private set

    init {
        setBackgroundColor(Color.parseColor("#101014"))
        setupWebView()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        val params = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        webView.layoutParams = params
        addView(webView)

        val settings: WebSettings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.databaseEnabled = true
        settings.allowFileAccess = true
        settings.loadWithOverviewMode = true
        settings.useWideViewPort = true
        settings.cacheMode = WebSettings.LOAD_DEFAULT

        webView.setBackgroundColor(Color.TRANSPARENT)
        webView.webChromeClient = WebChromeClient()
        webView.addJavascriptInterface(MapBridgeInterface(), "siroBridge")

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
            }
        }

        loadMapHtml()
    }

    private fun loadMapHtml() {
        val initialLat = controller.cameraPosition.target.latitude
        val initialLng = controller.cameraPosition.target.longitude
        val initialZoom = controller.cameraPosition.zoom
        val initialPitch = controller.cameraPosition.tilt
        val initialBearing = controller.cameraPosition.bearing
        val styleUrl = SiroStyles.TERRAIN

        val html = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8" />
                <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
                <title>Siro Maps Android</title>
                <script src="https://unpkg.com/maplibre-gl@4.7.1/dist/maplibre-gl.js"></script>
                <link href="https://unpkg.com/maplibre-gl@4.7.1/dist/maplibre-gl.css" rel="stylesheet" />
                <style>
                    html, body, #map { margin: 0; padding: 0; width: 100%; height: 100%; background: #101014; }
                    .marker-pin {
                        width: 32px;
                        height: 32px;
                        border-radius: 50% 50% 50% 0;
                        background: #0071E3;
                        position: absolute;
                        transform: rotate(-45deg);
                        left: 50%;
                        top: 50%;
                        margin: -16px 0 0 -16px;
                        box-shadow: 0 4px 12px rgba(0,0,0,0.4);
                        border: 2px solid #ffffff;
                    }
                </style>
            </head>
            <body>
                <div id="map"></div>
                <script>
                    var map = new maplibregl.Map({
                        container: 'map',
                        style: '$styleUrl',
                        center: [$initialLng, $initialLat],
                        zoom: $initialZoom,
                        pitch: $initialPitch,
                        bearing: $initialBearing,
                        attributionControl: false
                    });

                    var activeMarkers = {};

                    map.on('load', function() {
                        if (window.siroBridge) {
                            window.siroBridge.onMapReady();
                        }
                    });

                    map.on('click', function(e) {
                        if (window.siroBridge) {
                            window.siroBridge.onMapClick(e.lngLat.lat, e.lngLat.lng);
                        }
                    });

                    map.on('moveend', function() {
                        var c = map.getCenter();
                        if (window.siroBridge) {
                            window.siroBridge.onCameraMove(c.lat, c.lng, map.getZoom(), map.getPitch(), map.getBearing());
                        }
                    });

                    function setMapStyle(styleUrl) {
                        map.setStyle(styleUrl);
                    }

                    function set3D(enabled) {
                        if (enabled) {
                            map.easeTo({ pitch: 65, bearing: -35, duration: 1200 });
                        } else {
                            map.easeTo({ pitch: 0, bearing: 0, duration: 1000 });
                        }
                    }

                    function animateTo(lat, lng, zoom, pitch, bearing) {
                        var opt = { center: [lng, lat], duration: 1200 };
                        if (zoom !== null && zoom !== undefined) opt.zoom = zoom;
                        if (pitch !== null && pitch !== undefined) opt.pitch = pitch;
                        if (bearing !== null && bearing !== undefined) opt.bearing = bearing;
                        map.flyTo(opt);
                    }

                    function addMarker(id, lat, lng, title) {
                        if (activeMarkers[id]) {
                            activeMarkers[id].remove();
                        }
                        var el = document.createElement('div');
                        el.className = 'marker-pin';
                        var m = new maplibregl.Marker({ element: el })
                            .setLngLat([lng, lat])
                            .addTo(map);
                        if (title) {
                            var popup = new maplibregl.Popup({ offset: 25 }).setText(title);
                            m.setPopup(popup);
                        }
                        activeMarkers[id] = m;
                    }

                    function clearMarkers() {
                        for (var k in activeMarkers) {
                            activeMarkers[k].remove();
                        }
                        activeMarkers = {};
                    }
                </script>
            </body>
            </html>
        """.trimIndent()

        webView.loadDataWithBaseURL(SiroMaps.baseUrl, html, "text/html", "UTF-8", null)
    }

    private inner class MapBridgeInterface {
        @JavascriptInterface
        fun onMapReady() {
            post {
                isMapLoaded = true
                for (cb in pendingCallbacks) {
                    cb(controller)
                }
                pendingCallbacks.clear()
            }
        }

        @JavascriptInterface
        fun onMapClick(lat: Double, lng: Double) {
            post {
                val point = LatLng(lat, lng)
                controller.onMapClickListener?.invoke(point)
            }
        }

        @JavascriptInterface
        fun onCameraMove(lat: Double, lng: Double, zoom: Double, pitch: Double, bearing: Double) {
            post {
                try {
                    com.siro.maps.service.SiroTelemetryService.instance.recordPosition(
                        latitude = lat,
                        longitude = lng,
                        speedKmH = 0.0,
                        heading = bearing
                    )
                } catch (t: Throwable) {
                    // Fail silently
                }
                val pos = CameraPosition(
                    target = LatLng(lat, lng),
                    zoom = zoom.toFloat(),
                    tilt = pitch.toFloat(),
                    bearing = bearing.toFloat()
                )
                controller.onCameraMoveListener?.invoke(pos)
            }
        }
    }

    // ── Public API ──────────────────────────────────────────────────────────

    fun getMapAsync(callback: (SiroMapController) -> Unit) {
        if (isMapLoaded) {
            callback(controller)
        } else {
            pendingCallbacks.add(callback)
        }
    }

    fun getMapAsync(callback: OnMapReadyCallback) {
        getMapAsync { callback.onMapReady(it) }
    }

    fun set3DMode(enabled: Boolean) {
        this.is3DEnabled = enabled
        evaluateJs("set3D(${if (enabled) "true" else "false"});")
    }

    fun setStyle(styleUrl: String) {
        evaluateJs("setMapStyle('$styleUrl');")
    }

    fun setMapType(mapType: SiroMapType) {
        when (mapType) {
            SiroMapType.TERRAIN -> setStyle(SiroStyles.TERRAIN)
            SiroMapType.DARK -> setStyle(SiroStyles.DARK)
            SiroMapType.SATELLITE -> setStyle(SiroStyles.SATELLITE)
            SiroMapType.NORMAL, SiroMapType.LIGHT -> setStyle(SiroStyles.NORMAL)
            SiroMapType.NONE -> {}
        }
    }

    fun animateCamera(target: LatLng, zoom: Float? = null, pitch: Float? = null, bearing: Float? = null) {
        val zoomStr = zoom?.toString() ?: "null"
        val pitchStr = pitch?.toString() ?: "null"
        val bearingStr = bearing?.toString() ?: "null"
        evaluateJs("animateTo(${target.latitude}, ${target.longitude}, $zoomStr, $pitchStr, $bearingStr);")
    }

    fun addMarker(id: String, position: LatLng, title: String? = null) {
        val escapedTitle = title?.replace("'", "\\'") ?: ""
        evaluateJs("addMarker('$id', ${position.latitude}, ${position.longitude}, '$escapedTitle');")
    }

    fun clearMarkers() {
        evaluateJs("clearMarkers();")
    }

    private fun evaluateJs(script: String) {
        post {
            webView.evaluateJavascript(script, null)
        }
    }

    fun onDestroy() {
        webView.destroy()
    }
}

/**
 * Backward compatibility typealias matching Google Maps Android SDK.
 */
typealias MapView = SiroMapView


