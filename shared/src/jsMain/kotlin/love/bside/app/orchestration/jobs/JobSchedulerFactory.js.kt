package love.bside.app.orchestration.jobs

/**
 * JS/Web-specific job scheduler
 */
actual object JobSchedulerFactory {
    actual fun create(): JobScheduler {
        // On Web, would use Web Workers or Service Workers
        return InMemoryJobScheduler()
    }
}
