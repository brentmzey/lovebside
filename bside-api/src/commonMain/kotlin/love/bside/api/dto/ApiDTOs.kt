package love.bside.api.dto

import kotlinx.serialization.Serializable

/**
 * Data Transfer Objects (DTOs) for API communication.
 * These are platform-independent contracts between client and server.
 */

// ===== User DTOs =====

@Serializable
data class UserDTO(
    val id: String,
    val email: String,
    val displayName: String? = null,
    val profilePhotoUrl: String? = null,
    val bio: String? = null,
    val isActive: Boolean = true,
    val emailVerified: Boolean = false,
    val created: String,
    val updated: String
)

@Serializable
data class CreateUserRequest(
    val email: String,
    val password: String,
    val displayName: String? = null
)

@Serializable
data class UpdateUserRequest(
    val displayName: String? = null,
    val bio: String? = null,
    val profilePhotoUrl: String? = null
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class LoginResponse(
    val token: String,
    val user: UserDTO,
    val refreshToken: String? = null,
    val expiresIn: Long
)

// ===== Messaging DTOs =====

@Serializable
data class ConversationDTO(
    val id: String,
    val participantIds: List<String>,
    val isGroup: Boolean = false,
    val name: String? = null,
    val lastMessagePreview: String? = null,
    val lastMessageAt: String? = null,
    val unreadCount: Int = 0,
    val created: String,
    val updated: String
)

@Serializable
data class MessageDTO(
    val id: String,
    val conversationId: String,
    val senderId: String,
    val receiverId: String,
    val content: String,
    val messageType: String = "TEXT",
    val status: String = "SENT",
    val sentAt: String,
    val deliveredAt: String? = null,
    val readAt: String? = null,
    val editedAt: String? = null,
    val replyToMessageId: String? = null,
    val created: String,
    val updated: String
)

@Serializable
data class SendMessageRequest(
    val conversationId: String,
    val content: String,
    val replyToMessageId: String? = null
)

@Serializable
data class CreateConversationRequest(
    val participantIds: List<String>,
    val isGroup: Boolean = false,
    val name: String? = null
)

@Serializable
data class TypingIndicatorDTO(
    val conversationId: String,
    val userId: String,
    val isTyping: Boolean,
    val timestamp: String
)

// ===== Match DTOs =====

@Serializable
data class MatchDTO(
    val id: String,
    val userId1: String,
    val userId2: String,
    val compatibilityScore: Double,
    val status: String,
    val matchedAt: String,
    val respondedAt: String? = null,
    val created: String,
    val updated: String
)

@Serializable
data class MatchResponseRequest(
    val matchId: String,
    val accept: Boolean
)

// ===== Questionnaire DTOs =====

@Serializable
data class QuestionDTO(
    val id: String,
    val text: String,
    val category: String,
    val order: Int
)

@Serializable
data class AnswerDTO(
    val id: String,
    val userId: String,
    val questionId: String,
    val answer: String,
    val answeredAt: String
)

@Serializable
data class SubmitAnswerRequest(
    val questionId: String,
    val answer: String
)

// ===== Common Response Wrappers =====

@Serializable
data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val error: ErrorDTO? = null,
    val timestamp: String
)

@Serializable
data class PagedResponse<T>(
    val items: List<T>,
    val page: Int,
    val pageSize: Int,
    val totalItems: Int,
    val totalPages: Int,
    val hasNext: Boolean,
    val hasPrevious: Boolean
)

@Serializable
data class ErrorDTO(
    val code: String,
    val message: String,
    val field: String? = null,
    val details: Map<String, String>? = null
)

// ===== Health & Status =====

@Serializable
data class HealthCheckResponse(
    val status: String,
    val version: String,
    val timestamp: String,
    val checks: Map<String, String>
)
