package love.bside.app.data.serializers

import kotlinx.datetime.Instant
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import love.bside.app.core.logDebug

/**
 * Serializer for PocketBase date strings (ISO-8601) to [Instant].
 * 
 * PocketBase returns dates in format: `yyyy-MM-dd HH:mm:ss.SSSZ`
 * This serializer ensures they are correctly parsed to UTC [Instant]
 * and logs the transformation for verification.
 */
object PocketBaseInstantSerializer : KSerializer<Instant> {
    override val descriptor: SerialDescriptor = 
        PrimitiveSerialDescriptor("PocketBaseInstant", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): Instant {
        val rawString = decoder.decodeString()
        // PocketBase sometimes omits the 'Z' or uses space instead of 'T'
        // Standard ISO format for Instant.parse requires 'T' separator and 'Z' timezone
        val isoString = rawString
            .replace(" ", "T")
            .let { if (!it.endsWith("Z")) "${it}Z" else it }
            
        return try {
            val instant = Instant.parse(isoString)
            // logDebug("[Date] Raw: \"$rawString\" -> Epoch: ${instant.toEpochMilliseconds()}")
            instant
        } catch (e: Exception) {
            // logDebug("[Date] Failed to parse: \"$rawString\". Error: ${e.message}")
            throw e
        }
    }

    override fun serialize(encoder: Encoder, value: Instant) {
        val stringValue = value.toString() // Returns ISO-8601
        encoder.encodeString(stringValue)
    }
}
