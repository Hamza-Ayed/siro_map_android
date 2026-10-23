/**
 * وظيفة الملف: تعريف نموذج البيانات والحقول الخاصة بـ Geometry.
 */
package com.siro.maps.model

import android.graphics.Color

// ─────────────────────────────────────────────────────────────
// ID Types (Matching Flutter SDK & Google Maps Android SDK)
// ─────────────────────────────────────────────────────────────

data class MarkerId(val value: String)
data class PolylineId(val value: String)
data class PolygonId(val value: String)
data class CircleId(val value: String)

/**
 * An immutable class representing a latitude/longitude bounding box.
 */
data class LatLngBounds(
    val southwest: LatLng,
    val northeast: LatLng
) {
    init {
        require(southwest.latitude <= northeast.latitude) {
            "Southern latitude (${southwest.latitude}) must be <= northern latitude (${northeast.latitude})"
        }
    }

    fun contains(point: LatLng): Boolean {
        val inLat = point.latitude in southwest.latitude..northeast.latitude
        val inLng = if (southwest.longitude <= northeast.longitude) {
            point.longitude in southwest.longitude..northeast.longitude
        } else {
            point.longitude >= southwest.longitude || point.longitude <= northeast.longitude
        }
        return inLat && inLng
    }
}

// ─────────────────────────────────────────────────────────────
// Map Overlay Elements (Matching Flutter SDK)
// ─────────────────────────────────────────────────────────────

data class Marker(
    val markerId: MarkerId,
    val position: LatLng,
    val alpha: Float = 1.0f,
    val anchorX: Float = 0.5f,
    val anchorY: Float = 1.0f,
    val isDraggable: Boolean = false,
    val isFlat: Boolean = false,
    val icon: SiroBitmap = SiroBitmap.defaultMarker,
    val infoWindow: InfoWindow = InfoWindow.NO_TEXT,
    val rotation: Float = 0.0f,
    val isVisible: Boolean = true,
    val zIndex: Float = 0.0f,
    val onTap: (() -> Unit)? = null
)

data class Polyline(
    val polylineId: PolylineId,
    val points: List<LatLng>,
    val color: Int = Color.parseColor("#0071E3"),
    val width: Float = 10.0f,
    val isGeodesic: Boolean = true,
    val isVisible: Boolean = true,
    val zIndex: Float = 0.0f,
    val onTap: (() -> Unit)? = null
)

data class Polygon(
    val polygonId: PolygonId,
    val points: List<LatLng>,
    val holes: List<List<LatLng>> = emptyList(),
    val strokeColor: Int = Color.parseColor("#0071E3"),
    val strokeWidth: Float = 5.0f,
    val fillColor: Int = Color.parseColor("#330071E3"),
    val isVisible: Boolean = true,
    val zIndex: Float = 0.0f,
    val onTap: (() -> Unit)? = null
)

data class Circle(
    val circleId: CircleId,
    val center: LatLng,
    val radiusMeters: Double,
    val strokeColor: Int = Color.parseColor("#22C55E"),
    val strokeWidth: Float = 4.0f,
    val fillColor: Int = Color.parseColor("#3322C55E"),
    val isVisible: Boolean = true,
    val zIndex: Float = 0.0f,
    val onTap: (() -> Unit)? = null
)
