package love.bside.app.orchestration.health

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * System health monitor.
 * Tracks health of database, API, network, and services.
 */
class HealthMonitor(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) {
    private val _health = MutableStateFlow<SystemHealth>(SystemHealth.unknown())
    val health: StateFlow<SystemHealth> = _health.asStateFlow()

    private val checks = mutableMapOf<String, HealthCheck>()
    private var isRunning = false

    fun registerCheck(name: String, check: HealthCheck) {
        checks[name] = check
    }

    fun start(intervalMs: Long = 60_000) {
        if (isRunning) {
            return
        }
        isRunning = true

        scope.launch {
            while (isActive) {
                performHealthCheck()
                delay(intervalMs)
            }
        }
    }

    fun stop() {
        isRunning = false
    }

    private suspend fun performHealthCheck() {
        val results = mutableMapOf<String, HealthStatus>()
        
        for ((name, check) in checks) {
            results[name] = try {
                check.check()
            } catch (e: Exception) {
                HealthStatus.Unhealthy(e.message ?: "Check failed")
            }
        }

        val overallStatus = when {
            results.values.all { it is HealthStatus.Healthy } -> HealthStatus.Healthy()
            results.values.any { it is HealthStatus.Unhealthy } -> HealthStatus.Unhealthy("Some checks failed")
            else -> HealthStatus.Degraded("Some checks degraded")
        }

        _health.value = SystemHealth(
            status = overallStatus,
            checks = results,
            timestamp = Clock.System.now()
        )
    }

    suspend fun checkNow(): SystemHealth {
        performHealthCheck()
        return _health.value
    }
}

data class SystemHealth(
    val status: HealthStatus,
    val checks: Map<String, HealthStatus>,
    val timestamp: Instant
) {
    companion object {
        fun unknown() = SystemHealth(
            status = HealthStatus.Unknown,
            checks = emptyMap(),
            timestamp = Clock.System.now()
        )
    }
}

sealed class HealthStatus {
    data class Healthy(val message: String = "OK") : HealthStatus()
    data class Degraded(val message: String) : HealthStatus()
    data class Unhealthy(val message: String) : HealthStatus()
    data object Unknown : HealthStatus()
}

interface HealthCheck {
    suspend fun check(): HealthStatus
}

/**
 * Common health checks
 */
object HealthChecks {
    
    fun database(checkConnection: suspend () -> Boolean) = object : HealthCheck {
        override suspend fun check(): HealthStatus {
            return if (checkConnection()) {
                HealthStatus.Healthy("Database connection OK")
            } else {
                HealthStatus.Unhealthy("Cannot connect to database")
            }
        }
    }
    
    fun api(checkEndpoint: suspend () -> Boolean) = object : HealthCheck {
        override suspend fun check(): HealthStatus {
            return if (checkEndpoint()) {
                HealthStatus.Healthy("API reachable")
            } else {
                HealthStatus.Unhealthy("API unreachable")
            }
        }
    }
    
    fun network(isConnected: suspend () -> Boolean) = object : HealthCheck {
        override suspend fun check(): HealthStatus {
            return if (isConnected()) {
                HealthStatus.Healthy("Network connected")
            } else {
                HealthStatus.Unhealthy("No network connection")
            }
        }
    }
}
