package love.bside.app.domain.services

import arrow.core.Either
import kotlinx.coroutines.flow.Flow
import love.bside.app.domain.models.LocationModel

sealed class LocationError {
    data object PermissionDenied : LocationError()
    data object GpsDisabled : LocationError()
    data class NetworkError(val message: String) : LocationError()
    data object Unknown : LocationError()
}

interface LocationService {
    suspend fun getCurrentLocation(): Either<LocationError, LocationModel>
    fun observeLocation(): Flow<Either<LocationError, LocationModel>>
}
