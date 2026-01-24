package love.bside.app.domain.services

import arrow.core.Either
import arrow.core.right
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.Clock
import love.bside.app.domain.models.GeoCoordinate
import love.bside.app.domain.models.LocationModel
import love.bside.app.domain.models.LocationSource

/**
 * iOS implementation of LocationService.
 * TODO: Implement Core Location integration with proper delegate pattern
 * For now, using stub implementation for development
 */
class IosLocationService : LocationService {
    // San Francisco - stub location for development
    private val defaultLocation = LocationModel(
        id = "ios_stub",
        coordinate = GeoCoordinate(37.7749, -122.4194),
        _address = "San Francisco, CA",
        source = LocationSource.UserSelected,
        recordedAt = Clock.System.now()
    )

    override suspend fun getCurrentLocation(): Either<LocationError, LocationModel> {
        delay(500) // Simulate location fetch
        return defaultLocation.right()
    }

    override fun observeLocation(): Flow<Either<LocationError, LocationModel>> = flow {
        emit(getCurrentLocation())
    }
}
