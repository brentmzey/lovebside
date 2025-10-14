package love.bside.app.core

actual object AppLogger : Logger {
    private val consoleLogger = ConsoleLogger()

    actual override fun debug(tag: String, message: String, throwable: Throwable?) {
        consoleLogger.debug(tag, message, throwable)
    }

    actual override fun info(tag: String, message: String, throwable: Throwable?) {
        consoleLogger.info(tag, message, throwable)
    }

    actual override fun warn(tag: String, message: String, throwable: Throwable?) {
        consoleLogger.warn(tag, message, throwable)
    }

    actual override fun error(tag: String, message: String, throwable: Throwable?) {
        consoleLogger.error(tag, message, throwable)
    }
}
