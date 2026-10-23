/**
 * وظيفة الملف: خدمة العميل لتنفيذ عمليات SiroRoutingService والاتصال بواجهات المنصة عند الحاجة.
 */
package com.siro.maps.service

import com.siro.maps.SiroMaps
import com.siro.maps.model.LatLng
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class RouteStep(
    val instructionAr: String,
    val distanceMeters: Double,
    val durationSeconds: Double,
    val maneuverSign: Int,
    val inclinePercentage: Double = 0.0
)

data class CommercialRoute(
    val coordinates: List<LatLng>,
    val steps: List<RouteStep>,
    val totalDistanceMeters: Double,
    val totalDurationSeconds: Double,
    val elevationGainMeters: Double = 0.0,
    val elevationLossMeters: Double = 0.0,
    val averageInclinePercentage: Double = 0.0,
    val maxInclinePercentage: Double = 0.0
)

/**
 * Commercial Routing Service calling Siro SaaS Routing Engine.
 */
class SiroRoutingService {

    suspend fun getRoute(
        origin: LatLng,
        destination: LatLng,
        profile: String = "car",
        locale: String = "ar"
    ): CommercialRoute? = withContext(Dispatchers.IO) {
        try {
            val urlString = "${SiroMaps.baseUrl}/api/maps/route?" +
                    "fromLat=${origin.latitude}&fromLng=${origin.longitude}&" +
                    "toLat=${destination.latitude}&toLng=${destination.longitude}&" +
                    "profile=$profile&steps=true&locale=$locale"

            val url = URL(urlString)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10000
                readTimeout = 10000
                setRequestProperty("x-api-key", SiroMaps.apiKey)
                setRequestProperty("Accept", "application/json")
            }

            if (conn.responseCode == 200) {
                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val responseText = reader.use { it.readText() }
                return@withContext parseRouteResponse(responseText)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext null
    }

    private fun parseRouteResponse(jsonString: String): CommercialRoute {
        val root = JSONObject(jsonString)
        val paths = root.getJSONArray("paths")
        if (paths.length() == 0) throw IllegalStateException("No path returned")

        val path = paths.getJSONObject(0)
        val distance = path.optDouble("distance", 0.0)
        val timeMs = path.optDouble("time", 0.0)

        // Coordinates
        val coords = mutableListOf<LatLng>()
        val pointsObj = path.optJSONObject("points")
        if (pointsObj != null && pointsObj.has("coordinates")) {
            val arr = pointsObj.getJSONArray("coordinates")
            for (i in 0 until arr.length()) {
                val c = arr.getJSONArray(i)
                coords.add(LatLng(c.getDouble(1), c.getDouble(0)))
            }
        }

        // Steps
        val steps = mutableListOf<RouteStep>()
        val instrs = path.optJSONArray("instructions")
        if (instrs != null) {
            for (i in 0 until instrs.length()) {
                val inst = instrs.getJSONObject(i)
                steps.add(
                    RouteStep(
                        instructionAr = inst.optString("text", ""),
                        distanceMeters = inst.optDouble("distance", 0.0),
                        durationSeconds = inst.optDouble("time", 0.0) / 1000.0,
                        maneuverSign = inst.optInt("sign", 0)
                    )
                )
            }
        }

        return CommercialRoute(
            coordinates = coords,
            steps = steps,
            totalDistanceMeters = distance,
            totalDurationSeconds = timeMs / 1000.0
        )
    }
}
