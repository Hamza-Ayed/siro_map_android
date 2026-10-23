/**
 * وظيفة الملف: خدمة العميل لتنفيذ عمليات SiroTelemetryService ورفع تتبع المركبات دورياً بالدفعات (Batch Telemetry).
 */
package com.siro.maps.service

import android.content.Context
import com.siro.maps.SiroMaps
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
 * Telemetry & periodic sync engine for the Android SDK.
 * 1. Samples GPS every 3 seconds while moving. While stationary, records one heartbeat every 30 seconds,
 *    driven by an internal timer so it does not depend on new GPS events.
 * 2. Batches and uploads points every 2 minutes, or sooner at 40 points.
 * 3. Keeps up to 400 points in memory while offline.
 * Collection is off until the host app starts tracking with `consentGranted = true`.
 */
class SiroTelemetryService private constructor() {

    companion object {
        val instance: SiroTelemetryService by lazy { SiroTelemetryService() }

        const val SAMPLING_INTERVAL_MS = 3_000L
        const val HEARTBEAT_INTERVAL_MS = 30_000L
        const val DEFAULT_UPLOAD_INTERVAL_SECONDS = 120L // دقيقتان
        const val MAX_BATCH_SIZE = 40 // 40 نقطة * 3 ثوان = 120 ثانية
        const val MAX_BUFFER_SIZE = 400 // تخزين مؤقت حتى 20 دقيقة دون اتصال
        const val STATIONARY_SPEED_KMH = 1.0
        const val STATIONARY_DISTANCE_METERS = 3.0

        private const val PREFS_NAME = "siro_maps_telemetry"
        private const val KEY_INSTALL_ID = "install_id"

        /** يولّد معرّف تثبيت عشوائياً بصيغة UUID v4، لا يُشتق من أي بيانات عن الجهاز أو المستخدم. */
        fun generateInstallId(): String = UUID.randomUUID().toString()

        /**
         * يرجع معرّف التثبيت المحفوظ، أو يولّد واحداً جديداً عند أول تشغيل.
         * يُحفظ في SharedPreferences، فيُمسح مع بيانات التطبيق عند الحذف.
         */
        fun loadOrCreateInstallId(context: Context): String {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.getString(KEY_INSTALL_ID, null)?.let { return it }
            val id = generateInstallId()
            prefs.edit().putString(KEY_INSTALL_ID, id).apply()
            return id
        }
    }

    private var installId: String = ""
    private var enabled: Boolean = false
    private val buffer = Collections.synchronizedList(mutableListOf<TelemetryPoint>())

    private var lastSampleTime: Long = 0L
    private var lastRecordedPoint: TelemetryPoint? = null
    private var lastObservedPoint: TelemetryPoint? = null
    private var trackingJob: Job? = null
    private var heartbeatJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val stationaryState = MutableStateFlow(false)

    /** true عندما يكون الجهاز واقفاً. يُصدر القيمة عند كل تغيّر فقط. */
    val stationary: StateFlow<Boolean> = stationaryState.asStateFlow()

    /** هل التطبيق المضيف فعّل جمع التتبع بعد الموافقة. */
    val isEnabled: Boolean
        get() = enabled

    val bufferedPointCount: Int
        get() = buffer.size

    /**
     * يبدأ جلسة التتبع الدورية، عادةً عند بدء الملاحة.
     * إذا كانت `consentGranted` = false لا يُسجَّل أي شيء.
     */
    fun startTracking(
        installId: String,
        consentGranted: Boolean,
        uploadIntervalSeconds: Long = DEFAULT_UPLOAD_INTERVAL_SECONDS
    ) {
        setInstallId(installId)
        setEnabled(consentGranted)

        trackingJob?.cancel()
        heartbeatJob?.cancel()
        if (!consentGranted) return

        trackingJob = serviceScope.launch {
            while (isActive) {
                delay(uploadIntervalSeconds * 1000L)
                flush()
            }
        }
        heartbeatJob = serviceScope.launch {
            while (isActive) {
                delay(HEARTBEAT_INTERVAL_MS)
                recordHeartbeat()
            }
        }
    }

    /** يوقف المؤقتات ويرفع ما تبقى من النقاط. */
    fun stopTracking() {
        trackingJob?.cancel()
        trackingJob = null
        heartbeatJob?.cancel()
        heartbeatJob = null
        serviceScope.launch {
            flush()
        }
    }

    /**
     * يفعّل أو يوقف الجمع. عند الإيقاف تُحذف النقاط غير المرسلة ولا يُسجَّل شيء بعدها.
     */
    fun setEnabled(value: Boolean) {
        enabled = value
        if (!value) {
            synchronized(buffer) { buffer.clear() }
            lastSampleTime = 0L
            lastRecordedPoint = null
            lastObservedPoint = null
            stationaryState.value = false
        }
    }

    internal fun setInstallId(value: String) {
        installId = value
    }

    /**
     * يسجّل موقعاً جديداً. عند الحركة كل 3 ثوانٍ، وعند الوقوف نبضة كل 30 ثانية.
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
        if (!enabled) return false

        val observed = TelemetryPoint(
            driverId = customDriverId ?: installId,
            latitude = latitude,
            longitude = longitude,
            speed = speedKmH,
            heading = heading,
            elevation = elevation,
            distance = remainingDistance,
            timestamp = nowMs
        )
        lastObservedPoint = observed

        // 1. Throttle: at least 3 seconds since the last recorded sample
        if (lastSampleTime > 0 && (nowMs - lastSampleTime) < SAMPLING_INTERVAL_MS) {
            return false
        }

        // 2. Stationary filter: while stationary, record a point only every 30 seconds
        val last = lastRecordedPoint
        val stationaryNow = isStationaryAt(observed)
        stationaryState.value = stationaryNow
        if (stationaryNow && last != null && (nowMs - last.timestamp) < HEARTBEAT_INTERVAL_MS) {
            return false
        }

        appendPoint(observed, nowMs)
        return true
    }

    /**
     * نبضة ثابتة عند الوقوف: تُسجَّل كل 30 ثانية حتى دون وصول GPS جديد.
     * يستدعيها المؤقت داخلياً أثناء التتبع.
     */
    fun recordHeartbeat(nowMs: Long = System.currentTimeMillis()): Boolean {
        if (!enabled || !stationaryState.value) return false
        val last = lastObservedPoint ?: return false
        val recorded = lastRecordedPoint ?: return false
        if ((nowMs - recorded.timestamp) < HEARTBEAT_INTERVAL_MS) return false

        appendPoint(last.copy(timestamp = nowMs), nowMs)
        return true
    }

    private fun isStationaryAt(observed: TelemetryPoint): Boolean {
        val last = lastRecordedPoint ?: return false
        if (observed.speed >= STATIONARY_SPEED_KMH) return false
        val dist = calculateDistanceMeters(last.latitude, last.longitude, observed.latitude, observed.longitude)
        return dist < STATIONARY_DISTANCE_METERS
    }

    private fun appendPoint(point: TelemetryPoint, nowMs: Long) {
        synchronized(buffer) {
            if (buffer.size >= MAX_BUFFER_SIZE) {
                buffer.removeAt(0)
            }
            buffer.add(point)
        }
        lastSampleTime = nowMs
        lastRecordedPoint = point

        // 3. Proactive batch upload when reaching 40 points
        if (buffer.size >= MAX_BATCH_SIZE) {
            serviceScope.launch {
                flush()
            }
        }
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
            // Silently suppressed: points stay in the buffer for the next attempt
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
