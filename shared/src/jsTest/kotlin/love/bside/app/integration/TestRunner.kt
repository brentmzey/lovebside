package love.bside.app.integration

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.promise

/**
 * JS implementation of test runner
 */
@OptIn(DelicateCoroutinesApi::class)
actual fun runTest(block: suspend () -> Unit) {
    GlobalScope.promise {
        block()
    }
}
