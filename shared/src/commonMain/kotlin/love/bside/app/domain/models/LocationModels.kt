package love.bside.app.domain.models

import arrow.core.Option
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

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


