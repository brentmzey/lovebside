package io.pocketbase.types

import kotlinx.serialization.Serializable
import kotlinx.datetime.Instant
import io.pocketbase.models.RecordModel
import kotlinx.serialization.json.JsonObject

@Serializable
data class Profile(
    @kotlinx.serialization.SerialName("user_id")
    val userId: String,
    @kotlinx.serialization.SerialName("first_name")
    val firstName: String,
    val middle: String = "",
    @kotlinx.serialization.SerialName("last_name")
    val lastName: String,
    @kotlinx.serialization.SerialName("birth_date")
    val birthDate: Instant,
    val bio: String = "",
    val location: String = "",
    val seeking: String,
    @kotlinx.serialization.SerialName("profile_picture")
    val profilePicture: String = "",
    val photos: List<String> = emptyList(),
    @kotlinx.serialization.SerialName("about_me")
    val aboutMe: String = "",
    val height: Double? = null,
    val occupation: String = "",
    val education: String = "",
    val interests: List<String> = emptyList()
) : RecordModel()

@Serializable
data class Questionnaire(
    @kotlinx.serialization.SerialName("question_text")
    val questionText: String,
    @kotlinx.serialization.SerialName("question_order")
    val questionOrder: Int,
    @kotlinx.serialization.SerialName("is_active")
    val isActive: Boolean
) : RecordModel()

@Serializable
data class UserAnswer(
    @kotlinx.serialization.SerialName("user_id")
    val userId: String,
    @kotlinx.serialization.SerialName("question_id")
    val questionId: String,
    @kotlinx.serialization.SerialName("answer_text")
    val answerText: String
) : RecordModel()

@Serializable
data class KeyValue(
    @kotlinx.serialization.SerialName("value_text")
    val valueText: String,
    val category: String
) : RecordModel()

@Serializable
data class UserValue(
    @kotlinx.serialization.SerialName("user_id")
    val userId: String,
    @kotlinx.serialization.SerialName("value_id")
    val valueId: String
) : RecordModel()

@Serializable
data class Match(
    @kotlinx.serialization.SerialName("user_one_id")
    val userOneId: String,
    @kotlinx.serialization.SerialName("user_two_id")
    val userTwoId: String,
    @kotlinx.serialization.SerialName("match_score")
    val matchScore: Double,
    @kotlinx.serialization.SerialName("match_status")
    val matchStatus: String,
    @kotlinx.serialization.SerialName("generated_at")
    val generatedAt: Instant
) : RecordModel()

@Serializable
data class Prompt(
    @kotlinx.serialization.SerialName("match_id")
    val matchId: String,
    @kotlinx.serialization.SerialName("prompt_text")
    val promptText: String,
    @kotlinx.serialization.SerialName("prompt_type")
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
    @kotlinx.serialization.SerialName("conversation_type")
    val conversationType: String, // direct, group, channel
    @kotlinx.serialization.SerialName("conversation_name")
    val conversationName: String? = null,
    @kotlinx.serialization.SerialName("conversation_avatar")
    val conversationAvatar: String? = null,
    @kotlinx.serialization.SerialName("last_message_text")
    val lastMessageText: String? = null,
    @kotlinx.serialization.SerialName("last_message_at")
    val lastMessageAt: Instant? = null,
    @kotlinx.serialization.SerialName("total_message_count")
    val totalMessageCount: Int = 0,
    @kotlinx.serialization.SerialName("max_participants")
    val maxParticipants: Int = 2,
    @kotlinx.serialization.SerialName("is_archived")
    val isArchived: Boolean = false
) : RecordModel()

@Serializable
data class ConversationParticipant(
    @kotlinx.serialization.SerialName("conversation_id")
    val conversationId: String,
    @kotlinx.serialization.SerialName("user_id")
    val userId: String,
    val role: String, // admin, member, readonly
    @kotlinx.serialization.SerialName("unread_count")
    val unreadCount: Int = 0,
    @kotlinx.serialization.SerialName("last_read_at")
    val lastReadAt: Instant? = null,
    @kotlinx.serialization.SerialName("joined_at")
    val joinedAt: Instant,
    @kotlinx.serialization.SerialName("left_at")
    val leftAt: Instant? = null,
    @kotlinx.serialization.SerialName("is_muted")
    val isMuted: Boolean = false,
    @kotlinx.serialization.SerialName("is_pinned")
    val isPinned: Boolean = false
) : RecordModel()

@Serializable
data class Message(
    @kotlinx.serialization.SerialName("conversation_id")
    val conversationId: String,
    @kotlinx.serialization.SerialName("sender_id")
    val senderId: String,
    val content: String,
    @kotlinx.serialization.SerialName("message_type")
    val messageType: String, // text, image, file, system
    val attachments: List<String> = emptyList(),
    @kotlinx.serialization.SerialName("sent_at")
    val sentAt: Instant,
    @kotlinx.serialization.SerialName("edited_at")
    val editedAt: Instant? = null,
    @kotlinx.serialization.SerialName("deleted_at")
    val deletedAt: Instant? = null,
    @kotlinx.serialization.SerialName("read_by_count")
    val readByCount: Int = 0,
    // Threading fields
    @kotlinx.serialization.SerialName("reply_to_message_id")
    val replyToMessageId: String? = null,
    @kotlinx.serialization.SerialName("thread_root_id")
    val threadRootId: String? = null,
    @kotlinx.serialization.SerialName("thread_depth")
    val threadDepth: Int? = null,
    @kotlinx.serialization.SerialName("thread_reply_count")
    val threadReplyCount: Int? = null
) : RecordModel()

@Serializable
data class ReadReceipt(
    @kotlinx.serialization.SerialName("message_id")
    val messageId: String,
    @kotlinx.serialization.SerialName("user_id")
    val userId: String,
    @kotlinx.serialization.SerialName("read_at")
    val readAt: Instant
) : RecordModel()

@Serializable
data class TypingIndicator(
    val conversationId: String,
    val userId: String,
    val isTyping: Boolean,
    val lastUpdated: Instant
) : RecordModel()
