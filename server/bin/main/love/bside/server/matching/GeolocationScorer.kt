package love.bside.server.matching

import love.bside.app.data.api.PocketBaseClient
import love.bside.app.core.Result
import kotlin.math.*

/**
 * Geolocation Proximity Scorer
 * 
 * Calculates a compatibility score based on the physical distance between two users.
 * Uses the Haversine formula for accurate great-circle distance on Earth.
 * 
 * Score = max(0, 1 - distance/maxRadius)
 * - Users at the same location → 1.0
 * - Users at maxRadius apart → 0.0
 * - Users farther than maxRadius → 0.0
 */
class GeolocationScorer(
    private val pocketBase: PocketBaseClient,
    private val maxRadiusKm: Double = 100.0
) : ScoreDimension {
    
    override val name = "geolocation"
    override val defaultWeight = 0.15
    
    override suspend fun score(user1Id: String, user2Id: String): Double {
        val loc1 = getUserLocation(user1Id) ?: return 0.0
        val loc2 = getUserLocation(user2Id) ?: return 0.0
        
        val distanceKm = haversineDistance(loc1.first, loc1.second, loc2.first, loc2.second)
        return max(0.0, 1.0 - distanceKm / maxRadiusKm)
    }
    
    private suspend fun getUserLocation(userId: String): Pair<Double, Double>? {
        val filter = "user = '$userId'"
        return when (val result = pocketBase.getList<ProfileGeoRecord>(
            "s_profiles", filter = filter
        )) {
            is Result.Success -> {
                val profile = result.data.items.firstOrNull()
                if (profile != null && profile.latitude != 0.0 && profile.longitude != 0.0) {
                    Pair(profile.latitude, profile.longitude)
                } else null
            }
            else -> null
        }
    }
    
    /**
     * Haversine formula for great-circle distance between two GPS coordinates.
     * Returns distance in kilometers.
     */
    private fun haversineDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371.0 // Earth's radius in km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) + 
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return R * c
    }
}

@kotlinx.serialization.Serializable
private data class ProfileGeoRecord(
    val id: String,
    val user: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val created: String = "",
    val updated: String = ""
)
