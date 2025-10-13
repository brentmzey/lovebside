package love.bside.app.core

actual object AppLogger {
    actual fun debug(tag: String, message: String, throwable: Throwable?) {
        console.log("[DEBUG] $tag: $message")
        throwable?.let { console.log("Exception:", it) }
    }

    actual fun info(tag: String, message: String, throwable: Throwable?) {
        console.info("[INFO] $tag: $message")
        throwable?.let { console.info("Exception:", it) }
    }

    actual fun warn(tag: String, message: String, throwable: Throwable?) {
        console.warn("[WARN] $tag: $message")
        throwable?.let { console.warn("Exception:", it) }
    }

    actual fun error(tag: String, message: String, throwable: Throwable?) {
        console.error("[ERROR] $tag: $message")
        throwable?.let { console.error("Exception:", it) }
    }
}
