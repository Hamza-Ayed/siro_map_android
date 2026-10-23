/**
 * وظيفة الملف: واجهة الدخول أو نقطة تشغيل حزمة SiroMaps.
 */
package com.siro.maps

import android.content.Context
import com.siro.maps.service.SiroTelemetryService
import java.util.UUID

/**
 * Main entry point for Siro Commercial Maps SDK for Android (Kotlin).
 * Compatible with Google Maps SDK structure.
 */
object SiroMaps {

    var apiKey: String = ""
        private set

    var baseUrl: String = "https://map-saas.intaleqapp.com"
        private set

    var isInitialized: Boolean = false
        private set

    /**
     * Initializes the Siro Maps SDK with your SaaS API Key.
     */
    fun initialize(context: Context, apiKey: String, baseUrl: String = "https://map-saas.intaleqapp.com") {
        this.apiKey = apiKey
        this.baseUrl = baseUrl.trimEnd('/')
        this.isInitialized = true
    }
}

