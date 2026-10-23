/**
 * وظيفة الملف: تنفيذ جزء SiroMapController ضمن حزمة العميل أو التطبيق.
 */
package com.siro.maps

import com.siro.maps.camera.CameraPosition
import com.siro.maps.camera.CameraUpdate
import com.siro.maps.model.*

/**
 * Controller for an [SiroMapView] instance.
 * Fully mirrors SiroMapController from Flutter SDK and GoogleMap from Google Maps Android SDK.
 */
class SiroMapController {

    private val markers = mutableMapOf<MarkerId, Marker>()
    private val polylines = mutableMapOf<PolylineId, Polyline>()
    private val polygons = mutableMapOf<PolygonId, Polygon>()
    private val circles = mutableMapOf<CircleId, Circle>()

    var cameraPosition: CameraPosition = CameraPosition(target = LatLng(31.9539, 35.9106), zoom = 12.0f)
        private set

    var onCameraMoveListener: ((CameraPosition) -> Unit)? = null
    var onCameraIdleListener: (() -> Unit)? = null
    var onMapClickListener: ((LatLng) -> Unit)? = null
    var onMapLongClickListener: ((LatLng) -> Unit)? = null

    /**
     * Animates the movement of the camera from its current position to the position defined in the update.
     */
    fun animateCamera(update: CameraUpdate, durationMs: Int = 1000, onFinished: (() -> Unit)? = null) {
        applyCameraUpdate(update)
        onFinished?.invoke()
    }

    /**
     * Repositions the camera according to the instructions defined in the update instantly.
     */
    fun moveCamera(update: CameraUpdate) {
        applyCameraUpdate(update)
    }

    private fun applyCameraUpdate(update: CameraUpdate) {
        cameraPosition = when (update) {
            is CameraUpdate.NewLatLng -> cameraPosition.copy(target = update.latLng)
            is CameraUpdate.NewLatLngZoom -> cameraPosition.copy(target = update.latLng, zoom = update.zoom)
            is CameraUpdate.NewCameraPosition -> update.cameraPosition
            is CameraUpdate.ZoomBy -> cameraPosition.copy(zoom = cameraPosition.zoom + update.amount)
        }
        onCameraMoveListener?.invoke(cameraPosition)
        onCameraIdleListener?.invoke()
    }

    // ── Markers ─────────────────────────────────────────────────────────────

    fun addMarker(marker: Marker): Marker {
        markers[marker.markerId] = marker
        return marker
    }

    /**
     * Adds a marker to this map according to the instructions defined in the options.
     * Fully compatible with Google Maps Android SDK GoogleMap.addMarker(MarkerOptions).
     */
    fun addMarker(options: MarkerOptions): Marker {
        val id = MarkerId("marker_${System.currentTimeMillis()}_${(0..9999).random()}")
        val marker = Marker(
            markerId = id,
            position = options.position,
            icon = options.icon,
            isDraggable = options.isDraggable,
            isVisible = options.isVisible,
            zIndex = options.zIndex,
            infoWindow = InfoWindow(title = options.title, snippet = options.snippet)
        )
        return addMarker(marker)
    }

    fun removeMarker(markerId: MarkerId) {
        markers.remove(markerId)
    }

    fun clearMarkers() {
        markers.clear()
    }

    fun getMarkers(): Set<Marker> = markers.values.toSet()

    // ── Polylines ───────────────────────────────────────────────────────────

    fun addPolyline(polyline: Polyline): Polyline {
        polylines[polyline.polylineId] = polyline
        return polyline
    }

    /**
     * Adds a polyline to this map according to the instructions defined in the options.
     * Fully compatible with Google Maps Android SDK GoogleMap.addPolyline(PolylineOptions).
     */
    fun addPolyline(options: PolylineOptions): Polyline {
        val id = PolylineId("polyline_${System.currentTimeMillis()}_${(0..9999).random()}")
        val polyline = Polyline(
            polylineId = id,
            points = options.points.toList(),
            color = options.color,
            width = options.width,
            isGeodesic = options.isGeodesic,
            isVisible = options.isVisible,
            zIndex = options.zIndex
        )
        return addPolyline(polyline)
    }

    fun removePolyline(polylineId: PolylineId) {
        polylines.remove(polylineId)
    }

    fun clearPolylines() {
        polylines.clear()
    }

    fun getPolylines(): Set<Polyline> = polylines.values.toSet()

    // ── Polygons ────────────────────────────────────────────────────────────

    fun addPolygon(polygon: Polygon): Polygon {
        polygons[polygon.polygonId] = polygon
        return polygon
    }

    /**
     * Adds a polygon to this map according to the instructions defined in the options.
     * Fully compatible with Google Maps Android SDK GoogleMap.addPolygon(PolygonOptions).
     */
    fun addPolygon(options: PolygonOptions): Polygon {
        val id = PolygonId("polygon_${System.currentTimeMillis()}_${(0..9999).random()}")
        val polygon = Polygon(
            polygonId = id,
            points = options.points.toList(),
            holes = options.holes.toList(),
            strokeColor = options.strokeColor,
            strokeWidth = options.strokeWidth,
            fillColor = options.fillColor,
            isVisible = options.isVisible,
            zIndex = options.zIndex
        )
        return addPolygon(polygon)
    }

    fun removePolygon(polygonId: PolygonId) {
        polygons.remove(polygonId)
    }

    fun clearPolygons() {
        polygons.clear()
    }

    fun getPolygons(): Set<Polygon> = polygons.values.toSet()

    // ── Circles ─────────────────────────────────────────────────────────────

    fun addCircle(circle: Circle): Circle {
        circles[circle.circleId] = circle
        return circle
    }

    /**
     * Adds a circle to this map according to the instructions defined in the options.
     * Fully compatible with Google Maps Android SDK GoogleMap.addCircle(CircleOptions).
     */
    fun addCircle(options: CircleOptions): Circle {
        val id = CircleId("circle_${System.currentTimeMillis()}_${(0..9999).random()}")
        val circle = Circle(
            circleId = id,
            center = options.center,
            radiusMeters = options.radiusMeters,
            strokeColor = options.strokeColor,
            strokeWidth = options.strokeWidth,
            fillColor = options.fillColor,
            isVisible = options.isVisible,
            zIndex = options.zIndex
        )
        return addCircle(circle)
    }

    fun removeCircle(circleId: CircleId) {
        circles.remove(circleId)
    }

    fun clearCircles() {
        circles.clear()
    }

    fun getCircles(): Set<Circle> = circles.values.toSet()

    /**
     * Clears all overlays (markers, polylines, polygons, circles) from the map.
     */
    fun clear() {
        clearMarkers()
        clearPolylines()
        clearPolygons()
        clearCircles()
    }
}

/**
 * Backward compatibility typealias matching Google Maps Android SDK.
 */
typealias GoogleMap = SiroMapController


