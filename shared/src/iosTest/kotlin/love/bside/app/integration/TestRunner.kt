package love.bside.app.integration

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import kotlinx.coroutines.runBlocking
import platform.posix.getenv

/**
 * iOS implementation of test runner
 */
actual fun runTest(block: suspend () -> Unit) {
    if (!shouldRunIosIntegrationTests()) {
        println("[iOS] Skipping integration test - set RUN_IOS_INTEGRATION_TESTS=true to enable")
        return
    }

    runBlocking {
        block()
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun shouldRunIosIntegrationTests(): Boolean {
    val rawValue = getenv("RUN_IOS_INTEGRATION_TESTS")?.toKString() ?: return false
    return rawValue.equals("true", ignoreCase = true)
}
