package love.bside.server.migrations

import kotlinx.datetime.Instant

/**
 * Represents a database migration
 */
interface Migration {
    val version: Int
    val name: String
    val description: String
    suspend fun up(): MigrationResult
    suspend fun down(): MigrationResult
}

/**
 * Result of executing a migration
 */
sealed class MigrationResult {
    data class Success(val message: String) : MigrationResult()
    data class Failure(val error: String, val cause: Throwable? = null) : MigrationResult()
}

/**
 * Migration record for tracking applied migrations
 */
data class MigrationRecord(
    val id: String,
    val version: Int,
    val name: String,
    val appliedAt: Instant,
    val executionTimeMs: Long
)
