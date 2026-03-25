package love.bside.server.models.db

import kotlinx.serialization.Serializable

/**
 * Database models matching PocketBase responses
 * These represent the raw data structures from PocketBase API
 */

@Serializable
data class PBUser(
    val id: String,
    val collectionId: String = "",
    val collectionName: String = "users",
    val email: String,
    val emailVisibility: Boolean = false,
    val verified: Boolean = false,
    val created: String,
    val updated: String
)

@Serializable
data class PBProfile(
    val id: String,
    val collectionId: String = "",
    val collectionName: String = "s_profiles",
    val user: String,
    val firstName: String = "",
    val lastName: String = "",
    val birthDate: String = "2000-01-01",
    val bio: String? = null,
    val bioBrotliBase64: String? = null, // brotli:base64:text
    val location: String? = null,
    val locationBrotliBase64: String? = null, // brotli:base64:text
    val seeking: String = "both",
    val created: String,
    val updated: String
)

@Serializable
data class PBKeyValue(
    val id: String,
    val collectionId: String = "",
    val collectionName: String = "s_key_values",
    val key: String,
    val category: String,
    val description: String? = null,
    val displayOrder: Int = 0,
    val created: String,
    val updated: String
)

@Serializable
data class PBUserValue(
    val id: String,
    val collectionId: String = "",
    val collectionName: String = "s_user_values",
    val userId: String,
    val keyValueId: String,
    val importance: Int,
    val created: String,
    val updated: String,
    // Expanded relation fields
    val expand: PBUserValueExpand? = null
)

@Serializable
data class PBUserValueExpand(
    val keyValueId: PBKeyValue? = null
)

@Serializable
data class PBMatch(
    val id: String,
    val collectionId: String = "",
    val collectionName: String = "m_matches",
    val user1: String, // Changed to match migration
    val user2: String, // Changed to match migration
    val status: String,
    val created: String,
    val updated: String,
    // Expanded relation fields
    val expand: PBMatchExpand? = null
)

@Serializable
data class PBMatchExpand(
    val matchedUserId: PBUser? = null,
    val profileId: PBProfile? = null
)

@Serializable
data class PBPrompt(
    val id: String,
    val collectionId: String = "",
    val collectionName: String = "s_prompts",
    val text: String,
    val category: String,
    val displayOrder: Int = 0,
    val created: String,
    val updated: String
)

@Serializable
data class PBUserAnswer(
    val id: String,
    val collectionId: String = "",
    val collectionName: String = "s_user_answers",
    val userId: String,
    val promptId: String,
    val answer: String,
    val responseBrotliBase64: String? = null, // brotli:base64:text
    val created: String,
    val updated: String,
    // Expanded relation fields
    val expand: PBUserAnswerExpand? = null
)

@Serializable
data class PBUserAnswerExpand(
    val promptId: PBPrompt? = null
)

@Serializable
data class PBMessage(
    val id: String,
    val collectionId: String = "",
    val collectionName: String = "m_messages",
    val conversation: String,
    val sender: String,
    val content: String? = null,
    val contentBrotliBase64: String? = null, // brotli:base64:text
    val type: String = "text",
    val replyTo: String? = null,
    val created: String,
    val updated: String
)

// ===== PocketBase API Response Wrappers =====

@Serializable
data class PBListResponse<T>(
    val page: Int,
    val perPage: Int,
    val totalItems: Int,
    val totalPages: Int,
    val items: List<T>
)

@Serializable
data class PBAuthResponse(
    val token: String,
    val record: PBUser
)

@Serializable
data class PBErrorResponse(
    val code: Int,
    val message: String,
    val data: Map<String, PBFieldError>? = null
)

@Serializable
data class PBFieldError(
    val code: String,
    val message: String
)
