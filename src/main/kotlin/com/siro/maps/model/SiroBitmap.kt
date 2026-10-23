/**
 * وظيفة الملف: تعريف نموذج البيانات والحقول الخاصة بـ SiroBitmap.
 */
package com.siro.maps.model

import android.graphics.Bitmap

/**
 * Defines a bitmap image used for rendering custom markers on the map.
 * Mirrors SiroBitmap / BitmapDescriptor from Flutter SDK and Google Maps Android SDK.
 */
class SiroBitmap private constructor(
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

        val defaultMarker: SiroBitmap = SiroBitmap(Type.DEFAULT)

        fun defaultMarkerWithHue(hue: Float): SiroBitmap =
            SiroBitmap(Type.DEFAULT_WITH_HUE, hue = hue)

        fun fromAsset(assetPath: String): SiroBitmap =
            SiroBitmap(Type.ASSET, assetPath = assetPath)

        fun fromBitmap(bitmap: Bitmap): SiroBitmap =
            SiroBitmap(Type.BITMAP, bitmap = bitmap)
    }
}

/**
 * Factory class for creating BitmapDescriptor / SiroBitmap instances.
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

    fun defaultMarker(): SiroBitmap = SiroBitmap.defaultMarker
    fun defaultMarker(hue: Float): SiroBitmap = SiroBitmap.defaultMarkerWithHue(hue)
    fun fromAsset(assetPath: String): SiroBitmap = SiroBitmap.fromAsset(assetPath)
    fun fromBitmap(bitmap: Bitmap): SiroBitmap = SiroBitmap.fromBitmap(bitmap)
}

/**
 * Backward-compatible typealias matching Google Maps Android SDK.
 */
typealias BitmapDescriptor = SiroBitmap


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
