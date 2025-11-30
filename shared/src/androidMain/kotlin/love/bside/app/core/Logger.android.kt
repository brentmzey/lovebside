package love.bside.app.core

import android.util.Log

actual object AppLogger : Logger {
    private val fallbackLogger: Logger = ConsoleLogger()

    actual override fun debug(tag: String, message: String, throwable: Throwable?) {
        logWithFallback({ fallbackLogger.debug(tag, message, throwable) }) {
            Log.d(tag, message, throwable)
        }
    }

    actual override fun info(tag: String, message: String, throwable: Throwable?) {
        logWithFallback({ fallbackLogger.info(tag, message, throwable) }) {
            Log.i(tag, message, throwable)
        }
    }

    actual override fun warn(tag: String, message: String, throwable: Throwable?) {
        logWithFallback({ fallbackLogger.warn(tag, message, throwable) }) {
            Log.w(tag, message, throwable)
        }
    }

    actual override fun error(tag: String, message: String, throwable: Throwable?) {
        logWithFallback({ fallbackLogger.error(tag, message, throwable) }) {
            Log.e(tag, message, throwable)
        }
    }

    private inline fun logWithFallback(fallback: () -> Unit, androidLogger: () -> Unit) {
        try {
            androidLogger()
        } catch (notMocked: RuntimeException) {
            val shouldFallback = notMocked.message?.contains("not mocked", ignoreCase = true) == true
            if (shouldFallback) {
                fallback()
            } else {
                throw notMocked
            }
        }
    }
}
