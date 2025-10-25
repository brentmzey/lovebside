package love.bside.server.migrations

import kotlinx.cli.*
import kotlinx.coroutines.runBlocking
import love.bside.server.config.ServerConfig
import love.bside.app.core.logInfo

/**
 * Command-line tool for managing database migrations
 * 
 * Usage:
 * ```bash
 * # Run pending migrations
 * ./gradlew :server:runMigrations
 * 
 * # Generate a new migration
 * ./gradlew :server:runMigrations --args="generate add_user_photos"
 * 
 * # Rollback last migration
 * ./gradlew :server:runMigrations --args="rollback"
 * 
 * # Check migration status
 * ./gradlew :server:runMigrations --args="status"
 * ```
 */
fun main(args: Array<String>) = runBlocking {
    val parser = ArgParser("migrations")
    
    class RunCommand : Subcommand("run", "Run pending migrations") {
        override fun execute() {
            runBlocking {
                val manager = MigrationManager(createDummyConfig())
                val results = manager.runPendingMigrations()
                results.forEach { result ->
                    when (result) {
                        is MigrationResult.Success -> logInfo("✓ ${result.message}")
                        is MigrationResult.Failure -> logInfo("✗ ${result.error}")
                    }
                }
            }
        }
    }
    
    class GenerateCommand : Subcommand("generate", "Generate a new migration") {
        val migrationName by argument(ArgType.String, description = "Migration name (e.g., add_user_photos)")
        
        override fun execute() {
            val manager = MigrationManager(createDummyConfig())
            val code = manager.generateMigration(migrationName)
            println("\n========== Generated Migration ==========\n")
            println(code)
            println("\n=========================================\n")
            println("Save this to: server/src/main/kotlin/love/bside/server/migrations/versions/")
        }
    }
    
    class StatusCommand : Subcommand("status", "Show migration status") {
        override fun execute() {
            val manager = MigrationManager(createDummyConfig())
            val status = manager.getStatus()
            println("\n========== Migration Status ==========")
            println("Total migrations: ${status.totalMigrations}")
            println("Applied: ${status.appliedMigrations}")
            println("Pending: ${status.pendingMigrations}")
            println("Applied versions: ${status.appliedVersions.joinToString(", ")}")
            println("Pending versions: ${status.pendingVersions.joinToString(", ")}")
            println("======================================\n")
        }
    }
    
    class RollbackCommand : Subcommand("rollback", "Rollback the last migration") {
        override fun execute() {
            runBlocking {
                val manager = MigrationManager(createDummyConfig())
                val result = manager.rollbackLast()
                when (result) {
                    is MigrationResult.Success -> logInfo("✓ ${result.message}")
                    is MigrationResult.Failure -> logInfo("✗ ${result.error}")
                }
            }
        }
    }
    
    parser.subcommands(RunCommand(), GenerateCommand(), StatusCommand(), RollbackCommand())
    
    if (args.isEmpty()) {
        // Default: run migrations
        RunCommand().execute()
    } else {
        parser.parse(args)
    }
}

private fun createDummyConfig(): ServerConfig {
    return ServerConfig(
        environment = ServerConfig.Environment.DEVELOPMENT,
        jwt = ServerConfig.JwtConfig(
            secret = "test",
            issuer = "test",
            audience = "test",
            realm = "test"
        ),
        pocketbase = ServerConfig.PocketBaseConfig(
            baseUrl = "https://bside.pockethost.io"
        ),
        server = ServerConfig.HttpServerConfig(
            host = "localhost",
            port = 8080,
            allowedOrigins = listOf()
        )
    )
}
