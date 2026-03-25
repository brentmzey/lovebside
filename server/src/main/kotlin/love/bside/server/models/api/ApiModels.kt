package love.bside.server.models.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * API Request/Response DTOs
 * All enum fields use @SerialName to match wire format.
 */

// ===== Enums (serialized as lowercase strings on the wire) =====

@Serializable
enum class SeekingTypeDTO {
    @SerialName("friendship") FRIENDSHIP,
    @SerialName("relationship") RELATIONSHIP,
    @SerialName("both") BOTH;

    companion object {
        fun fromString(v: String): SeekingTypeDTO = when (v.lowercase()) {
            "friendship" -> FRIENDSHIP
            "relationship" -> RELATIONSHIP
            else -> BOTH
        }
    }
}

@Serializable
enum class MatchStatusDTO {
    @SerialName("pending") PENDING,
    @SerialName("liked") LIKED,
    @SerialName("passed") PASSED,
    @SerialName("mutual") MUTUAL,
    @SerialName("discovered") DISCOVERED;

    companion object {
        fun fromString(v: String): MatchStatusDTO = when (v.lowercase()) {
            "liked" -> LIKED
            "passed" -> PASSED
            "mutual" -> MUTUAL
            "discovered" -> DISCOVERED
            else -> PENDING
        }
    }
}

@Serializable
enum class MatchActionDTO {
    @SerialName("like") LIKE,
    @SerialName("pass") PASS,
    @SerialName("superlike") SUPERLIKE
}

@Serializable
enum class MessageTypeDTO {
    @SerialName("text") TEXT,
    @SerialName("image") IMAGE,
    @SerialName("audio") AUDIO,
    @SerialName("video") VIDEO,
    @SerialName("file") FILE
}

// ===== Authentication DTOs =====

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class RegisterRequest(
    val email: String,
    val password: String,
    val passwordConfirm: String,
    val firstName: String,
    val lastName: String,
    val birthDate: String, // ISO date: YYYY-MM-DD
    val seeking: SeekingTypeDTO
)

@Serializable
data class AuthResponse(
    val token: String,
    val refreshToken: String,
    val expiresIn: Long,
    val user: UserDTO
)

@Serializable
data class RefreshTokenRequest(val refreshToken: String)

@Serializable
data class ForgotPasswordRequest(val email: String)

@Serializable
data class ResetPasswordRequest(
    val token: String,
    val password: String,
    val passwordConfirm: String
)

// ===== User DTOs =====

@Serializable
data class UserDTO(
    val id: String,
    val email: String,
    val profile: ProfileDTO?
)

@Serializable
data class ProfileDTO(
    val firstName: String,
    val lastName: String,
    val age: Int,
    val bio: String? = null,
    val location: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val seeking: SeekingTypeDTO
)

@Serializable
data class UpdateProfileRequest(
    val firstName: String? = null,
    val lastName: String? = null,
    val bio: String? = null,
    val location: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val seeking: SeekingTypeDTO? = null
)

// ===== Values DTOs =====

@Serializable
data class KeyValueDTO(
    val id: String,
    val key: String,
    val category: String,
    val description: String? = null,
    val displayOrder: Int = 0
)

@Serializable
data class UserValueDTO(
    val id: String,
    val keyValue: KeyValueDTO,
    val importance: Int
)

@Serializable
data class SaveUserValuesRequest(val values: List<UserValueInput>)

@Serializable
data class UserValueInput(
    val keyValueId: String,
    val importance: Int
)

// ===== Match DTOs =====

@Serializable
data class MatchDTO(
    val id: String,
    val user: UserDTO,
    val compatibilityScore: Double,
    val sharedValues: List<KeyValueDTO>,
    val status: MatchStatusDTO,
    val createdAt: String
)

@Serializable
data class MatchActionRequest(val action: MatchActionDTO)

@Serializable
data class DiscoverMatchesResponse(
    val matches: List<MatchDTO>,
    val hasMore: Boolean
)

@Serializable
data class SwipeRequest(
    val targetUserId: String,
    val direction: MatchActionDTO
)

// ===== Prompt DTOs =====

@Serializable
data class PromptDTO(
    val id: String,
    val text: String,
    val category: String,
    val displayOrder: Int = 0
)

@Serializable
data class PromptAnswerDTO(
    val id: String,
    val prompt: PromptDTO,
    val answer: String,
    val createdAt: String
)

@Serializable
data class SubmitAnswerRequest(
    val promptId: String,
    val answer: String
)

// ===== Common Response Wrappers =====

@Serializable
data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val error: ApiError? = null
)

@Serializable
data class ApiError(
    val code: String,
    val message: String,
    val details: Map<String, String>? = null
)

@Serializable
data class PaginatedResponse<T>(
    val items: List<T>,
    val page: Int,
    val perPage: Int,
    val totalItems: Int,
    val totalPages: Int
)

// ===== Health & Meta =====

@Serializable
data class HealthResponse(
    val status: String,
    val version: String,
    val timestamp: String
)
