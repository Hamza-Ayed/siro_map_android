/**
 * وظيفة الملف: تنفيذ جزء SiroStyles ضمن حزمة العميل أو التطبيق.
 */
package com.siro.maps.styles

import com.siro.maps.SiroMaps

/**
 * Predefined map visual styles for the Siro Map SDK.
 * Mirrors SiroStyles from Flutter SDK.
 */
object SiroStyles {
    val DARK: String
        get() {
            val keyParam = if (SiroMaps.apiKey.isNotEmpty()) "?key=${SiroMaps.apiKey}" else ""
            return "${SiroMaps.baseUrl}/style-dark.json$keyParam"
        }

    val LIGHT: String
        get() {
            val keyParam = if (SiroMaps.apiKey.isNotEmpty()) "?key=${SiroMaps.apiKey}" else ""
            return "${SiroMaps.baseUrl}/style.json$keyParam"
        }

    val NORMAL: String
        get() {
            val keyParam = if (SiroMaps.apiKey.isNotEmpty()) "?key=${SiroMaps.apiKey}" else ""
            return "${SiroMaps.baseUrl}/style.json$keyParam"
        }

    val SATELLITE: String
        get() {
            val keyParam = if (SiroMaps.apiKey.isNotEmpty()) "?key=${SiroMaps.apiKey}" else ""
            return "${SiroMaps.baseUrl}/style-satellite.json$keyParam"
        }

    val OUTDOORS: String
        get() {
            val keyParam = if (SiroMaps.apiKey.isNotEmpty()) "?key=${SiroMaps.apiKey}" else ""
            return "${SiroMaps.baseUrl}/style-tourism.json$keyParam"
        }

    val TERRAIN: String
        get() {
            val keyParam = if (SiroMaps.apiKey.isNotEmpty()) "?key=${SiroMaps.apiKey}" else ""
            return "${SiroMaps.baseUrl}/style-3d-terrain.json$keyParam"
        }
}

enum class SiroMapType {
    NONE,
    NORMAL,
    LIGHT,
    SATELLITE,
    DARK,
    TERRAIN
}
