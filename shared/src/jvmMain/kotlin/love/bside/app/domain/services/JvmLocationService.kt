package love.bside.app.domain.services

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import love.bside.app.domain.models.GeoCoordinate
import love.bside.app.domain.models.LocationModel
import love.bside.app.domain.models.LocationSource

/**
 * JVM/Desktop implementation of LocationService using IP geolocation.
 * Falls back to stub location for development.
 */
class JvmLocationService(private val httpClient: HttpClient) : LocationService {
    
    override suspend fun getCurrentLocation(): Either<LocationError, LocationModel> {
        return try {
            val response = httpClient.get("https://ipapi.co/json/")
            val ipLocation: IpApiResponse = response.body()

            LocationModel(
                id = "jvm_${Clock.System.now().toEpochMilliseconds()}",
                coordinate = GeoCoordinate(ipLocation.latitude, ipLocation.longitude),
                _address = "${ipLocation.city}, ${ipLocation.region}, ${ipLocation.country_name}",
                source = LocationSource.IpBased(estimatedPrecision = 5000.0), // ~5km accuracy for IP geolocation
                recordedAt = Clock.System.now()
            ).right()
        } catch (e: Exception) {
            // Fallback to stub location for development
            LocationModel(
                id = "jvm_stub",
                coordinate = GeoCoordinate(37.7749, -122.4194), // San Francisco
                _address = "Development Location",
                source = LocationSource.UserSelected,
                recordedAt = Clock.System.now()
            ).right()
        }
    }

    override fun observeLocation(): Flow<Either<LocationError, LocationModel>> = flow {
        emit(getCurrentLocation())
    }

    @Serializable
    private data class IpApiResponse(
        val latitude: Double,
        val longitude: Double,
        val city: String,
        val region: String,
        val country_name: String
    )
}
