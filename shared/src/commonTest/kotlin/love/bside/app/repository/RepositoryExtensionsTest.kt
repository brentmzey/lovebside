package love.bside.app.repository

import love.bside.app.core.Result
import love.bside.app.data.repository.Page
import love.bside.app.data.repository.RepositoryCache
import love.bside.app.data.repository.retryWithBackoff
import love.bside.app.data.repository.safeRepositoryCall
import love.bside.app.data.repository.withCache
import love.bside.app.integration.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Tests for repository extension functions and helpers.
 */
class RepositoryExtensionsTest {

    @Test
    fun `cache stores retrieves and invalidates values`() {
        val cache = RepositoryCache<String, String>()
        cache.put("key1", "value1")
        assertEquals("value1", cache.get("key1"))

        cache.invalidate("key1")
        assertEquals(null, cache.get("key1"))

        cache.put("key2", "value2")
        cache.clear()
        assertEquals(0, cache.size())
    }

    @Test
    fun `withCache reuses cached value without reloading`() = runTest {
        val cache = RepositoryCache<String, Int>()
        var loadCount = 0

        val first = withCache(cache, key = "numbers") {
            loadCount++
            42
        }
        val second = withCache(cache, key = "numbers") {
            loadCount++
            100
        }

        assertEquals(42, first)
        assertEquals(42, second)
        assertEquals(1, loadCount)
    }

    @Test
    fun `safe repository call wraps successful result`() = runTest {
        val result = safeRepositoryCall { "success" }
        assertIs<Result.Success<String>>(result)
        assertEquals("success", result.data)
    }

    @Test
    fun `safe repository call wraps thrown exception`() = runTest {
        val result = safeRepositoryCall<String> { error("boom") }
        assertIs<Result.Error>(result)
        assertTrue(result.exception.message!!.contains("boom"))
    }

    @Test
    fun `retryWithBackoff retries until success`() = runTest {
        var attempts = 0
        val value = retryWithBackoff(maxRetries = 5) {
            attempts++
            if (attempts < 3) {
                throw IllegalStateException("not yet")
            }
            "done"
        }

        assertEquals("done", value)
        assertEquals(3, attempts)
    }

    @Test
    fun `retryWithBackoff stops when shouldRetry returns false`() = runTest {
        var attempts = 0
        assertFailsWith<IllegalArgumentException> {
            retryWithBackoff(
                maxRetries = 5,
                shouldRetry = { false } // short circuit after first failure
            ) {
                attempts++
                throw IllegalArgumentException("permanent failure")
            }
        }
        assertEquals(1, attempts)
    }

    @Test
    fun `pagination calculates metadata correctly`() {
        val page = Page.create(
            items = (1..10).toList(),
            page = 2,
            pageSize = 10,
            totalItems = 25
        )

        assertEquals(3, page.totalPages)
        assertTrue(page.hasNext)
        assertTrue(page.hasPrevious)
        assertEquals(2, page.page)
        assertEquals(10, page.items.size)
    }
}
