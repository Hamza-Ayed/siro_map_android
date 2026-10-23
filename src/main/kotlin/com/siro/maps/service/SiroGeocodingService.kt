/**
 * وظيفة الملف: خدمة العميل لتنفيذ عمليات SiroGeocodingService والاتصال بواجهات المنصة عند الحاجة.
 */
package com.siro.maps.service

import com.siro.maps.SiroMaps
import com.siro.maps.model.LatLng
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class GeocodingPlace(
    val id: String,
    val name: String,
    val nameAr: String?,
    val nameEn: String?,
    val category: String?,
    val city: String?,
    val neighbourhood: String?,
    val address: String?,
    val position: LatLng
)

/**
 * Commercial Geocoding & Reverse Geocoding Service calling Siro SaaS API.
 */
class SiroGeocodingService {

    /**
     * Search for places, addresses, and landmarks by query string.
     */
    suspend fun search(
        query: String,
        proximity: LatLng? = null,
        limit: Int = 10
    ): List<GeocodingPlace> = withContext(Dispatchers.IO) {
        try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            var urlString = "${SiroMaps.baseUrl}/api/geocoding/search?q=$encodedQuery&limit=$limit"
            if (proximity != null) {
                urlString += "&lat=${proximity.latitude}&lng=${proximity.longitude}"
            }

            val url = URL(urlString)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("x-api-key", SiroMaps.apiKey)
                setRequestProperty("Accept", "application/json")
            }

            if (conn.responseCode == 200) {
                val responseText = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
                return@withContext parsePlaces(responseText)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext emptyList()
    }

    /**
     * Reverse geocode GPS coordinate to get Arabic street name, neighborhood, and city.
     */
    suspend fun reverseGeocode(position: LatLng): GeocodingPlace? = withContext(Dispatchers.IO) {
        try {
            val urlString = "${SiroMaps.baseUrl}/api/geocoding/reverse?lat=${position.latitude}&lng=${position.longitude}"
            val url = URL(urlString)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("x-api-key", SiroMaps.apiKey)
                setRequestProperty("Accept", "application/json")
            }

            if (conn.responseCode == 200) {
                val responseText = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
                val list = parsePlaces(responseText)
                return@withContext list.firstOrNull()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext null
    }

    companion object {
        fun parsePlaces(jsonString: String): List<GeocodingPlace> {
            val places = mutableListOf<GeocodingPlace>()
            val trimmed = jsonString.trim()
            val items = mutableListOf<JSONObject>()

            if (trimmed.startsWith("[")) {
                val array = JSONArray(trimmed)
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i)
                    if (obj != null) items.add(obj)
                }
            } else if (trimmed.startsWith("{")) {
                val root = JSONObject(trimmed)
                if (root.has("features")) {
                    val arr = root.getJSONArray("features")
                    for (i in 0 until arr.length()) {
                        val obj = arr.optJSONObject(i)
                        if (obj != null) items.add(obj)
                    }
                } else if (root.has("results")) {
                    val arr = root.getJSONArray("results")
                    for (i in 0 until arr.length()) {
                        val obj = arr.optJSONObject(i)
                        if (obj != null) items.add(obj)
                    }
                } else {
                    items.add(root)
                }
            }

            for ((index, item) in items.withIndex()) {
                val props = item.optJSONObject("properties") ?: item
                val geom = item.optJSONObject("geometry")

                var lat: Double? = null
                var lng: Double? = null

                if (props.has("latitude") && props.has("longitude")) {
                    lat = props.optDouble("latitude")
                    lng = props.optDouble("longitude")
                } else if (props.has("location")) {
                    val loc = props.optJSONObject("location")
                    if (loc != null) {
                        lat = loc.optDouble("lat")
                        lng = loc.optDouble("lng")
                    }
                } else if (geom != null && geom.has("coordinates")) {
                    val coords = geom.optJSONArray("coordinates")
                    if (coords != null && coords.length() >= 2) {
                        lat = coords.optDouble(1)
                        lng = coords.optDouble(0)
                    }
                }

                if (lat != null && lng != null && !lat.isNaN() && !lng.isNaN()) {
                    val idStr = item.optString("id", props.optString("id", index.toString()))
                    val nameStr = when {
                        props.has("name") && !props.isNull("name") -> props.getString("name")
                        props.has("name_ar") && !props.isNull("name_ar") -> props.getString("name_ar")
                        props.has("human_readable_address") && !props.isNull("human_readable_address") -> props.getString("human_readable_address")
                        props.has("full_address") && !props.isNull("full_address") -> props.getString("full_address")
                        else -> "موقع محدد"
                    }

                    val nameAr = if (props.has("name_ar") && !props.isNull("name_ar")) props.getString("name_ar") else null
                    val nameEn = if (props.has("name_en") && !props.isNull("name_en")) props.getString("name_en") else null
                    val category = if (props.has("category") && !props.isNull("category")) props.getString("category") else null
                    val city = if (props.has("city") && !props.isNull("city")) props.getString("city") else if (props.has("governorate") && !props.isNull("governorate")) props.getString("governorate") else null
                    val neighbourhood = if (props.has("neighbourhood") && !props.isNull("neighbourhood")) props.getString("neighbourhood") else if (props.has("district") && !props.isNull("district")) props.getString("district") else null
                    val address = if (props.has("full_address") && !props.isNull("full_address")) props.getString("full_address") else if (props.has("address") && !props.isNull("address")) props.getString("address") else null

                    places.add(
                        GeocodingPlace(
                            id = idStr,
                            name = nameStr,
                            nameAr = nameAr,
                            nameEn = nameEn,
                            category = category,
                            city = city,
                            neighbourhood = neighbourhood,
                            address = address,
                            position = LatLng(lat, lng)
                        )
                    )
                }
            }
            return places
        }
    }
}
