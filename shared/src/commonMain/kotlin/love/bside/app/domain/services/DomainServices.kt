package love.bside.app.domain.services

import love.bside.app.core.EntityId
import love.bside.app.core.Result
import love.bside.app.domain.aggregates.MatchAggregate
import love.bside.app.domain.aggregates.UserAggregate
import love.bside.app.domain.core.DomainService
import love.bside.app.orchestration.events.EventBus
import love.bside.app.orchestration.events.MatchCreated

/**
 * Domain service for matching logic.
 * Calculates compatibility scores based on questionnaire responses and values.
 */
class MatchingService(
    private val eventBus: EventBus
) : DomainService {

    suspend fun calculateCompatibility(user1: UserAggregate, user2: UserAggregate): Double {
        // TODO: Implement actual compatibility algorithm
        // This would typically involve:
        // 1. Comparing questionnaire responses
        // 2. Analyzing value alignments
        // 3. Weighting different factors
        // 4. Normalizing to 0-100 scale
        
        return 75.0 // Placeholder
    }

    suspend fun createMatch(
        user1: UserAggregate,
        user2: UserAggregate,
        score: Double
    ): Result<MatchAggregate> {
        val match = MatchAggregate(
            id = love.bside.app.core.UuidUtils.random(),
            userId1 = user1.id,
            userId2 = user2.id,
            compatibilityScore = score
        )

        eventBus.publish(MatchCreated(
            aggregateId = match.id.toString(),
            matchId = match.id.toString(),
            userId1 = user1.id.toString(),
            userId2 = user2.id.toString(),
            score = score
        ))

        return Result.Success(match)
    }

    fun isEligibleForMatch(user1: UserAggregate, user2: UserAggregate): Boolean {
        return user1.isActive && 
               user2.isActive && 
               user1.emailVerified && 
               user2.emailVerified &&
               user1.id != user2.id
    }
}

/**
 * Domain service for conversation management
 */
class ConversationService : DomainService {
    
    fun canStartConversation(user1Id: EntityId, user2Id: EntityId, hasActiveMatch: Boolean): Boolean {
        return user1Id != user2Id && hasActiveMatch
    }

    fun generateConversationName(participantNames: List<String>): String? {
        return when {
            participantNames.size == 2 -> null // 1-1 conversation doesn't need name
            participantNames.size <= 4 -> participantNames.joinToString(", ")
            else -> "${participantNames.take(3).joinToString(", ")}, +${participantNames.size - 3} others"
        }
    }
}

/**
 * Domain service for notification logic
 */
class NotificationService(
    private val eventBus: EventBus
) : DomainService {

    suspend fun shouldNotifyUser(
        userId: EntityId,
        notificationType: NotificationType,
        userPreferences: NotificationPreferences
    ): Boolean {
        return when (notificationType) {
            NotificationType.NEW_MESSAGE -> userPreferences.enableMessageNotifications
            NotificationType.NEW_MATCH -> userPreferences.enableMatchNotifications
            NotificationType.MATCH_RESPONSE -> userPreferences.enableMatchNotifications
        }
    }
}

enum class NotificationType {
    NEW_MESSAGE,
    NEW_MATCH,
    MATCH_RESPONSE
}

data class NotificationPreferences(
    val enableMessageNotifications: Boolean = true,
    val enableMatchNotifications: Boolean = true,
    val enablePushNotifications: Boolean = true,
    val quietHoursStart: Int? = null,
    val quietHoursEnd: Int? = null
)
