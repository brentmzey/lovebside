package love.bside.app.domain.utils

import love.bside.app.domain.models.GeoCoordinate
import kotlin.math.*

/**
 * Geolocation utilities for distance calculations and formatting.
 */
object GeoUtils {
    private const val EARTH_RADIUS_KM = 6371.0
    private const val METERS_PER_KM = 1000.0

    /**
     * Calculate distance between two coordinates using Haversine formula.
     * @return Distance in meters
     */
    fun calculateDistance(from: GeoCoordinate, to: GeoCoordinate): Double {
        val lat1Rad = from.lat * PI / 180.0
        val lat2Rad = to.lat * PI / 180.0
        val deltaLat = (to.lat - from.lat) * PI / 180.0
        val deltaLng = (to.lng - from.lng) * PI / 180.0

        val a = sin(deltaLat / 2).pow(2) +
                cos(lat1Rad) * cos(lat2Rad) *
                sin(deltaLng / 2).pow(2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return EARTH_RADIUS_KM * c * METERS_PER_KM
    }

    /**
     * Format distance for human-readable display.
     * Examples: "150 m", "2.5 km", "45 km"
     */
    fun formatDistance(meters: Double): String {
        return when {
            meters < METERS_PER_KM -> "${meters.roundToInt()} m"
            meters < 10 * METERS_PER_KM -> {
                val km = (meters / METERS_PER_KM * 10).roundToInt() / 10.0
                "$km km"
            }
            else -> "${(meters / METERS_PER_KM).roundToInt()} km"
        }
    }

    /**
     * Check if a coordinate is within a given radius of another coordinate.
     */
    fun isWithinRadius(
        center: GeoCoordinate,
        point: GeoCoordinate,
        radiusMeters: Double
    ): Boolean {
        return calculateDistance(center, point) <= radiusMeters
    }
}
