package love.bside.app.orchestration.jobs

/**
 * iOS-specific job scheduler using Background Tasks
 */
actual object JobSchedulerFactory {
    actual fun create(): JobScheduler {
        // On iOS, would use BGTaskScheduler
        // For now, return in-memory implementation
        return InMemoryJobScheduler()
    }
}
