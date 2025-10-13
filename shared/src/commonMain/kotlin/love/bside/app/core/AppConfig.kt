package love.bside.app.core

/**
 * Configuration management for the application.
 * Allows environment-specific settings and feature flags.
 */
data class AppConfig(
    val apiBaseUrl: String,
    val apiTimeout: Long = 30_000L,
    val maxRetryAttempts: Int = 3,
    val retryDelayMs: Long = 1000L,
    val enableLogging: Boolean = true,
    val enableAnalytics: Boolean = true,
    val cacheEnabled: Boolean = true,
    val cacheDurationMs: Long = 300_000L, // 5 minutes
    val environment: Environment = Environment.PRODUCTION,
    val features: FeatureFlags = FeatureFlags()
)

enum class Environment {
    DEVELOPMENT,
    STAGING,
    PRODUCTION
}

data class FeatureFlags(
    val enableBiometricAuth: Boolean = false,
    val enableOfflineMode: Boolean = false,
    val enablePushNotifications: Boolean = false,
    val enableDarkMode: Boolean = true,
    val enableDebugMenu: Boolean = false,
    val maxFileUploadSizeMb: Int = 10
)

/**
 * Global configuration instance
 */
expect object ConfigProvider {
    fun getConfig(): AppConfig
}

/**
 * Helper function to access config
 */
fun appConfig(): AppConfig = ConfigProvider.getConfig()
