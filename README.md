# Siro Maps SDK for Android (Kotlin)
### خريطة سيرو لنظام أندرويد (كوتلن)

[![JitPack](https://jitpack.io/v/Hamza-Ayed/siro_map_android.svg)](https://jitpack.io/#Hamza-Ayed/siro_map_android)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](https://opensource.org/licenses/MIT)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.23-orange.svg)](https://kotlinlang.org)
[![Android](https://img.shields.io/badge/Android-SDK%2021%2B-green.svg)](https://developer.android.com)

High-performance commercial sovereign map SDK for Android offering high-resolution 2D vector maps, 3D terrain elevation mesh, 3D architectural extrusions, routing, forward/reverse geocoding in Arabic and English. Fully compatible with Google Maps Android SDK conventions.

---

## Installation / التثبيت

### 1. Add Repository
In your `settings.gradle.kts` (or root `build.gradle.kts`):

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

### 2. Add Dependency
In your app module `build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.github.Hamza-Ayed:siro_map_android:v1.0.0")
}
```

---

## Quickstart / دليل البدء السريع

### 1. Initialize SDK in Application or MainActivity

```kotlin
import com.siro.maps.SiroMaps

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Siro Maps with your API key
        SiroMaps.initialize(
            context = this,
            apiKey = "YOUR_API_KEY",
            baseUrl = "https://map-saas.intaleqapp.com"
        )

        setContentView(R.layout.activity_main)
    }
}
```

### 2. Add Map View to Layout XML

```xml
<com.siro.maps.view.SiroMapView
    android:id="@+id/mapView"
    android:layout_width="match_parent"
    android:layout_height="match_parent" />
```

### 3. Control Map and 3D Mode

```kotlin
val mapView = findViewById<SiroMapView>(R.id.mapView)

mapView.getMapAsync { controller ->
    // Toggle 3D Terrain elevation and 65° pitch
    mapView.set3DMode(true)

    // Switch styles (TERRAIN, DARK, SATELLITE, NORMAL)
    mapView.setMapType(SiroMapType.TERRAIN)

    // Animate camera to Amman
    mapView.animateCamera(
        target = LatLng(31.9539, 35.9106),
        zoom = 15.0f,
        pitch = 65.0f,
        bearing = -35.0f
    )

    // Add marker using Google Maps SDK conventions
    mapView.addMarker("pin1", LatLng(31.9539, 35.9106), "وسط البلد عمان")
}
```

### 4. Forward & Reverse Geocoding (البحث والاستعلام العكسي)

```kotlin
val geocodingService = SiroGeocodingService()

lifecycleScope.launch {
    // 1. Search places
    val places = geocodingService.search("البتراء")
    places.firstOrNull()?.let {
        println("Found: ${it.nameAr} at ${it.position}")
    }

    // 2. Reverse Geocode (Coordinate to Arabic Address)
    val address = geocodingService.reverseGeocode(LatLng(31.9539, 35.9106))
    println("Address: ${address?.address}")
}
```

---

## License
MIT License. © 2026 Siro Platform.
