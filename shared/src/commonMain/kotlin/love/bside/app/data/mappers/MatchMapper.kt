package love.bside.app.data.mappers

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import love.bside.app.data.api.MatchDTO
import love.bside.app.domain.models.Match

/**
 * Mappers for converting between API DTOs and domain models for matches
 */

/**
 * Convert MatchDTO from API to domain Match model
 */
fun MatchDTO.toDomain(currentUserId: String): Match {
    val now = Clock.System.now()
    val createdInstant = parseIsoInstant(createdAt) ?: now
    
    return Match(
        id = id,
        created = createdInstant,
        updated = now,
        userOneId = currentUserId,
        userTwoId = user.id,
        matchScore = compatibilityScore,
        matchStatus = status,
        generatedAt = createdInstant
    )
}

/**
 * Parse ISO 8601 timestamp string to Instant
 * Returns null if parsing fails
 */
private fun parseIsoInstant(timestamp: String): Instant? {
    return try {
        Instant.parse(timestamp)
    } catch (e: Exception) {
        null
    }
}

/**
 * Match action types
 */
enum class MatchAction(val value: String) {
    LIKE("LIKE"),
    PASS("PASS");
    
    companion object {
        fun fromString(value: String): MatchAction? {
            return values().find { it.value == value.uppercase() }
        }
    }
}
