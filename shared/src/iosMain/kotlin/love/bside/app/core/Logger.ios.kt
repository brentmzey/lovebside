package love.bside.app.core

import platform.Foundation.NSLog

actual object AppLogger : Logger {
    actual override fun debug(tag: String, message: String, throwable: Throwable?) {
        NSLog("[DEBUG] %@: %@", tag, message)
        throwable?.let { NSLog("Exception: %@", it.message ?: "") }
    }

    actual override fun info(tag: String, message: String, throwable: Throwable?) {
        NSLog("[INFO] %@: %@", tag, message)
        throwable?.let { NSLog("Exception: %@", it.message ?: "") }
    }

    actual override fun warn(tag: String, message: String, throwable: Throwable?) {
        NSLog("[WARN] %@: %@", tag, message)
        throwable?.let { NSLog("Exception: %@", it.message ?: "") }
    }

    actual override fun error(tag: String, message: String, throwable: Throwable?) {
        NSLog("[ERROR] %@: %@", tag, message)
        throwable?.let { NSLog("Exception: %@", it.message ?: "") }
    }
}
