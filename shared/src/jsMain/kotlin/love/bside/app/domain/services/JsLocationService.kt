package love.bside.app.domain.services

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import kotlinx.coroutines.await
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.Clock
import love.bside.app.domain.models.GeoCoordinate
import love.bside.app.domain.models.LocationModel
import love.bside.app.domain.models.LocationSource
import kotlin.js.Promise

/**
 * JS/Browser implementation of LocationService using Geolocation API.
 */
class JsLocationService : LocationService {

    override suspend fun getCurrentLocation(): Either<LocationError, LocationModel> {
        return try {
            val position = getPosition().await()
            
            LocationModel(
                id = "js_${Clock.System.now().toEpochMilliseconds()}",
                coordinate = GeoCoordinate(
                    position.coords.latitude,
                    position.coords.longitude
                ),
                _address = null,
                source = LocationSource.Gps,
                recordedAt = Clock.System.now()
            ).right()
        } catch (e: dynamic) {
            when (e.code as? Int) {
                1 -> LocationError.PermissionDenied.left()
                2 -> LocationError.NetworkError("Position unavailable").left()
                3 -> LocationError.NetworkError("Timeout").left()
                else -> LocationError.Unknown.left()
            }
        }
    }

    override fun observeLocation(): Flow<Either<LocationError, LocationModel>> = flow {
        emit(getCurrentLocation())
    }

    private fun getPosition(): Promise<dynamic> {
        return Promise { resolve, reject ->
            js("navigator.geolocation.getCurrentPosition")(resolve, reject)
        }
    }
}
