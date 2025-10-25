package love.bside.app.data.mappers

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import love.bside.app.data.api.PromptDTO
import love.bside.app.data.api.PromptAnswerDTO
import love.bside.app.domain.models.Prompt
import love.bside.app.domain.models.UserAnswer

/**
 * Mappers for converting between API DTOs and domain models for prompts
 */

/**
 * Convert PromptDTO from API to domain Prompt model
 * Note: API prompt doesn't have matchId, using category as type for now
 */
fun PromptDTO.toDomain(matchId: String = ""): Prompt {
    val now = Clock.System.now()
    return Prompt(
        id = id,
        created = now,
        updated = now,
        matchId = matchId,
        promptText = text,
        promptType = category
    )
}

/**
 * Convert domain Prompt to API PromptDTO
 */
fun Prompt.toDTO(): PromptDTO {
    return PromptDTO(
        id = id,
        text = promptText,
        category = promptType,
        displayOrder = 0
    )
}

/**
 * Convert PromptAnswerDTO from API to domain UserAnswer model
 */
fun PromptAnswerDTO.toDomain(userId: String): UserAnswer {
    val now = Clock.System.now()
    val createdInstant = parseIsoInstant(createdAt) ?: now
    
    return UserAnswer(
        id = id,
        created = createdInstant,
        updated = now,
        userId = userId,
        questionId = prompt.id,
        answerText = answer
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
