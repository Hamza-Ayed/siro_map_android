/**
 * وظيفة الملف: تعريف نموذج البيانات والحقول الخاصة بـ LatLng.
 */
package com.siro.maps.model

import java.io.Serializable
import kotlin.math.*

/**
 * An immutable class representing a pair of latitude and longitude coordinates in degrees.
 * Fully compatible with Google Maps SDK LatLng signature.
 */
data class LatLng(
    val latitude: Double,
    val longitude: Double
) : Serializable {

    init {
        require(latitude in -90.0..90.0) { "Latitude must be between -90.0 and 90.0 degrees" }
        require(longitude in -180.0..180.0) { "Longitude must be between -180.0 and 180.0 degrees" }
    }

    /**
     * Calculates spherical distance in meters to another LatLng
     */
    fun distanceTo(other: LatLng): Double {
        val earthRadiusM = 6371000.0
        val dLat = Math.toRadians(other.latitude - this.latitude)
        val dLon = Math.toRadians(other.longitude - this.longitude)
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(this.latitude)) * cos(Math.toRadians(other.latitude)) *
                sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadiusM * c
    }

    /**
     * Calculates bearing in degrees to another LatLng
     */
    fun bearingTo(other: LatLng): Double {
        val lat1 = Math.toRadians(this.latitude)
        val lon1 = Math.toRadians(this.longitude)
        val lat2 = Math.toRadians(other.latitude)
        val lon2 = Math.toRadians(other.longitude)

        val dLon = lon2 - lon1
        val y = sin(dLon) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)

        val rad = atan2(y, x)
        return (Math.toDegrees(rad) + 360.0) % 360.0
    }
}
