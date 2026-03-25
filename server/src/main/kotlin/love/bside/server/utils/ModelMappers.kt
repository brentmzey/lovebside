package love.bside.server.utils

import love.bside.server.models.api.*
import love.bside.server.models.domain.*
import love.bside.server.models.db.*
import love.bside.app.utils.CompressionService
import arrow.core.toOption
import arrow.core.getOrElse
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

/**
 * Extension functions to map between different model layers:
 * - Database (PocketBase) ↔ Domain (Business Logic)
 * - Domain ↔ API (DTOs for clients)
 */

fun String.toInstant(): Instant {
    // PocketBase uses ' ' instead of 'T' for ISO-8601
    return Instant.parse(this.replace(' ', 'T'))
}

// ===== Database to Domain =====

fun PBUser.toDomain(): User = User(
    id = id,
    email = email,
    emailVisibility = emailVisibility,
    verified = verified,
    createdAt = created.toInstant(),
    updatedAt = updated.toInstant()
)

suspend fun PBProfile.toDomain(): Profile = Profile(
    id = id,
    userId = user,
    firstName = firstName,
    lastName = lastName,
    birthDate = LocalDate.parse(birthDate),
    // Transparently prefer compressed data
    bio = CompressionService.decompressFromBase64(bioBrotliBase64.toOption()).let { 
        if (it.isSome()) it else bio.toOption() 
    },
    location = CompressionService.decompressFromBase64(locationBrotliBase64.toOption()).let {
        if (it.isSome()) it else location.toOption()
    },
    seeking = SeekingType.fromString(seeking),
    createdAt = created.toInstant(),
    updatedAt = updated.toInstant()
)

suspend fun Profile.toDB(): PBProfile = PBProfile(
    id = id,
    user = userId,
    firstName = firstName,
    lastName = lastName,
    birthDate = birthDate.toString(),
    // Standard bio for legacy/admin readability
    bio = bio.getOrElse { null },
    // Extreme compression for optimized storage
    bioBrotliBase64 = CompressionService.compressToBase64(bio).getOrElse { null },
    location = location.getOrElse { null },
    locationBrotliBase64 = CompressionService.compressToBase64(location).getOrElse { null },
    seeking = seeking.name.lowercase(),
    created = createdAt.toString(),
    updated = updatedAt.toString()
)

fun PBKeyValue.toDomain(): KeyValue = KeyValue(
    id = id,
    key = key,
    category = CategoryType.fromString(category),
    description = description,
    displayOrder = displayOrder,
    createdAt = created.toInstant(),
    updatedAt = updated.toInstant()
)

fun PBUserValue.toDomain(): UserValue = UserValue(
    id = id,
    userId = userId,
    keyValue = expand?.keyValueId?.toDomain() ?: KeyValue(
        id = keyValueId,
        key = "unknown",
        category = CategoryType.VALUES,
        description = null,
        displayOrder = 0,
        createdAt = Instant.parse(created),
        updatedAt = Instant.parse(updated)
    ),
    importance = importance,
    createdAt = created.toInstant(),
    updatedAt = updated.toInstant()
)

fun PBMatch.toDomain(): Match = Match(
    id = id,
    userId = user1, // Updated for m_matches naming
    matchedUserId = user2, // Updated for m_matches naming
    compatibilityScore = 0.0, // Default for now
    status = MatchStatus.fromString(status),
    createdAt = created.toInstant(),
    updatedAt = updated.toInstant()
)

fun PBPrompt.toDomain(): Prompt = Prompt(
    id = id,
    text = text,
    category = category,
    displayOrder = displayOrder,
    createdAt = created.toInstant(),
    updatedAt = updated.toInstant()
)

suspend fun PBUserAnswer.toDomain(): UserAnswer = UserAnswer(
    id = id,
    userId = userId,
    promptId = promptId,
    // Prefer compressed response
    answer = CompressionService.decompressFromBase64(responseBrotliBase64.toOption())
        .map { if (it.isEmpty() && answer.isNotEmpty()) answer else it }
        .getOrElse { answer },
    createdAt = created.toInstant(),
    updatedAt = updated.toInstant()
)

suspend fun PBMessage.toDomain(): Message = Message(
    id = id,
    collectionId = collectionId,
    conversationId = conversation,
    senderId = sender,
    content = CompressionService.decompressFromBase64(contentBrotliBase64.toOption()).let {
        if (it.isSome()) it else content.toOption()
    },
    messageType = MessageType.valueOf(type.uppercase()),
    attachments = emptyList(),
    sentAt = created.toInstant(),
    editedAt = null,
    deletedAt = null,
    readByCount = 0,
    created = created.toInstant(),
    updated = updated.toInstant(),
    replyToMessageId = replyTo,
    threadRootId = null,
    threadDepth = 0,
    threadReplyCount = 0
)

// ===== Domain to API =====

fun User.toDTO(profile: Profile? = null): UserDTO = UserDTO(
    id = id,
    email = email,
    profile = profile?.toDTO()
)

fun Profile.toDTO(): ProfileDTO = ProfileDTO(
    firstName = firstName,
    lastName = lastName,
    age = age,
    bio = bio.getOrNull(),
    location = location.getOrNull(),
    seeking = SeekingTypeDTO.fromString(seeking.name)
)

fun KeyValue.toDTO(): KeyValueDTO = KeyValueDTO(
    id = id,
    key = key,
    category = category.name,
    description = description,
    displayOrder = displayOrder
)

fun UserValue.toDTO(): UserValueDTO = UserValueDTO(
    id = id,
    keyValue = keyValue.toDTO(),
    importance = importance
)

fun Match.toDTO(matchedUser: User, matchedProfile: Profile?, sharedValues: List<KeyValue>): MatchDTO = MatchDTO(
    id = id,
    user = matchedUser.toDTO(matchedProfile),
    compatibilityScore = compatibilityScore,
    sharedValues = sharedValues.map { it.toDTO() },
    status = MatchStatusDTO.fromString(status.name),
    createdAt = createdAt.toString()
)

fun Prompt.toDTO(): PromptDTO = PromptDTO(
    id = id,
    text = text,
    category = category,
    displayOrder = displayOrder
)

fun UserAnswer.toDTO(prompt: Prompt): PromptAnswerDTO = PromptAnswerDTO(
    id = id,
    prompt = prompt.toDTO(),
    answer = answer,
    createdAt = createdAt.toString()
)

fun AuthToken.toAuthResponse(user: User, profile: Profile?): AuthResponse = AuthResponse(
    token = token,
    refreshToken = refreshToken,
    expiresIn = expiresAt.toEpochMilliseconds() - kotlinx.datetime.Clock.System.now().toEpochMilliseconds(),
    user = user.toDTO(profile)
)

// ===== Helper Functions =====

/**
 * Create success API response
 */
fun <T> T.toSuccessResponse(): ApiResponse<T> = ApiResponse(
    success = true,
    data = this,
    error = null
)

/**
 * Create error API response
 */
fun String.toErrorResponse(code: String = "INTERNAL_ERROR", details: Map<String, String>? = null): ApiResponse<Nothing> = ApiResponse(
    success = false,
    data = null,
    error = ApiError(
        code = code,
        message = this,
        details = details
    )
)

/**
 * Create paginated response
 */
fun <T, R> PBListResponse<T>.toPaginatedResponse(transform: (T) -> R): PaginatedResponse<R> = PaginatedResponse(
    items = items.map(transform),
    page = page,
    perPage = perPage,
    totalItems = totalItems,
    totalPages = totalPages
)
