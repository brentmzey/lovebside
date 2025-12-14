package io.pocketbase.types

import kotlinx.serialization.Serializable
import kotlinx.datetime.Instant
import io.pocketbase.models.RecordModel
import kotlinx.serialization.json.JsonObject

@Serializable
data class Profile(
    val userId: String,
    val firstName: String,
    val middle: String = "",
    val lastName: String,
    val birthDate: Instant,
    val bio: String = "",
    val location: String = "",
    val seeking: String,
    val profilePicture: String = "",
    val photos: List<String> = emptyList(),
    val aboutMe: String = "",
    val height: Double? = null,
    val occupation: String = "",
    val education: String = "",
    val interests: List<String> = emptyList()
) : RecordModel()

@Serializable
data class Questionnaire(
    val questionText: String,
    val questionOrder: Int,
    val isActive: Boolean
) : RecordModel()

@Serializable
data class UserAnswer(
    val userId: String,
    val questionId: String,
    val answerText: String
) : RecordModel()

@Serializable
data class KeyValue(
    val valueText: String,
    val category: String
) : RecordModel()

@Serializable
data class UserValue(
    val userId: String,
    val valueId: String
) : RecordModel()

@Serializable
data class Match(
    val userOneId: String,
    val userTwoId: String,
    val matchScore: Double,
    val matchStatus: String,
    val generatedAt: Instant
) : RecordModel()

@Serializable
data class Prompt(
    val matchId: String,
    val promptText: String,
    val promptType: String
) : RecordModel()

// ============================================================================
// GROUP MESSAGING MODELS (Updated)
// ============================================================================

enum class ConversationType {
    DIRECT, GROUP, CHANNEL
}

enum class ParticipantRole {
    ADMIN, MEMBER, READONLY
}

enum class MessageType {
    TEXT, IMAGE, FILE, SYSTEM
}

@Serializable
data class Conversation(
    val conversationType: String, // direct, group, channel
    val conversationName: String? = null,
    val conversationAvatar: String? = null,
    val lastMessageText: String? = null,
    val lastMessageAt: Instant? = null,
    val totalMessageCount: Int = 0,
    val maxParticipants: Int = 2,
    val isArchived: Boolean = false
) : RecordModel()

@Serializable
data class ConversationParticipant(
    val conversationId: String,
    val userId: String,
    val role: String, // admin, member, readonly
    val unreadCount: Int = 0,
    val lastReadAt: Instant? = null,
    val joinedAt: Instant,
    val leftAt: Instant? = null,
    val isMuted: Boolean = false,
    val isPinned: Boolean = false
) : RecordModel()

@Serializable
data class Message(
    val conversationId: String,
    val senderId: String,
    val content: String,
    val messageType: String, // text, image, file, system
    val attachments: List<String> = emptyList(),
    val sentAt: Instant,
    val editedAt: Instant? = null,
    val deletedAt: Instant? = null,
    val readByCount: Int = 0,
    // Threading fields
    val replyToMessageId: String? = null,
    val threadRootId: String? = null,
    val threadDepth: Int? = null,
    val threadReplyCount: Int? = null
) : RecordModel()

@Serializable
data class ReadReceipt(
    val messageId: String,
    val userId: String,
    val readAt: Instant
) : RecordModel()

@Serializable
data class TypingIndicator(
    val conversationId: String,
    val userId: String,
    val isTyping: Boolean,
    val lastUpdated: Instant
) : RecordModel()
