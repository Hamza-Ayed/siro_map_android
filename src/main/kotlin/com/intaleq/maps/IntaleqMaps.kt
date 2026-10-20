/**
 * وظيفة الملف: واجهة الدخول أو نقطة تشغيل حزمة IntaleqMaps.
 */
package com.intaleq.maps

import android.content.Context
import com.intaleq.maps.service.IntaleqTelemetryService
import java.util.UUID

/**
 * Main entry point for Intaleq Commercial Maps SDK for Android (Kotlin).
 * Compatible with Google Maps SDK structure.
 */
object IntaleqMaps {

    var apiKey: String = ""
        private set

    var baseUrl: String = "https://map-saas.intaleqapp.com"
        private set

    var isInitialized: Boolean = false
        private set

    /**
     * Initializes the Intaleq Maps SDK with your SaaS API Key.
     */
    fun initialize(context: Context, apiKey: String, baseUrl: String = "https://map-saas.intaleqapp.com") {
        this.apiKey = apiKey
        this.baseUrl = baseUrl.trimEnd('/')
        this.isInitialized = true

        try {
            val deviceId = try {
                android.provider.Settings.Secure.getString(
                    context.contentResolver,
                    android.provider.Settings.Secure.ANDROID_ID
                ) ?: UUID.randomUUID().toString()
            } catch (t: Throwable) {
                UUID.randomUUID().toString()
            }
            IntaleqTelemetryService.instance.startTracking(deviceId)
        } catch (t: Throwable) {
            // Fail silently
        }
    }
}

/**
 * Primary Siro brand aliases.
 */
typealias SiroMap = IntaleqMaps
typealias SiroMaps = IntaleqMaps

