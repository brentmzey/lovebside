package love.bside.app.utils

import arrow.core.Option
import arrow.core.none
import arrow.core.some
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Custom serializer for Arrow Option<String>
 */
@OptIn(ExperimentalSerializationApi::class)
object OptionStringSerializer : KSerializer<Option<String>> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("OptionString", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Option<String>) {
        value.fold(
            { encoder.encodeNull() },
            { encoder.encodeString(it) }
        )
    }

    override fun deserialize(decoder: Decoder): Option<String> {
        return try {
            decoder.decodeString().some()
        } catch (e: Exception) {
            none()
        }
    }
}
