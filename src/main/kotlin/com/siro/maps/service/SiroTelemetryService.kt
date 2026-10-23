/**
 * وظيفة الملف: خدمة العميل لتنفيذ عمليات SiroTelemetryService ورفع تتبع المركبات دورياً بالدفعات (Batch Telemetry).
 */
package com.siro.maps.service

import com.siro.maps.SiroMaps
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*

data class TelemetryPoint(
    val driverId: String,
    val latitude: Double,
    val longitude: Double,
    val speed: Double,
    val heading: Double,
    val elevation: Double = 0.0,
    val distance: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toJsonObject(): JSONObject {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        return JSONObject().apply {
            put("driver_id", driverId)
            put("latitude", latitude)
            put("longitude", longitude)
            put("speed", if (speed < 0) 0.0 else speed)
            put("heading", if (heading < 0) 0.0 else heading)
            put("elevation", elevation)
            put("distance", distance)
            put("timestamp", sdf.format(Date(timestamp)))
        }
    }
}

/**
 * Commercial Telemetry & Periodic Sync Engine for Android Kotlin SDK.
 * 1. Samples GPS position every 3 seconds with smart stationary filtering.
 * 2. Batches and flushes points to MapSaaS backend every 2 minutes (or 40 points).
 * 3. Keeps up to 400 points offline buffer when disconnected.
 */
class SiroTelemetryService private constructor() {

    companion object {
        val instance: SiroTelemetryService by lazy { SiroTelemetryService() }

        const val SAMPLING_INTERVAL_SECONDS = 3
        const val DEFAULT_UPLOAD_INTERVAL_SECONDS = 120L // دقيقتان
        const val MAX_BATCH_SIZE = 40 // 40 نقطة * 3 ثوان = 120 ثانية
        const val MAX_BUFFER_SIZE = 400 // تخزين مؤقت حتى 20 دقيقة دون اتصال
    }

    private var defaultDriverId: String = "anonymous_driver"
    private val buffer = Collections.synchronizedList(mutableListOf<TelemetryPoint>())

    private var lastSampleTime: Long = 0L
    private var lastRecordedPoint: TelemetryPoint? = null
    private var trackingJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    val bufferedPointCount: Int
        get() = buffer.size

    /**
     * Start periodic tracking session (typically called upon navigation start).
     */
    fun startTracking(driverId: String, uploadIntervalSeconds: Long = DEFAULT_UPLOAD_INTERVAL_SECONDS) {
        this.defaultDriverId = driverId
        trackingJob?.cancel()

        trackingJob = serviceScope.launch {
            while (isActive) {
                delay(uploadIntervalSeconds * 1000L)
                flush()
            }
        }
    }

    /**
     * Stop tracking and flush any remaining points.
     */
    fun stopTracking() {
        trackingJob?.cancel()
        trackingJob = null
        serviceScope.launch {
            flush()
        }
    }

    /**
     * Records a new GPS position snapshot every 3 seconds.
     * Applies stationary filter to skip redundant points when vehicle is parked (<1 km/h).
     */
    fun recordPosition(
        latitude: Double,
        longitude: Double,
        speedKmH: Double,
        heading: Double,
        elevation: Double = 0.0,
        remainingDistance: Double = 0.0,
        customDriverId: String? = null,
        nowMs: Long = System.currentTimeMillis()
    ): Boolean {
        // 1. Throttle: Must be at least 3 seconds since last sample
        if (lastSampleTime > 0 && (nowMs - lastSampleTime) < (SAMPLING_INTERVAL_SECONDS * 1000L)) {
            return false
        }

        // 2. Smart stationary filter:
        // If speed < 1 km/h and moved < 3 meters, only record 1 heartbeat every 30 seconds
        val last = lastRecordedPoint
        if (last != null) {
            val isNearZeroSpeed = speedKmH < 1.0
            val dist = calculateDistanceMeters(last.latitude, last.longitude, latitude, longitude)
            if (isNearZeroSpeed && dist < 3.0) {
                if ((nowMs - last.timestamp) < 30_000L) {
                    return false
                }
            }
        }

        val point = TelemetryPoint(
            driverId = customDriverId ?: defaultDriverId,
            latitude = latitude,
            longitude = longitude,
            speed = speedKmH,
            heading = heading,
            elevation = elevation,
            distance = remainingDistance,
            timestamp = nowMs
        )

        synchronized(buffer) {
            if (buffer.size >= MAX_BUFFER_SIZE) {
                buffer.removeAt(0)
            }
            buffer.add(point)
        }

        lastSampleTime = nowMs
        lastRecordedPoint = point

        // 3. Proactive batch flush when reaching 40 points
        if (buffer.size >= MAX_BATCH_SIZE) {
            serviceScope.launch {
                flush()
            }
        }

        return true
    }

    /**
     * Flushes buffered telemetry points to MapSaaS backend in a single bulk request (POST /api/telemetry/batch).
     */
    suspend fun flush(): Boolean = withContext(Dispatchers.IO) {
        val pointsToSend: List<TelemetryPoint>
        synchronized(buffer) {
            if (buffer.isEmpty()) return@withContext true
            pointsToSend = ArrayList(buffer)
        }

        try {
            val jsonArray = JSONArray()
            for (p in pointsToSend) {
                jsonArray.put(p.toJsonObject())
            }

            val payload = JSONObject().apply {
                put("points", jsonArray)
            }

            val url = URL("${SiroMaps.baseUrl}/api/telemetry/batch")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15000
                readTimeout = 15000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "application/json")
                if (SiroMaps.apiKey.isNotEmpty()) {
                    setRequestProperty("x-api-key", SiroMaps.apiKey)
                }
            }

            OutputStreamWriter(conn.outputStream).use { writer ->
                writer.write(payload.toString())
                writer.flush()
            }

            val code = conn.responseCode
            if (code in 200..299) {
                synchronized(buffer) {
                    buffer.removeAll(pointsToSend)
                }
                return@withContext true
            }
        } catch (e: Throwable) {
            // Silently suppress for background fleet telemetry
        }

        return@withContext false
    }

    /**
     * Instant single telemetry point upload for real-time dispatching.
     */
    suspend fun sendInstantTelemetry(point: TelemetryPoint): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL("${SiroMaps.baseUrl}/api/telemetry")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 8000
                readTimeout = 8000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "application/json")
                if (SiroMaps.apiKey.isNotEmpty()) {
                    setRequestProperty("x-api-key", SiroMaps.apiKey)
                }
            }

            OutputStreamWriter(conn.outputStream).use { writer ->
                writer.write(point.toJsonObject().toString())
                writer.flush()
            }

            return@withContext conn.responseCode in 200..299
        } catch (e: Throwable) {
            // Silently suppress
            return@withContext false
        }
    }

    private fun calculateDistanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371000.0
        val phi1 = Math.toRadians(lat1)
        val phi2 = Math.toRadians(lat2)
        val deltaPhi = Math.toRadians(lat2 - lat1)
        val deltaLambda = Math.toRadians(lon2 - lon1)

        val a = sin(deltaPhi / 2.0).pow(2.0) + cos(phi1) * cos(phi2) * sin(deltaLambda / 2.0).pow(2.0)
        val c = 2.0 * atan2(sqrt(a), sqrt(1.0 - a))
        return r * c
    }
}
