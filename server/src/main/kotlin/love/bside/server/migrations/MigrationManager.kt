package love.bside.server.migrations

import love.bside.server.config.ServerConfig
import kotlinx.datetime.Clock
import love.bside.app.core.logInfo
import love.bside.app.core.logError

/**
 * Manages database migrations for PocketBase schema
 * 
 * This tool helps ensure the PocketBase database schema is up-to-date
 * and provides a way to version control schema changes.
 * 
 * Usage:
 * ```
 * val migrator = MigrationManager(config)
 * migrator.runPendingMigrations()
 * ```
 */
class MigrationManager(
    private val config: ServerConfig,
    private val migrations: List<Migration> = defaultMigrations
) {
    private val appliedMigrations = mutableMapOf<Int, MigrationRecord>()
    
    /**
     * Run all pending migrations
     */
    suspend fun runPendingMigrations(): List<MigrationResult> {
        logInfo("Starting migration check...")
        
        // Load already applied migrations
        loadAppliedMigrations()
        
        val results = mutableListOf<MigrationResult>()
        val sortedMigrations = migrations.sortedBy { it.version }
        
        for (migration in sortedMigrations) {
            if (!isApplied(migration.version)) {
                logInfo("Applying migration v${migration.version}: ${migration.name}")
                
                var executionTime: Long = 0
                val result = try {
                    val startTime = Clock.System.now()
                    val migrationResult = migration.up()
                    val endTime = Clock.System.now()
                    executionTime = (endTime - startTime).inWholeMilliseconds
                    migrationResult
                } catch (e: Exception) {
                    logError("Migration v${migration.version} failed: ${e.message}")
                    MigrationResult.Failure("Migration failed", e)
                }
                
                results.add(result)
                
                if (result is MigrationResult.Success) {
                    recordMigration(migration, executionTime)
                    logInfo("Migration v${migration.version} completed successfully")
                }
            } else {
                logInfo("Migration v${migration.version} already applied, skipping")
            }
        }
        
        if (results.isEmpty()) {
            logInfo("No pending migrations")
        } else {
            logInfo("Applied ${results.size} migration(s)")
        }
        
        return results
    }
    
    /**
     * Rollback the last migration
     */
    suspend fun rollbackLast(): MigrationResult {
        val lastMigration = appliedMigrations.values.maxByOrNull { it.version }
            ?: return MigrationResult.Failure("No migrations to rollback")
        
        val migration = migrations.find { it.version == lastMigration.version }
            ?: return MigrationResult.Failure("Migration v${lastMigration.version} not found")
        
        logInfo("Rolling back migration v${migration.version}: ${migration.name}")
        
        return try {
            val result = migration.down()
            if (result is MigrationResult.Success) {
                appliedMigrations.remove(migration.version)
                logInfo("Rollback completed successfully")
            }
            result
        } catch (e: Exception) {
            logError("Rollback failed: ${e.message}")
            MigrationResult.Failure("Rollback failed", e)
        }
    }
    
    /**
     * Get migration status
     */
    fun getStatus(): MigrationStatus {
        val total = migrations.size
        val applied = appliedMigrations.size
        val pending = total - applied
        
        return MigrationStatus(
            totalMigrations = total,
            appliedMigrations = applied,
            pendingMigrations = pending,
            appliedVersions = appliedMigrations.keys.sorted(),
            pendingVersions = migrations.map { it.version }.filterNot { isApplied(it) }.sorted()
        )
    }
    
    /**
     * Generate a new migration template
     */
    fun generateMigration(name: String): String {
        val nextVersion = (migrations.maxOfOrNull { it.version } ?: 0) + 1
        val className = name.split("_").joinToString("") { it.capitalize() }
        
        return """
package love.bside.server.migrations.versions

import love.bside.server.migrations.Migration
import love.bside.server.migrations.MigrationResult

/**
 * Migration v$nextVersion: $name
 * 
 * Generated: ${Clock.System.now()}
 */
class Migration${nextVersion}_$className : Migration {
    override val version = $nextVersion
    override val name = "$name"
    override val description = ""
    
    override suspend fun up(): MigrationResult {
        return try {
            // TODO: Implement migration up logic
            // Example:
            // - Create new collection
            // - Add new field to collection
            // - Update indexes
            // - Migrate data
            
            MigrationResult.Success("Migration v$nextVersion completed")
        } catch (e: Exception) {
            MigrationResult.Failure("Migration failed: ${'$'}{e.message}", e)
        }
    }
    
    override suspend fun down(): MigrationResult {
        return try {
            // TODO: Implement rollback logic
            
            MigrationResult.Success("Rollback v$nextVersion completed")
        } catch (e: Exception) {
            MigrationResult.Failure("Rollback failed: ${'$'}{e.message}", e)
        }
    }
}
        """.trimIndent()
    }
    
    private fun isApplied(version: Int): Boolean {
        return appliedMigrations.containsKey(version)
    }
    
    private fun loadAppliedMigrations() {
        // In a production system, this would load from a migrations table in PocketBase
        // For now, we'll use in-memory tracking
        // TODO: Implement persistent migration tracking
    }
    
    private fun recordMigration(migration: Migration, executionTimeMs: Long) {
        appliedMigrations[migration.version] = MigrationRecord(
            id = "migration_${migration.version}",
            version = migration.version,
            name = migration.name,
            appliedAt = Clock.System.now(),
            executionTimeMs = executionTimeMs
        )
        // TODO: Persist to PocketBase migrations collection
    }
}

/**
 * Status of database migrations
 */
data class MigrationStatus(
    val totalMigrations: Int,
    val appliedMigrations: Int,
    val pendingMigrations: Int,
    val appliedVersions: List<Int>,
    val pendingVersions: List<Int>
)

/**
 * Default migrations list
 * Add new migrations here in order
 */
val defaultMigrations: List<Migration> = listOf(
    love.bside.server.migrations.versions.Migration1_InitialSchema()
)
