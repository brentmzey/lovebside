package love.bside.app.orchestration.events

import kotlinx.datetime.Instant

/**
 * Domain events for the B-Side application.
 * These events represent business-significant occurrences.
 */

// ===== User Events =====

data class UserRegistered(
    override val aggregateId: String,
    val email: String,
    val userId: String
) : BaseDomainEvent()

data class UserLoggedIn(
    override val aggregateId: String,
    val userId: String,
    val sessionId: String
) : BaseDomainEvent()

data class UserLoggedOut(
    override val aggregateId: String,
    val userId: String
) : BaseDomainEvent()

data class UserProfileUpdated(
    override val aggregateId: String,
    val userId: String,
    val updatedFields: Set<String>
) : BaseDomainEvent()

// ===== Messaging Events =====

data class MessageSent(
    override val aggregateId: String,
    val messageId: String,
    val conversationId: String,
    val senderId: String,
    val receiverId: String,
    val content: String
) : BaseDomainEvent()

data class MessageDelivered(
    override val aggregateId: String,
    val messageId: String,
    val deliveredAt: Instant
) : BaseDomainEvent()

data class MessageRead(
    override val aggregateId: String,
    val messageId: String,
    val readerId: String,
    val readAt: Instant
) : BaseDomainEvent()

data class ConversationCreated(
    override val aggregateId: String,
    val conversationId: String,
    val participantIds: List<String>
) : BaseDomainEvent()

data class TypingStarted(
    override val aggregateId: String,
    val conversationId: String,
    val userId: String
) : BaseDomainEvent()

data class TypingStopped(
    override val aggregateId: String,
    val conversationId: String,
    val userId: String
) : BaseDomainEvent()

// ===== Match Events =====

data class MatchCreated(
    override val aggregateId: String,
    val matchId: String,
    val userId1: String,
    val userId2: String,
    val score: Double
) : BaseDomainEvent()

data class MatchAccepted(
    override val aggregateId: String,
    val matchId: String,
    val acceptedBy: String
) : BaseDomainEvent()

data class MatchRejected(
    override val aggregateId: String,
    val matchId: String,
    val rejectedBy: String
) : BaseDomainEvent()

// ===== Questionnaire Events =====

data class QuestionnaireStarted(
    override val aggregateId: String,
    val userId: String,
    val questionnaireId: String
) : BaseDomainEvent()

data class QuestionAnswered(
    override val aggregateId: String,
    val userId: String,
    val questionId: String,
    val answer: String
) : BaseDomainEvent()

data class QuestionnaireCompleted(
    override val aggregateId: String,
    val userId: String,
    val questionnaireId: String
) : BaseDomainEvent()

// ===== System Events =====

data class NetworkConnected(
    val connectionType: String
) : BaseDomainEvent()

data class NetworkDisconnected(
    val reason: String?
) : BaseDomainEvent()

data class SyncStarted(
    override val aggregateId: String,
    val syncType: String
) : BaseDomainEvent()

data class SyncCompleted(
    override val aggregateId: String,
    val syncType: String,
    val itemsSynced: Int
) : BaseDomainEvent()

data class SyncFailed(
    override val aggregateId: String,
    val syncType: String,
    val error: String
) : BaseDomainEvent()
