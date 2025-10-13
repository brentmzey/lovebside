package love.bside.app.core

actual object AppLogger {
    private val consoleLogger = ConsoleLogger()

    actual fun debug(tag: String, message: String, throwable: Throwable?) {
        consoleLogger.debug(tag, message, throwable)
    }

    actual fun info(tag: String, message: String, throwable: Throwable?) {
        consoleLogger.info(tag, message, throwable)
    }

    actual fun warn(tag: String, message: String, throwable: Throwable?) {
        consoleLogger.warn(tag, message, throwable)
    }

    actual fun error(tag: String, message: String, throwable: Throwable?) {
        consoleLogger.error(tag, message, throwable)
    }
}
