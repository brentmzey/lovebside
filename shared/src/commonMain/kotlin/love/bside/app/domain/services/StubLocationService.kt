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

class StubLocationService : LocationService {
    // San Francisco
    private val defaultLocation = LocationModel(
        id = "stub_sf",
        coordinate = GeoCoordinate(37.7749, -122.4194),
        _address = "San Francisco, CA",
        source = LocationSource.UserSelected,
        recordedAt = Clock.System.now()
    )

    override suspend fun getCurrentLocation(): Either<LocationError, LocationModel> {
        delay(500) // Simulate network/gps
        return defaultLocation.right()
    }

    override fun observeLocation(): Flow<Either<LocationError, LocationModel>> = flow {
        emit(defaultLocation.right())
        // Simulate movement if needed
        delay(5000)
        emit(defaultLocation.copy(
            coordinate = GeoCoordinate(37.7849, -122.4094), // Move slightly
            _address = "Downtown SF"
        ).right())
    }
}
