package com.siro.maps

import com.siro.maps.camera.CameraPosition
import com.siro.maps.camera.CameraUpdateFactory
import com.siro.maps.model.LatLng
import com.siro.maps.service.SiroGeocodingService
import com.siro.maps.styles.SiroStyles
import org.junit.Assert.*
import org.junit.Test

class SiroMapsTest {

    @Test
    fun testStylesContainCorrectPaths() {
        assertTrue(SiroStyles.TERRAIN.contains("style-3d-terrain.json"))
        assertTrue(SiroStyles.DARK.contains("style-dark.json"))
        assertTrue(SiroStyles.NORMAL.contains("style.json"))
        assertTrue(SiroStyles.SATELLITE.contains("style-satellite.json"))
    }

    @Test
    fun testLatLngCalculations() {
        val amman = LatLng(31.9539, 35.9106)
        val zarqa = LatLng(32.0608, 36.0941)

        val distance = amman.distanceTo(zarqa)
        assertTrue(distance > 15000.0)
        assertTrue(distance < 30000.0)

        val bearing = amman.bearingTo(zarqa)
        assertTrue(bearing in 30.0..90.0)
    }

    @Test
    fun testGeocodingArrayParsing() {
        val jsonArray = """
        [
            {
                "id": "places_jordan_72778",
                "name": "Amman City Hall",
                "name_ar": "أمانة عمان الكبرى",
                "category": "government",
                "city": "Amman",
                "latitude": 31.94514,
                "longitude": 35.92433,
                "full_address": "Amman City Hall, Ali Bin Abi Taleb Street, Medina, Amman"
            }
        ]
        """.trimIndent()

        val places = SiroGeocodingService.parsePlaces(jsonArray)
        assertEquals(1, places.size)
        val place = places[0]
        assertEquals("places_jordan_72778", place.id)
        assertEquals("Amman City Hall", place.name)
        assertEquals("أمانة عمان الكبرى", place.nameAr)
        assertEquals(31.94514, place.position.latitude, 0.0001)
        assertEquals(35.92433, place.position.longitude, 0.0001)
    }

    @Test
    fun testGeocodingGeoJSONParsing() {
        val geoJson = """
        {
            "type": "FeatureCollection",
            "features": [
                {
                    "id": "place_petra_001",
                    "properties": {
                        "name": "Petra Treasury",
                        "name_ar": "خزنة البتراء",
                        "category": "tourism",
                        "city": "Wadi Musa"
                    },
                    "geometry": {
                        "type": "Point",
                        "coordinates": [35.4444, 30.3222]
                    }
                }
            ]
        }
        """.trimIndent()

        val places = SiroGeocodingService.parsePlaces(geoJson)
        assertEquals(1, places.size)
        val place = places[0]
        assertEquals("Petra Treasury", place.name)
        assertEquals(30.3222, place.position.latitude, 0.0001)
        assertEquals(35.4444, place.position.longitude, 0.0001)
    }

    @Test
    fun testCameraPositionAndController() {
        val controller = SiroMapController()
        assertEquals(31.9539, controller.cameraPosition.target.latitude, 0.0001)

        val target = LatLng(30.3222, 35.4444)
        controller.animateCamera(CameraUpdateFactory.newLatLngZoom(target, 15f))

        assertEquals(30.3222, controller.cameraPosition.target.latitude, 0.0001)
        assertEquals(15f, controller.cameraPosition.zoom, 0.001f)
    }
}
