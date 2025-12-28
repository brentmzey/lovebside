package love.bside.app.domain.models

import arrow.core.Option
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class GeoCoordinate(val lat: Double, val lng: Double)

sealed interface LocationSource {
    @Serializable
    data object Gps : LocationSource
    
    @Serializable
    data object UserSelected : LocationSource
    
    @Serializable
    data class IpBased(val estimatedPrecision: Double) : LocationSource
}

enum class MapType {
    NORMAL, SATELLITE, HYBRID, TERRAIN
}

@Serializable
data class LocationModel(
    val id: String,
    val coordinate: GeoCoordinate,
    private val _address: String?,
    val source: LocationSource,
    val recordedAt: Instant
) {
    val address: Option<String> get() = Option.fromNullable(_address)
}


