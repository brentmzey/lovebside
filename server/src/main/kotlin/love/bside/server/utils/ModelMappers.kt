package love.bside.server.utils

import love.bside.server.models.api.*
import love.bside.server.models.domain.*
import love.bside.server.models.db.*
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

/**
 * Extension functions to map between different model layers:
 * - Database (PocketBase) ↔ Domain (Business Logic)
 * - Domain ↔ API (DTOs for clients)
 */

// ===== Database to Domain =====

fun PBUser.toDomain(): User = User(
    id = id,
    email = email,
    emailVisibility = emailVisibility,
    verified = verified,
    createdAt = Instant.parse(created),
    updatedAt = Instant.parse(updated)
)

fun PBProfile.toDomain(): Profile = Profile(
    id = id,
    userId = userId,
    firstName = firstName,
    lastName = lastName,
    birthDate = LocalDate.parse(birthDate),
    bio = bio,
    location = location,
    seeking = SeekingType.fromString(seeking),
    createdAt = Instant.parse(created),
    updatedAt = Instant.parse(updated)
)

fun PBKeyValue.toDomain(): KeyValue = KeyValue(
    id = id,
    key = key,
    category = CategoryType.fromString(category),
    description = description,
    displayOrder = displayOrder,
    createdAt = Instant.parse(created),
    updatedAt = Instant.parse(updated)
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
    createdAt = Instant.parse(created),
    updatedAt = Instant.parse(updated)
)

fun PBMatch.toDomain(): Match = Match(
    id = id,
    userId = userId,
    matchedUserId = matchedUserId,
    compatibilityScore = compatibilityScore,
    status = MatchStatus.fromString(status),
    createdAt = Instant.parse(created),
    updatedAt = Instant.parse(updated)
)

fun PBPrompt.toDomain(): Prompt = Prompt(
    id = id,
    text = text,
    category = category,
    displayOrder = displayOrder,
    createdAt = Instant.parse(created),
    updatedAt = Instant.parse(updated)
)

fun PBUserAnswer.toDomain(): UserAnswer = UserAnswer(
    id = id,
    userId = userId,
    promptId = promptId,
    answer = answer,
    createdAt = Instant.parse(created),
    updatedAt = Instant.parse(updated)
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
    bio = bio,
    location = location,
    seeking = seeking.name
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
    status = status.name,
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
fun <T> PBListResponse<T>.toPaginatedResponse(transform: (T) -> Any): PaginatedResponse<Any> = PaginatedResponse(
    items = items.map(transform),
    page = page,
    perPage = perPage,
    totalItems = totalItems,
    totalPages = totalPages
)
