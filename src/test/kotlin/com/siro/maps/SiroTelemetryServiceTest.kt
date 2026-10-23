/**
 * وظيفة الملف: اختبارات محرك التتبع في Android، تغطي نبضة الوقوف وبوابة الموافقة ومعرّف التثبيت.
 */
package com.siro.maps

import com.siro.maps.service.SiroTelemetryService
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class SiroTelemetryServiceTest {

    private val service = SiroTelemetryService.instance
    private val t0 = 1_700_000_000_000L

    @Before
    fun setUp() {
        service.setEnabled(false)
        service.setInstallId("test-install")
        service.setEnabled(true)
    }

    @After
    fun tearDown() {
        service.setEnabled(false)
    }

    @Test
    fun stationaryDeviceRecordsHeartbeatEvery30SecondsWithoutGpsEvents() {
        assertTrue(service.recordPosition(32.1, 36.1, 0.0, 0.0, nowMs = t0))
        assertFalse(service.recordPosition(32.1, 36.1, 0.0, 0.0, nowMs = t0 + 5_000))
        assertTrue(service.stationary.value)

        assertFalse(service.recordHeartbeat(t0 + 10_000))
        assertTrue(service.recordHeartbeat(t0 + 31_000))
        assertEquals(2, service.bufferedPointCount)
    }

    @Test
    fun movingDeviceDoesNotProduceHeartbeats() {
        service.recordPosition(32.1, 36.1, 40.0, 90.0, nowMs = t0)
        assertFalse(service.stationary.value)
        assertFalse(service.recordHeartbeat(t0 + 60_000))
        assertEquals(1, service.bufferedPointCount)
    }

    @Test
    fun nothingIsRecordedWhileDisabled() {
        service.setEnabled(false)
        assertFalse(service.isEnabled)
        assertFalse(service.recordPosition(32.1, 36.1, 40.0, 90.0, nowMs = t0))
        assertFalse(service.recordHeartbeat(t0 + 60_000))
        assertEquals(0, service.bufferedPointCount)
    }

    @Test
    fun disablingDropsUnsentPoints() {
        service.recordPosition(32.1, 36.1, 40.0, 90.0, nowMs = t0)
        assertEquals(1, service.bufferedPointCount)
        service.setEnabled(false)
        assertEquals(0, service.bufferedPointCount)
    }

    @Test
    fun generatedInstallIdIsRandomUuidV4() {
        val pattern = Regex("^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$")
        val first = SiroTelemetryService.generateInstallId()
        val second = SiroTelemetryService.generateInstallId()
        assertTrue(pattern.matches(first))
        assertNotEquals(first, second)
    }
}
