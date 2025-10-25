package love.bside.app.data.mappers

import kotlinx.datetime.Clock
import love.bside.app.data.api.KeyValueDTO
import love.bside.app.data.api.UserValueDTO
import love.bside.app.data.api.UserValueInput
import love.bside.app.domain.models.KeyValue
import love.bside.app.domain.models.UserValue

/**
 * Mappers for converting between API DTOs and domain models for values
 */

/**
 * Convert KeyValueDTO from API to domain KeyValue model
 */
fun KeyValueDTO.toDomain(): KeyValue {
    val now = Clock.System.now()
    return KeyValue(
        id = id,
        created = now,
        updated = now,
        key = key,
        valueText = key, // API uses key as the display text
        category = category,
        description = description,
        displayOrder = displayOrder
    )
}

/**
 * Convert domain KeyValue to API KeyValueDTO
 */
fun KeyValue.toDTO(): KeyValueDTO {
    return KeyValueDTO(
        id = id,
        key = key,
        category = category,
        description = description,
        displayOrder = displayOrder
    )
}

/**
 * Convert UserValueDTO from API to domain UserValue model
 * Note: API includes the full KeyValue, but domain UserValue only stores IDs
 */
fun UserValueDTO.toDomain(userId: String): UserValue {
    val now = Clock.System.now()
    return UserValue(
        id = id,
        created = now,
        updated = now,
        userId = userId,
        valueId = keyValue.id
    )
}

/**
 * Convert domain UserValue to UserValueInput for API requests
 */
fun UserValue.toInput(importance: Int): UserValueInput {
    return UserValueInput(
        keyValueId = valueId,
        importance = importance
    )
}

/**
 * Helper to create UserValueInput directly
 */
fun createUserValueInput(keyValueId: String, importance: Int): UserValueInput {
    return UserValueInput(
        keyValueId = keyValueId,
        importance = importance
    )
}
