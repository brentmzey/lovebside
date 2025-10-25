package love.bside.app.data.repository

import love.bside.app.core.AppException
import love.bside.app.core.Result
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Repository extension functions for common patterns
 * Provides retry logic, caching, and error handling utilities
 */

/**
 * Retry an operation with exponential backoff
 */
suspend fun <T> retryWithBackoff(
    maxRetries: Int = 3,
    initialDelay: Duration = 100.milliseconds,
    maxDelay: Duration = 2.seconds,
    factor: Double = 2.0,
    shouldRetry: (Throwable) -> Boolean = { true },
    operation: suspend () -> T
): T {
    var currentDelay = initialDelay
    var lastException: Throwable? = null
    
    repeat(maxRetries) { attempt ->
        try {
            return operation()
        } catch (e: Throwable) {
            lastException = e
            
            if (attempt == maxRetries - 1 || !shouldRetry(e)) {
                throw e
            }
            
            delay(currentDelay)
            currentDelay = (currentDelay * factor).coerceAtMost(maxDelay)
        }
    }
    
    throw lastException ?: IllegalStateException("Retry failed without exception")
}

/**
 * Execute operation with timeout
 */
suspend fun <T> withTimeout(
    timeout: Duration,
    operation: suspend () -> T
): Result<T> {
    return try {
        kotlinx.coroutines.withTimeout(timeout) {
            Result.Success(operation())
        }
    } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
        Result.Error(AppException.Network.Timeout(e))
    } catch (e: Exception) {
        Result.Error(AppException.Unknown(e.message ?: "Unknown error", e))
    }
}

/**
 * Simple in-memory cache for repository results
 */
class RepositoryCache<K, V>(
    private val ttlMillis: Long = 60_000L // 1 minute default
) {
    private data class CacheEntry<V>(
        val value: V,
        val timestamp: Long
    )
    
    private val cache = mutableMapOf<K, CacheEntry<V>>()
    
    fun get(key: K): V? {
        val entry = cache[key] ?: return null
        
        val now = Clock.System.now().toEpochMilliseconds()
        if (now - entry.timestamp > ttlMillis) {
            cache.remove(key)
            return null
        }
        
        return entry.value
    }
    
    fun put(key: K, value: V) {
        val now = Clock.System.now().toEpochMilliseconds()
        cache[key] = CacheEntry(value, now)
    }
    
    fun invalidate(key: K) {
        cache.remove(key)
    }
    
    fun clear() {
        cache.clear()
    }
    
    fun size(): Int = cache.size
}

/**
 * Execute with caching
 */
suspend fun <K, V> withCache(
    cache: RepositoryCache<K, V>,
    key: K,
    forceRefresh: Boolean = false,
    operation: suspend () -> V
): V {
    if (!forceRefresh) {
        cache.get(key)?.let { return it }
    }
    
    val result = operation()
    cache.put(key, result)
    return result
}

/**
 * Batch operations helper
 */
suspend fun <T, R> batchProcess(
    items: List<T>,
    batchSize: Int = 10,
    operation: suspend (List<T>) -> List<R>
): List<R> {
    return items.chunked(batchSize)
        .flatMap { batch -> operation(batch) }
}

/**
 * Safe repository operation that always returns a Result
 */
suspend fun <T> safeRepositoryCall(
    operation: suspend () -> T
): Result<T> {
    return try {
        Result.Success(operation())
    } catch (e: AppException) {
        Result.Error(e)
    } catch (e: Exception) {
        Result.Error(AppException.Unknown(e.message ?: "Unknown error occurred", e))
    }
}

/**
 * Validate and execute operation
 */
suspend fun <T> validateAndExecute(
    validation: () -> love.bside.app.core.validation.ValidationResult,
    operation: suspend () -> T
): Result<T> {
    return when (val validationResult = validation()) {
        is love.bside.app.core.validation.ValidationResult.Valid -> {
            safeRepositoryCall(operation)
        }
        is love.bside.app.core.validation.ValidationResult.Invalid -> {
            Result.Error(validationResult.exception)
        }
    }
}

/**
 * Optimistic locking helper
 */
data class VersionedEntity<T>(
    val data: T,
    val version: Int
)

suspend fun <T> withOptimisticLocking(
    entity: VersionedEntity<T>,
    maxRetries: Int = 3,
    update: suspend (T, Int) -> VersionedEntity<T>
): Result<VersionedEntity<T>> {
    var currentEntity = entity
    
    repeat(maxRetries) { attempt ->
        try {
            return Result.Success(update(currentEntity.data, currentEntity.version))
        } catch (e: ConcurrentModificationException) {
            if (attempt == maxRetries - 1) {
                return Result.Error(AppException.Unknown(
                    "Failed to update after $maxRetries attempts due to concurrent modifications"
                ))
            }
            // Fetch latest version and retry
            // In practice, would fetch from repository
            delay(50.milliseconds * (attempt + 1))
        }
    }
    
    return Result.Error(AppException.Unknown("Optimistic locking failed"))
}

/**
 * Pagination helper
 */
data class Page<T>(
    val items: List<T>,
    val page: Int,
    val pageSize: Int,
    val totalItems: Int,
    val totalPages: Int,
    val hasNext: Boolean,
    val hasPrevious: Boolean
) {
    companion object {
        fun <T> create(
            items: List<T>,
            page: Int,
            pageSize: Int,
            totalItems: Int
        ): Page<T> {
            val totalPages = (totalItems + pageSize - 1) / pageSize
            return Page(
                items = items,
                page = page,
                pageSize = pageSize,
                totalItems = totalItems,
                totalPages = totalPages,
                hasNext = page < totalPages,
                hasPrevious = page > 1
            )
        }
    }
}

/**
 * Search helper with filtering and sorting
 */
data class SearchCriteria(
    val query: String? = null,
    val filters: Map<String, Any> = emptyMap(),
    val sortBy: String? = null,
    val sortOrder: SortOrder = SortOrder.ASC,
    val page: Int = 1,
    val pageSize: Int = 20
)

enum class SortOrder {
    ASC, DESC
}

/**
 * Result mapper for transforming repository results
 */
fun <T, R> Result<T>.mapResult(transform: (T) -> R): Result<R> {
    return when (this) {
        is Result.Success -> Result.Success(transform(data))
        is Result.Error -> Result.Error(exception)
        is Result.Loading -> Result.Loading
    }
}

/**
 * Combine multiple results
 */
fun <T> combineResults(vararg results: Result<T>): Result<List<T>> {
    val data = mutableListOf<T>()
    
    for (result in results) {
        when (result) {
            is Result.Success -> data.add(result.data)
            is Result.Error -> return Result.Error(result.exception)
            is Result.Loading -> return Result.Loading
        }
    }
    
    return Result.Success(data)
}

/**
 * Transaction helper for multiple operations
 */
suspend fun <T> transaction(
    operations: suspend () -> T,
    onError: suspend (Throwable) -> Unit = {}
): Result<T> {
    return try {
        // In a real implementation, this would wrap DB transactions
        Result.Success(operations())
    } catch (e: AppException) {
        onError(e)
        Result.Error(e)
    } catch (e: Exception) {
        onError(e)
        Result.Error(AppException.Unknown(e.message ?: "Transaction failed"))
    }
}

/**
 * Conditional execution based on feature flags
 */
suspend fun <T> withFeatureFlag(
    flagName: String,
    isEnabled: () -> Boolean,
    enabledOperation: suspend () -> T,
    disabledOperation: suspend () -> T
): T {
    return if (isEnabled()) {
        enabledOperation()
    } else {
        disabledOperation()
    }
}

/**
 * Idempotency key support
 */
class IdempotencyManager {
    private val processedKeys = mutableSetOf<String>()
    
    suspend fun <T> executeIdempotent(
        key: String,
        operation: suspend () -> T
    ): Result<T> {
        if (processedKeys.contains(key)) {
            return Result.Error(AppException.Unknown(
                "Request with idempotency key '$key' has already been processed"
            ))
        }
        
        return try {
            val result = operation()
            processedKeys.add(key)
            Result.Success(result)
        } catch (e: Exception) {
            Result.Error(AppException.Unknown(e.message ?: "Operation failed", e))
        }
    }
    
    fun clear(key: String) {
        processedKeys.remove(key)
    }
}
