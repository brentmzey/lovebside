package love.bside.app.integration

import kotlinx.coroutines.runBlocking

/**
 * iOS implementation of test runner
 */
actual fun runTest(block: suspend () -> Unit) {
    runBlocking {
        block()
    }
}
