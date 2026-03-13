package love.bside.app.orchestration.jobs

/**
 * JVM/Desktop-specific job scheduler
 */
actual object JobSchedulerFactory {
    actual fun create(): JobScheduler {
        // On JVM, could use Quartz or similar
        return InMemoryJobScheduler()
    }
}
