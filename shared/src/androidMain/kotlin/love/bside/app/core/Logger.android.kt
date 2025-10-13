package love.bside.app.core

import android.util.Log

actual object AppLogger {
    actual fun debug(tag: String, message: String, throwable: Throwable?) {
        Log.d(tag, message, throwable)
    }

    actual fun info(tag: String, message: String, throwable: Throwable?) {
        Log.i(tag, message, throwable)
    }

    actual fun warn(tag: String, message: String, throwable: Throwable?) {
        Log.w(tag, message, throwable)
    }

    actual fun error(tag: String, message: String, throwable: Throwable?) {
        Log.e(tag, message, throwable)
    }
}
