package love.bside.app.core

import com.benasher44.uuid.Uuid
import com.benasher44.uuid.uuid4
import com.benasher44.uuid.uuidFrom
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Type alias for validated UUID strings used as identifiers across the app.
 * 
 * Uses benasher44/uuid library for Kotlin Multiplatform UUID support.
 * Currently using UUID v4 (random) - may contribute to library for v1 support.
 * 
 * Benefits:
 * - Cross-platform: Works on Android, iOS, Desktop, Web
 * - Type-safe: Compile-time validation of UUID usage
 * - Serializable: Automatically converts to/from JSON strings
 * - Validated: Runtime validation of UUID format
 */
typealias EntityId = @Serializable(with = UuidSerializer::class) Uuid

/**
 * UUID utilities for Kotlin Multiplatform.
 * 
 * This object provides helper functions for generating and validating UUIDs
 * across all platforms (Android, iOS, Desktop, Web via KMP).
 */
object UuidUtils {
    
    /**
     * Generate a new random UUID v4.
     * 
     * Example:
     * ```kotlin
     * val messageId = UuidUtils.random()
     * val conversationId = UuidUtils.random()
     * ```
     */
    fun random(): Uuid = uuid4()
    
    /**
     * Generate a new random UUID v4 and return as string.
     * 
     * Useful for database operations that require string IDs.
     */
    fun randomString(): String = uuid4().toString()
    
    /**
     * Parse and validate a UUID string.
     * 
     * @param value The UUID string to parse
     * @return Result containing Uuid on success, or exception on failure
     * 
     * Example:
     * ```kotlin
     * val result = UuidUtils.parse("550e8400-e29b-41d4-a716-446655440000")
     * result.getOrNull()?.let { uuid ->
     *     // Valid UUID
     * }
     * ```
     */
    fun parse(value: String): kotlin.Result<Uuid> = runCatching {
        uuidFrom(value)
    }
    
    /**
     * Validate if a string is a valid UUID.
     * 
     * @param value The string to validate
     * @return true if valid UUID, false otherwise
     * 
     * Example:
     * ```kotlin
     * if (UuidUtils.isValid(userId)) {
     *     // Process valid UUID
     * }
     * ```
     */
    fun isValid(value: String): Boolean = 
        parse(value).isSuccess
    
    /**
     * Parse UUID string or return null if invalid.
     * 
     * Convenience method for null-safe parsing.
     */
    fun parseOrNull(value: String): Uuid? = 
        parse(value).getOrNull()
    
    /**
     * Generate a nil UUID (all zeros).
     * 
     * Useful as a placeholder or default value.
     */
    fun nil(): Uuid = uuidFrom("00000000-0000-0000-0000-000000000000")
    
    /**
     * Check if UUID is nil (all zeros).
     */
    fun isNil(uuid: Uuid): Boolean = 
        uuid.toString() == "00000000-0000-0000-0000-000000000000"
}

/**
 * Kotlinx.serialization serializer for UUID.
 * 
 * Automatically serializes UUID to/from JSON strings.
 * This is used by the EntityId typealias for seamless JSON conversion.
 */
object UuidSerializer : KSerializer<Uuid> {
    override val descriptor: SerialDescriptor = 
        PrimitiveSerialDescriptor("Uuid", PrimitiveKind.STRING)
    
    override fun serialize(encoder: Encoder, value: Uuid) {
        encoder.encodeString(value.toString())
    }
    
    override fun deserialize(decoder: Decoder): Uuid {
        val string = decoder.decodeString()
        return try {
            uuidFrom(string)
        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid UUID string: $string", e)
        }
    }
}

/**
 * Extension functions for UUID.
 */

/**
 * Convert UUID to string (explicit for clarity).
 */
fun Uuid.toIdString(): String = toString()

/**
 * Check if this UUID is nil (all zeros).
 */
fun Uuid.isNil(): Boolean = UuidUtils.isNil(this)

/**
 * Compare UUIDs for equality (explicit for clarity in business logic).
 */
infix fun Uuid.matches(other: Uuid): Boolean = this == other

/**
 * String extension to safely parse as UUID.
 */
fun String.toUuidOrNull(): Uuid? = UuidUtils.parseOrNull(this)

/**
 * String extension to parse as UUID or throw.
 */
fun String.toUuid(): Uuid = UuidUtils.parse(this).getOrThrow()

/**
 * Validate string as UUID.
 */
fun String.isValidUuid(): Boolean = UuidUtils.isValid(this)
