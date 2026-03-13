package love.bside.app.orchestration.jobs

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import love.bside.app.core.Result

/**
 * Multiplatform job scheduler for background tasks.
 * Platform implementations handle actual scheduling (WorkManager, etc.)
 */
interface JobScheduler {
    suspend fun initialize()
    suspend fun scheduleJob(job: Job): Result<String>
    suspend fun cancelJob(jobId: String): Result<Unit>
    suspend fun cancelAll()
    suspend fun resumeAll()
    suspend fun pauseNonCritical()
    fun getJobStatus(jobId: String): Flow<JobStatus>
}

/**
 * Job definition
 */
data class Job(
    val id: String,
    val name: String,
    val type: JobType,
    val priority: JobPriority = JobPriority.NORMAL,
    val constraints: JobConstraints = JobConstraints(),
    val retryPolicy: RetryPolicy = RetryPolicy.default(),
    val payload: Map<String, String> = emptyMap(),
    val scheduledAt: Instant = Clock.System.now()
)

enum class JobType {
    ONE_TIME,
    PERIODIC,
    IMMEDIATE
}

enum class JobPriority {
    CRITICAL,   // Must run ASAP
    HIGH,       // Important but can wait
    NORMAL,     // Regular priority
    LOW,        // Can be deferred
    BACKGROUND  // Run when idle
}

data class JobConstraints(
    val requiresNetwork: Boolean = false,
    val requiresCharging: Boolean = false,
    val requiresDeviceIdle: Boolean = false,
    val minimumBatteryLevel: Int? = null
)

data class RetryPolicy(
    val maxAttempts: Int = 3,
    val backoffDelayMs: Long = 1000,
    val backoffMultiplier: Double = 2.0
) {
    companion object {
        fun default() = RetryPolicy()
        fun immediate() = RetryPolicy(maxAttempts = 1, backoffDelayMs = 0)
        fun aggressive() = RetryPolicy(maxAttempts = 5, backoffDelayMs = 500, backoffMultiplier = 1.5)
    }
}

sealed class JobStatus {
    data object Pending : JobStatus()
    data object Running : JobStatus()
    data class Success(val completedAt: Instant) : JobStatus()
    data class Failed(val error: String, val attempt: Int) : JobStatus()
    data object Cancelled : JobStatus()
}

/**
 * Base implementation with in-memory job queue
 */
class InMemoryJobScheduler(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) : JobScheduler {
    private val jobs = mutableMapOf<String, Job>()
    private val statuses = mutableMapOf<String, MutableStateFlow<JobStatus>>()
    private val activeJobs = mutableSetOf<String>()

    override suspend fun initialize() {
        // Setup job processing
    }

    override suspend fun scheduleJob(job: Job): Result<String> {
        jobs[job.id] = job
        statuses[job.id] = MutableStateFlow(JobStatus.Pending)
        
        // Execute based on type
        when (job.type) {
            JobType.IMMEDIATE -> executeJob(job)
            JobType.ONE_TIME -> scheduleDelayedExecution(job)
            JobType.PERIODIC -> schedulePeriodicExecution(job)
        }
        
        return Result.Success(job.id)
    }

    override suspend fun cancelJob(jobId: String): Result<Unit> {
        jobs.remove(jobId)
        statuses[jobId]?.value = JobStatus.Cancelled
        activeJobs.remove(jobId)
        return Result.Success(Unit)
    }

    override suspend fun cancelAll() {
        jobs.clear()
        statuses.values.forEach { it.value = JobStatus.Cancelled }
        activeJobs.clear()
    }

    override suspend fun resumeAll() {
        jobs.values.forEach { job ->
            if (job.id !in activeJobs && statuses[job.id]?.value is JobStatus.Pending) {
                executeJob(job)
            }
        }
    }

    override suspend fun pauseNonCritical() {
        jobs.values.filter { it.priority != JobPriority.CRITICAL }.forEach { job ->
            if (job.id in activeJobs) {
                // Pause execution
            }
        }
    }

    override fun getJobStatus(jobId: String): Flow<JobStatus> {
        return statuses[jobId]?.asStateFlow() ?: flowOf(JobStatus.Cancelled)
    }

    private fun executeJob(job: Job) {
        scope.launch {
            activeJobs.add(job.id)
            statuses[job.id]?.value = JobStatus.Running
            
            try {
                // Execute job logic here
                delay(1000) // Placeholder
                statuses[job.id]?.value = JobStatus.Success(Clock.System.now())
            } catch (e: Exception) {
                statuses[job.id]?.value = JobStatus.Failed(e.message ?: "Unknown error", 1)
            } finally {
                activeJobs.remove(job.id)
            }
        }
    }

    private fun scheduleDelayedExecution(job: Job) {
        scope.launch {
            delay(5000) // Example delay
            executeJob(job)
        }
    }

    private fun schedulePeriodicExecution(job: Job) {
        scope.launch {
            while (job.id in jobs) {
                executeJob(job)
                delay(60000) // Every minute
            }
        }
    }
}

/**
 * Platform-specific factory
 */
expect object JobSchedulerFactory {
    fun create(): JobScheduler
}
