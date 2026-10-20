/**
 * وظيفة الملف: تعريف نموذج البيانات والحقول الخاصة بـ InlqBitmap.
 */
package com.intaleq.maps.model

import android.graphics.Bitmap

/**
 * Defines a bitmap image used for rendering custom markers on the map.
 * Mirrors InlqBitmap / BitmapDescriptor from Flutter SDK and Google Maps Android SDK.
 */
class InlqBitmap private constructor(
    val type: Type,
    val bitmap: Bitmap? = null,
    val assetPath: String? = null,
    val hue: Float = 0.0f
) {
    enum class Type {
        DEFAULT,
        DEFAULT_WITH_HUE,
        ASSET,
        BITMAP
    }

    companion object {
        const val HUE_RED = 0.0f
        const val HUE_ORANGE = 30.0f
        const val HUE_YELLOW = 60.0f
        const val HUE_GREEN = 120.0f
        const val HUE_CYAN = 180.0f
        const val HUE_AZURE = 210.0f
        const val HUE_BLUE = 240.0f
        const val HUE_VIOLET = 270.0f
        const val HUE_MAGENTA = 300.0f
        const val HUE_ROSE = 330.0f

        val defaultMarker: InlqBitmap = InlqBitmap(Type.DEFAULT)

        fun defaultMarkerWithHue(hue: Float): InlqBitmap =
            InlqBitmap(Type.DEFAULT_WITH_HUE, hue = hue)

        fun fromAsset(assetPath: String): InlqBitmap =
            InlqBitmap(Type.ASSET, assetPath = assetPath)

        fun fromBitmap(bitmap: Bitmap): InlqBitmap =
            InlqBitmap(Type.BITMAP, bitmap = bitmap)
    }
}

/**
 * Factory class for creating BitmapDescriptor / InlqBitmap instances.
 * Fully compatible with Google Maps Android SDK BitmapDescriptorFactory.
 */
object BitmapDescriptorFactory {
    const val HUE_RED = 0.0f
    const val HUE_ORANGE = 30.0f
    const val HUE_YELLOW = 60.0f
    const val HUE_GREEN = 120.0f
    const val HUE_CYAN = 180.0f
    const val HUE_AZURE = 210.0f
    const val HUE_BLUE = 240.0f
    const val HUE_VIOLET = 270.0f
    const val HUE_MAGENTA = 300.0f
    const val HUE_ROSE = 330.0f

    fun defaultMarker(): InlqBitmap = InlqBitmap.defaultMarker
    fun defaultMarker(hue: Float): InlqBitmap = InlqBitmap.defaultMarkerWithHue(hue)
    fun fromAsset(assetPath: String): InlqBitmap = InlqBitmap.fromAsset(assetPath)
    fun fromBitmap(bitmap: Bitmap): InlqBitmap = InlqBitmap.fromBitmap(bitmap)
}

/**
 * Backward-compatible typealias matching Google Maps Android SDK.
 */
typealias BitmapDescriptor = InlqBitmap


/**
 * Text information window displayed above a marker when tapped.
 * Mirrors InfoWindow from Flutter SDK.
 */
data class InfoWindow(
    val title: String? = null,
    val snippet: String? = null,
    val anchorX: Float = 0.5f,
    val anchorY: Float = 0.0f,
    val onTap: (() -> Unit)? = null
) {
    companion object {
        val NO_TEXT = InfoWindow()
    }
}
