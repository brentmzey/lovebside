package love.bside.app.core

/**
 * Centralized logging interface for the application.
 * Platform implementations can integrate with native logging systems.
 */
interface Logger {
    fun debug(tag: String, message: String, throwable: Throwable? = null)
    fun info(tag: String, message: String, throwable: Throwable? = null)
    fun warn(tag: String, message: String, throwable: Throwable? = null)
    fun error(tag: String, message: String, throwable: Throwable? = null)
}

/**
 * Default console logger implementation
 */
class ConsoleLogger : Logger {
    override fun debug(tag: String, message: String, throwable: Throwable?) {
        log("DEBUG", tag, message, throwable)
    }

    override fun info(tag: String, message: String, throwable: Throwable?) {
        log("INFO", tag, message, throwable)
    }

    override fun warn(tag: String, message: String, throwable: Throwable?) {
        log("WARN", tag, message, throwable)
    }

    override fun error(tag: String, message: String, throwable: Throwable?) {
        log("ERROR", tag, message, throwable)
    }

    private fun log(level: String, tag: String, message: String, throwable: Throwable?) {
        println("[$level] $tag: $message")
        throwable?.let {
            println("  Exception: ${it.message}")
            it.printStackTrace()
        }
    }
}

/**
 * Global logger instance - platform-specific implementation
 */
expect object AppLogger : Logger {
    override fun debug(tag: String, message: String, throwable: Throwable?)
    override fun info(tag: String, message: String, throwable: Throwable?)
    override fun warn(tag: String, message: String, throwable: Throwable?)
    override fun error(tag: String, message: String, throwable: Throwable?)
}

/**
 * Extension functions for easy logging
 */
fun Any.logDebug(message: String, throwable: Throwable? = null) {
    AppLogger.debug(this::class.simpleName ?: "Unknown", message, throwable)
}

fun Any.logInfo(message: String, throwable: Throwable? = null) {
    AppLogger.info(this::class.simpleName ?: "Unknown", message, throwable)
}

fun Any.logWarn(message: String, throwable: Throwable? = null) {
    AppLogger.warn(this::class.simpleName ?: "Unknown", message, throwable)
}

fun Any.logError(message: String, throwable: Throwable? = null) {
    AppLogger.error(this::class.simpleName ?: "Unknown", message, throwable)
}
