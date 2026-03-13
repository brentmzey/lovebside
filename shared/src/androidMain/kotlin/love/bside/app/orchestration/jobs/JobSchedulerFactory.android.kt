package love.bside.app.orchestration.jobs

/**
 * Android-specific job scheduler using WorkManager
 */
actual object JobSchedulerFactory {
    actual fun create(): JobScheduler {
        // On Android, would use WorkManager
        // For now, return in-memory implementation
        return InMemoryJobScheduler()
    }
}
