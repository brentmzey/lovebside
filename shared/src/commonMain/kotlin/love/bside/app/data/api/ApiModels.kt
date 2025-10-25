package love.bside.app.data.api

import kotlinx.serialization.Serializable

/**
 * API DTOs for communication with internal API
 * These mirror the server's API models
 */

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
    val birthDate: String,
    val seeking: String
)

@Serializable
data class AuthResponse(
    val token: String,
    val refreshToken: String,
    val expiresIn: Long,
    val user: UserDTO
)

@Serializable
data class RefreshTokenRequest(
    val refreshToken: String
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
    val seeking: String
)

@Serializable
data class UpdateProfileRequest(
    val firstName: String? = null,
    val lastName: String? = null,
    val bio: String? = null,
    val location: String? = null,
    val seeking: String? = null
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
data class SaveUserValuesRequest(
    val values: List<UserValueInput>
)

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
    val status: String,
    val createdAt: String
)

@Serializable
data class MatchActionRequest(
    val action: String
)

@Serializable
data class DiscoverMatchesResponse(
    val matches: List<MatchDTO>,
    val hasMore: Boolean
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
data class HealthResponse(
    val status: String,
    val version: String,
    val timestamp: String
)
