package love.bside.app.core

actual object AppLogger : Logger {
    actual override fun debug(tag: String, message: String, throwable: Throwable?) {
        println("[DEBUG] $tag: $message")
        throwable?.let { println("Exception: ${it.message}") }
    }

    actual override fun info(tag: String, message: String, throwable: Throwable?) {
        println("[INFO] $tag: $message")
        throwable?.let { println("Exception: ${it.message}") }
    }

    actual override fun warn(tag: String, message: String, throwable: Throwable?) {
        println("[WARN] $tag: $message")
        throwable?.let { println("Exception: ${it.message}") }
    }

    actual override fun error(tag: String, message: String, throwable: Throwable?) {
        println("[ERROR] $tag: $message")
        throwable?.let { println("Exception: ${it.message}") }
    }
}
