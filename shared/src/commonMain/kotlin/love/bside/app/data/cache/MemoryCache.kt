package love.bside.app.data.cache

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration

/**
 * In-memory LRU cache with TTL (Time-To-Live) support
 * 
 * Features:
 * - LRU eviction when max size reached
 * - TTL-based expiration
 * - Thread-safe operations
 * - Automatic cleanup of expired entries
 * 
 * @param maxSize Maximum number of entries
 * @param defaultTtl Default time-to-live for entries
 */
class MemoryCache<K : Any, V : Any>(
    private val maxSize: Int,
    private val defaultTtl: Duration
) {
    private val mutex = Mutex()
    private val cache = mutableMapOf<K, CacheEntry<V>>()
    private val accessOrder = mutableListOf<K>() // For LRU tracking
    
    data class CacheEntry<V>(
        val value: V,
        val expiresAt: Instant
    ) {
        fun isExpired(): Boolean = Clock.System.now() > expiresAt
    }
    
    /**
     * Gets a value from cache, returns null if not found or expired
     */
    suspend fun get(key: K): V? = mutex.withLock {
        val entry = cache[key] ?: return null
        
        // Check if expired
        if (entry.isExpired()) {
            cache.remove(key)
            accessOrder.remove(key)
            return null
        }
        
        // Update access order (move to end for LRU)
        accessOrder.remove(key)
        accessOrder.add(key)
        
        return entry.value
    }
    
    /**
     * Puts a value in cache with default TTL
     */
    suspend fun put(key: K, value: V) = put(key, value, defaultTtl)
    
    /**
     * Puts a value in cache with custom TTL
     */
    suspend fun put(key: K, value: V, ttl: Duration) = mutex.withLock {
        // Check if we need to evict
        if (cache.size >= maxSize && !cache.containsKey(key)) {
            evictLru()
        }
        
        val expiresAt = Clock.System.now() + ttl
        cache[key] = CacheEntry(value, expiresAt)
        
        // Update access order
        accessOrder.remove(key)
        accessOrder.add(key)
    }
    
    /**
     * Removes a value from cache
     */
    suspend fun remove(key: K) = mutex.withLock {
        cache.remove(key)
        accessOrder.remove(key)
    }
    
    /**
     * Clears all entries from cache
     */
    suspend fun clear() = mutex.withLock {
        cache.clear()
        accessOrder.clear()
    }
    
    /**
     * Gets cache size
     */
    suspend fun size(): Int = mutex.withLock {
        cache.size
    }
    
    /**
     * Checks if key exists and is not expired
     */
    suspend fun contains(key: K): Boolean = mutex.withLock {
        val entry = cache[key] ?: return false
        return !entry.isExpired()
    }
    
    /**
     * Cleans up expired entries
     */
    suspend fun cleanupExpired() = mutex.withLock {
        val expiredKeys = cache.entries
            .filter { it.value.isExpired() }
            .map { it.key }
        
        expiredKeys.forEach { key ->
            cache.remove(key)
            accessOrder.remove(key)
        }
    }
    
    /**
     * Evicts least recently used entry
     */
    private fun evictLru() {
        if (accessOrder.isEmpty()) return
        
        val lruKey = accessOrder.first()
        cache.remove(lruKey)
        accessOrder.removeAt(0)
    }
    
    /**
     * Gets cache statistics (for monitoring)
     */
    suspend fun getStats(): CacheStats = mutex.withLock {
        val expired = cache.count { it.value.isExpired() }
        CacheStats(
            size = cache.size,
            maxSize = maxSize,
            expiredCount = expired
        )
    }
}

/**
 * Cache statistics data class
 */
data class CacheStats(
    val size: Int,
    val maxSize: Int,
    val expiredCount: Int
)
