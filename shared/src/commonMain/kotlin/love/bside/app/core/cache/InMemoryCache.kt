package love.bside.app.core.cache

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import love.bside.app.core.appConfig

/**
 * Simple in-memory cache with TTL support
 */
class InMemoryCache<K, V> {
    private data class CacheEntry<V>(
        val value: V,
        val timestamp: Instant,
        val ttlMs: Long
    ) {
        fun isExpired(): Boolean {
            val now = Clock.System.now()
            return (now.toEpochMilliseconds() - timestamp.toEpochMilliseconds()) > ttlMs
        }
    }

    private val cache = mutableMapOf<K, CacheEntry<V>>()

    /**
     * Stores a value in cache with optional TTL
     */
    fun put(key: K, value: V, ttlMs: Long = appConfig().cacheDurationMs) {
        cache[key] = CacheEntry(value, Clock.System.now(), ttlMs)
    }

    /**
     * Retrieves a value from cache if not expired
     */
    fun get(key: K): V? {
        val entry = cache[key] ?: return null
        return if (entry.isExpired()) {
            cache.remove(key)
            null
        } else {
            entry.value
        }
    }

    /**
     * Removes a value from cache
     */
    fun remove(key: K) {
        cache.remove(key)
    }

    /**
     * Clears all cache entries
     */
    fun clear() {
        cache.clear()
    }

    /**
     * Removes all expired entries
     */
    fun cleanup() {
        val expiredKeys = cache.filterValues { it.isExpired() }.keys
        expiredKeys.forEach { cache.remove(it) }
    }

    /**
     * Gets or puts a value in cache
     */
    suspend fun getOrPut(key: K, ttlMs: Long = appConfig().cacheDurationMs, provider: suspend () -> V): V {
        return get(key) ?: provider().also { put(key, it, ttlMs) }
    }
}

/**
 * Cache key builder for consistent keys
 */
object CacheKeys {
    fun userProfile(userId: String) = "user_profile_$userId"
    fun matches(userId: String) = "matches_$userId"
    fun values(userId: String) = "values_$userId"
    fun questionnaire(userId: String) = "questionnaire_$userId"
    fun allKeyValues() = "all_key_values"
}
