/**
 * وظيفة الملف: تعريف نموذج البيانات والحقول الخاصة بـ OverlayOptions.
 */
package com.siro.maps.model

import android.graphics.Color

/**
 * Defines MarkerOptions for creating and rendering a tactical / commercial marker.
 * Fully compatible with Google Maps SDK MarkerOptions API.
 */
class MarkerOptions {
    var position: LatLng = LatLng(0.0, 0.0)
        private set
    var title: String? = null
        private set
    var snippet: String? = null
        private set
    var iconHue: Float = 210.0f // Azure Blue default
        private set
    var icon: SiroBitmap = SiroBitmap.defaultMarker
        private set
    var isDraggable: Boolean = false
        private set
    var isVisible: Boolean = true
        private set
    var zIndex: Float = 0.0f
        private set

    fun position(latLng: LatLng): MarkerOptions = apply { this.position = latLng }
    fun title(title: String?): MarkerOptions = apply { this.title = title }
    fun snippet(snippet: String?): MarkerOptions = apply { this.snippet = snippet }
    fun iconHue(hue: Float): MarkerOptions = apply { this.iconHue = hue }
    fun icon(descriptor: SiroBitmap): MarkerOptions = apply {
        this.icon = descriptor
        if (descriptor.type == SiroBitmap.Type.DEFAULT_WITH_HUE) {
            this.iconHue = descriptor.hue
        }
    }
    fun draggable(draggable: Boolean): MarkerOptions = apply { this.isDraggable = draggable }
    fun visible(visible: Boolean): MarkerOptions = apply { this.isVisible = visible }
    fun zIndex(zIndex: Float): MarkerOptions = apply { this.zIndex = zIndex }
}

/**
 * Defines PolylineOptions for creating and rendering paths and routes.
 * Fully compatible with Google Maps SDK PolylineOptions API.
 */
class PolylineOptions {
    val points: MutableList<LatLng> = mutableListOf()
    var color: Int = Color.parseColor("#0071E3")
        private set
    var width: Float = 10.0f
        private set
    var isGeodesic: Boolean = true
        private set
    var isVisible: Boolean = true
        private set
    var zIndex: Float = 0.0f
        private set

    fun add(point: LatLng): PolylineOptions = apply { this.points.add(point) }
    fun addAll(points: Iterable<LatLng>): PolylineOptions = apply { this.points.addAll(points) }
    fun color(color: Int): PolylineOptions = apply { this.color = color }
    fun width(width: Float): PolylineOptions = apply { this.width = width }
    fun geodesic(geodesic: Boolean): PolylineOptions = apply { this.isGeodesic = geodesic }
    fun visible(visible: Boolean): PolylineOptions = apply { this.isVisible = visible }
    fun zIndex(zIndex: Float): PolylineOptions = apply { this.zIndex = zIndex }
}

/**
 * Defines PolygonOptions for creating tactical zones and reachability polygons.
 * Fully compatible with Google Maps SDK PolygonOptions API.
 */
class PolygonOptions {
    val points: MutableList<LatLng> = mutableListOf()
    val holes: MutableList<List<LatLng>> = mutableListOf()
    var strokeColor: Int = Color.parseColor("#0071E3")
        private set
    var strokeWidth: Float = 5.0f
        private set
    var fillColor: Int = Color.parseColor("#330071E3")
        private set
    var isVisible: Boolean = true
        private set
    var zIndex: Float = 0.0f
        private set

    fun add(point: LatLng): PolygonOptions = apply { this.points.add(point) }
    fun addAll(points: Iterable<LatLng>): PolygonOptions = apply { this.points.addAll(points) }
    fun addHole(hole: List<LatLng>): PolygonOptions = apply { this.holes.add(hole) }
    fun strokeColor(color: Int): PolygonOptions = apply { this.strokeColor = color }
    fun strokeWidth(width: Float): PolygonOptions = apply { this.strokeWidth = width }
    fun fillColor(color: Int): PolygonOptions = apply { this.fillColor = color }
    fun visible(visible: Boolean): PolygonOptions = apply { this.isVisible = visible }
    fun zIndex(zIndex: Float): PolygonOptions = apply { this.zIndex = zIndex }
}

/**
 * Defines CircleOptions for circular accuracy and buffer zones.
 * Fully compatible with Google Maps SDK CircleOptions API.
 */
class CircleOptions {
    var center: LatLng = LatLng(0.0, 0.0)
        private set
    var radiusMeters: Double = 0.0
        private set
    var strokeColor: Int = Color.parseColor("#22C55E")
        private set
    var strokeWidth: Float = 4.0f
        private set
    var fillColor: Int = Color.parseColor("#3322C55E")
        private set
    var isVisible: Boolean = true
        private set
    var zIndex: Float = 0.0f
        private set

    fun center(center: LatLng): CircleOptions = apply { this.center = center }
    fun radius(radiusMeters: Double): CircleOptions = apply { this.radiusMeters = radiusMeters }
    fun strokeColor(color: Int): CircleOptions = apply { this.strokeColor = color }
    fun strokeWidth(width: Float): CircleOptions = apply { this.strokeWidth = width }
    fun fillColor(color: Int): CircleOptions = apply { this.fillColor = color }
    fun visible(visible: Boolean): CircleOptions = apply { this.isVisible = visible }
    fun zIndex(zIndex: Float): CircleOptions = apply { this.zIndex = zIndex }
}
