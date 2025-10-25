package love.bside.app.data.cache

import love.bside.app.data.models.Profile
import love.bside.app.data.models.Match
import kotlin.time.Duration.Companion.minutes

/**
 * Cache manager that provides specialized caches for different domain objects
 * with appropriate TTL and size limits
 */
class CacheManager {
    /**
     * Profile cache: 5 min TTL, 100 max entries
     * Profiles don't change often, can be cached longer
     */
    val profiles = MemoryCache<String, Profile>(
        maxSize = 100,
        defaultTtl = 5.minutes
    )
    
    /**
     * Match cache: 10 min TTL, 50 max entries
     * Matches are calculated daily, can cache for a while
     */
    val matches = MemoryCache<String, List<Match>>(
        maxSize = 50,
        defaultTtl = 10.minutes
    )
    
    /**
     * Generic cache for lists of any type
     * Used for conversations, messages, etc.
     */
    fun <T : Any> createCache(
        maxSize: Int,
        ttlMinutes: Int
    ): MemoryCache<String, List<T>> {
        return MemoryCache(
            maxSize = maxSize,
            defaultTtl = ttlMinutes.minutes
        )
    }
    
    /**
     * Auth token cache: 55 min TTL (tokens valid for 1 hour)
     * Store current user's auth token
     */
    val authTokens = MemoryCache<String, String>(
        maxSize = 1,
        defaultTtl = 55.minutes
    )
    
    /**
     * Clears all caches (useful for logout)
     */
    suspend fun clearAll() {
        profiles.clear()
        matches.clear()
        authTokens.clear()
    }
    
    /**
     * Performs cleanup on all caches (remove expired entries)
     */
    suspend fun cleanupAll() {
        profiles.cleanupExpired()
        matches.cleanupExpired()
        authTokens.cleanupExpired()
    }
    
    /**
     * Gets combined cache statistics for monitoring
     */
    suspend fun getAllStats(): Map<String, CacheStats> = mapOf(
        "profiles" to profiles.getStats(),
        "matches" to matches.getStats(),
        "authTokens" to authTokens.getStats()
    )
}
