/**
 * وظيفة الملف: تنفيذ جزء CameraPosition ضمن حزمة العميل أو التطبيق.
 */
package com.siro.maps.camera

import com.siro.maps.model.LatLng
import java.io.Serializable

/**
 * An immutable class representing the camera position in terms of target, zoom, tilt, and bearing.
 * Fully compatible with Google Maps SDK CameraPosition API.
 */
data class CameraPosition(
    val target: LatLng,
    val zoom: Float = 12.0f,
    val tilt: Float = 0.0f,
    val bearing: Float = 0.0f
) : Serializable {

    class Builder {
        private var target: LatLng = LatLng(0.0, 0.0)
        private var zoom: Float = 12.0f
        private var tilt: Float = 0.0f
        private var bearing: Float = 0.0f

        fun target(target: LatLng): Builder = apply { this.target = target }
        fun zoom(zoom: Float): Builder = apply { this.zoom = zoom }
        fun tilt(tilt: Float): Builder = apply { this.tilt = tilt }
        fun bearing(bearing: Float): Builder = apply { this.bearing = bearing }

        fun build(): CameraPosition = CameraPosition(target, zoom, tilt, bearing)
    }

    companion object {
        fun builder(): Builder = Builder()
        fun fromLatLngZoom(target: LatLng, zoom: Float): CameraPosition =
            CameraPosition(target = target, zoom = zoom)
    }
}

/**
 * CameraUpdate encapsulates instructions for changing the map camera viewport.
 */
sealed class CameraUpdate {
    data class NewLatLng(val latLng: LatLng) : CameraUpdate()
    data class NewLatLngZoom(val latLng: LatLng, val zoom: Float) : CameraUpdate()
    data class NewCameraPosition(val cameraPosition: CameraPosition) : CameraUpdate()
    data class ZoomBy(val amount: Float) : CameraUpdate()
}

/**
 * Factory class for creating CameraUpdate instances.
 * Fully compatible with Google Maps SDK CameraUpdateFactory API.
 */
object CameraUpdateFactory {
    fun newLatLng(latLng: LatLng): CameraUpdate = CameraUpdate.NewLatLng(latLng)
    fun newLatLngZoom(latLng: LatLng, zoom: Float): CameraUpdate = CameraUpdate.NewLatLngZoom(latLng, zoom)
    fun newCameraPosition(cameraPosition: CameraPosition): CameraUpdate = CameraUpdate.NewCameraPosition(cameraPosition)
    fun zoomIn(): CameraUpdate = CameraUpdate.ZoomBy(1.0f)
    fun zoomOut(): CameraUpdate = CameraUpdate.ZoomBy(-1.0f)
}
