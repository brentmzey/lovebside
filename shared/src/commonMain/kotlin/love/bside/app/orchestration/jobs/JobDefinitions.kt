package love.bside.app.orchestration.jobs

import kotlinx.datetime.Clock
import love.bside.app.orchestration.events.*

/**
 * Pre-defined jobs for B-Side application
 */
object JobDefinitions {
    
    fun syncMessages(userId: String) = Job(
        id = "sync_messages_$userId",
        name = "Sync Messages",
        type = JobType.PERIODIC,
        priority = JobPriority.HIGH,
        constraints = JobConstraints(requiresNetwork = true),
        payload = mapOf("userId" to userId)
    )
    
    fun syncMatches(userId: String) = Job(
        id = "sync_matches_$userId",
        name = "Sync Matches",
        type = JobType.PERIODIC,
        priority = JobPriority.NORMAL,
        constraints = JobConstraints(requiresNetwork = true),
        payload = mapOf("userId" to userId)
    )
    
    fun uploadMedia(mediaId: String, path: String) = Job(
        id = "upload_media_$mediaId",
        name = "Upload Media",
        type = JobType.ONE_TIME,
        priority = JobPriority.HIGH,
        constraints = JobConstraints(requiresNetwork = true),
        payload = mapOf("mediaId" to mediaId, "path" to path)
    )
    
    fun cleanupCache() = Job(
        id = "cleanup_cache",
        name = "Cleanup Cache",
        type = JobType.PERIODIC,
        priority = JobPriority.BACKGROUND,
        constraints = JobConstraints(requiresDeviceIdle = true)
    )
    
    fun calculateMatchScores(userId: String) = Job(
        id = "calculate_scores_$userId",
        name = "Calculate Match Scores",
        type = JobType.ONE_TIME,
        priority = JobPriority.NORMAL,
        constraints = JobConstraints(requiresNetwork = false)
    )
    
    fun sendPushNotification(userId: String, message: String) = Job(
        id = "push_notification_${Clock.System.now().toEpochMilliseconds()}",
        name = "Send Push Notification",
        type = JobType.IMMEDIATE,
        priority = JobPriority.CRITICAL,
        payload = mapOf("userId" to userId, "message" to message)
    )
}

/**
 * Maps domain events to background jobs
 */
class EventToJobMapper(private val scheduler: JobScheduler) {
    
    suspend fun handleEvent(event: DomainEvent) {
        when (event) {
            is MessageSent -> {
                // Trigger sync job
                scheduler.scheduleJob(JobDefinitions.syncMessages(event.senderId))
            }
            is QuestionnaireCompleted -> {
                // Calculate new matches
                scheduler.scheduleJob(JobDefinitions.calculateMatchScores(event.userId))
            }
            is UserProfileUpdated -> {
                // Re-calculate compatibility scores
                scheduler.scheduleJob(JobDefinitions.calculateMatchScores(event.userId))
            }
            // Add more mappings as needed
        }
    }
}
