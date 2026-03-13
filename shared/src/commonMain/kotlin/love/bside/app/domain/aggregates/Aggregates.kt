package love.bside.app.domain.aggregates

import love.bside.app.core.EntityId
import love.bside.app.domain.core.AggregateRoot
import love.bside.app.orchestration.events.*
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * User aggregate root
 */
data class UserAggregate(
    override val id: EntityId,
    val email: String,
    val displayName: String?,
    val profilePhotoUrl: String?,
    val bio: String?,
    val isActive: Boolean = true,
    val emailVerified: Boolean = false,
    override val version: Long = 0,
    override val createdAt: Instant = Clock.System.now(),
    override val updatedAt: Instant = Clock.System.now()
) : AggregateRoot<EntityId>() {

    fun register(email: String, password: String): UserAggregate {
        val newUser = copy(
            email = email,
            emailVerified = false,
            updatedAt = Clock.System.now()
        )
        
        newUser.addDomainEvent(UserRegistered(
            aggregateId = id.toString(),
            email = email,
            userId = id.toString()
        ))
        
        return newUser
    }

    fun updateProfile(
        displayName: String? = null,
        bio: String? = null,
        profilePhotoUrl: String? = null
    ): UserAggregate {
        val updatedFields = mutableSetOf<String>()
        if (displayName != null) {
            updatedFields.add("displayName")
        }
        if (bio != null) {
            updatedFields.add("bio")
        }
        if (profilePhotoUrl != null) {
            updatedFields.add("profilePhotoUrl")
        }

        val updated = copy(
            displayName = displayName ?: this.displayName,
            bio = bio ?: this.bio,
            profilePhotoUrl = profilePhotoUrl ?: this.profilePhotoUrl,
            updatedAt = Clock.System.now(),
            version = version + 1
        )

        updated.addDomainEvent(UserProfileUpdated(
            aggregateId = id.toString(),
            userId = id.toString(),
            updatedFields = updatedFields
        ))

        return updated
    }

    fun deactivate(): UserAggregate {
        return copy(
            isActive = false,
            updatedAt = Clock.System.now(),
            version = version + 1
        )
    }

    fun verifyEmail(): UserAggregate {
        return copy(
            emailVerified = true,
            updatedAt = Clock.System.now(),
            version = version + 1
        )
    }
}

/**
 * Conversation aggregate root
 */
data class ConversationAggregate(
    override val id: EntityId,
    val participantIds: List<EntityId>,
    val isGroup: Boolean = false,
    val name: String? = null,
    val lastMessageId: EntityId? = null,
    val lastMessageAt: Instant? = null,
    val lastMessagePreview: String? = null,
    override val version: Long = 0,
    override val createdAt: Instant = Clock.System.now(),
    override val updatedAt: Instant = Clock.System.now()
) : AggregateRoot<EntityId>() {

    init {
        require(participantIds.size >= 2) { "Conversation must have at least 2 participants" }
    }

    fun sendMessage(senderId: EntityId, content: String, messageId: EntityId): ConversationAggregate {
        val receiverId = participantIds.first { it != senderId }
        
        val updated = copy(
            lastMessageId = messageId,
            lastMessageAt = Clock.System.now(),
            lastMessagePreview = content.take(100),
            updatedAt = Clock.System.now(),
            version = version + 1
        )

        updated.addDomainEvent(MessageSent(
            aggregateId = id.toString(),
            messageId = messageId.toString(),
            conversationId = id.toString(),
            senderId = senderId.toString(),
            receiverId = receiverId.toString(),
            content = content
        ))

        return updated
    }

    fun addParticipant(userId: EntityId): ConversationAggregate {
        if (userId in participantIds) {
            return this
        }

        return copy(
            participantIds = participantIds + userId,
            updatedAt = Clock.System.now(),
            version = version + 1
        )
    }

    fun removeParticipant(userId: EntityId): ConversationAggregate {
        require(participantIds.size > 2) { "Cannot remove participant from 1-1 conversation" }

        return copy(
            participantIds = participantIds - userId,
            updatedAt = Clock.System.now(),
            version = version + 1
        )
    }
}

/**
 * Match aggregate root
 */
data class MatchAggregate(
    override val id: EntityId,
    val userId1: EntityId,
    val userId2: EntityId,
    val compatibilityScore: Double,
    val status: MatchStatus = MatchStatus.PENDING,
    val matchedAt: Instant = Clock.System.now(),
    val respondedAt: Instant? = null,
    override val version: Long = 0,
    override val createdAt: Instant = Clock.System.now(),
    override val updatedAt: Instant = Clock.System.now()
) : AggregateRoot<EntityId>() {

    init {
        require(userId1 != userId2) { "Cannot match user with themselves" }
        require(compatibilityScore in 0.0..100.0) { "Compatibility score must be between 0 and 100" }
    }

    fun accept(userId: EntityId): MatchAggregate {
        require(userId == userId1 || userId == userId2) { "Only participants can accept match" }
        require(status == MatchStatus.PENDING) { "Can only accept pending matches" }

        val updated = copy(
            status = MatchStatus.ACCEPTED,
            respondedAt = Clock.System.now(),
            updatedAt = Clock.System.now(),
            version = version + 1
        )

        updated.addDomainEvent(MatchAccepted(
            aggregateId = id.toString(),
            matchId = id.toString(),
            acceptedBy = userId.toString()
        ))

        return updated
    }

    fun reject(userId: EntityId): MatchAggregate {
        require(userId == userId1 || userId == userId2) { "Only participants can reject match" }
        require(status == MatchStatus.PENDING) { "Can only reject pending matches" }

        val updated = copy(
            status = MatchStatus.REJECTED,
            respondedAt = Clock.System.now(),
            updatedAt = Clock.System.now(),
            version = version + 1
        )

        updated.addDomainEvent(MatchRejected(
            aggregateId = id.toString(),
            matchId = id.toString(),
            rejectedBy = userId.toString()
        ))

        return updated
    }
}

enum class MatchStatus {
    PENDING,
    ACCEPTED,
    REJECTED,
    EXPIRED
}
