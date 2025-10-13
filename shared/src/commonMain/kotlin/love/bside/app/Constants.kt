package love.bside.app

/**
 * Application-wide constants
 */
object AppConstants {
    const val SERVER_PORT = 8080
    
    // API Configuration
    const val API_VERSION = "v1"
    const val DEFAULT_PAGE_SIZE = 20
    const val MAX_PAGE_SIZE = 100
    
    // Cache Keys
    const val CACHE_PREFIX = "bside_"
    
    // Storage Keys
    const val STORAGE_TOKEN_KEY = "auth_token"
    const val STORAGE_REFRESH_TOKEN_KEY = "refresh_token"
    const val STORAGE_USER_ID_KEY = "user_id"
    
    // Validation Constants
    const val MIN_PASSWORD_LENGTH = 8
    const val MAX_PASSWORD_LENGTH = 128
    const val MAX_USERNAME_LENGTH = 50
    const val MAX_BIO_LENGTH = 500
    
    // Timeouts
    const val DEFAULT_TIMEOUT_MS = 30_000L
    const val UPLOAD_TIMEOUT_MS = 60_000L
    
    // Feature Flags Default
    const val DEFAULT_ENABLE_ANALYTICS = true
    const val DEFAULT_ENABLE_CRASH_REPORTING = true
}

// Legacy support
const val SERVER_PORT = AppConstants.SERVER_PORT