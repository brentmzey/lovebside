package love.bside.app.core

import platform.Foundation.NSLog

actual object AppLogger {
    actual fun debug(tag: String, message: String, throwable: Throwable?) {
        NSLog("[DEBUG] %@: %@", tag, message)
        throwable?.let { NSLog("Exception: %@", it.message ?: "") }
    }

    actual fun info(tag: String, message: String, throwable: Throwable?) {
        NSLog("[INFO] %@: %@", tag, message)
        throwable?.let { NSLog("Exception: %@", it.message ?: "") }
    }

    actual fun warn(tag: String, message: String, throwable: Throwable?) {
        NSLog("[WARN] %@: %@", tag, message)
        throwable?.let { NSLog("Exception: %@", it.message ?: "") }
    }

    actual fun error(tag: String, message: String, throwable: Throwable?) {
        NSLog("[ERROR] %@: %@", tag, message)
        throwable?.let { NSLog("Exception: %@", it.message ?: "") }
    }
}
