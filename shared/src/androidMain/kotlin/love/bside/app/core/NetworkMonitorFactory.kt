package love.bside.app.core

import android.content.Context

private var appContext: Context? = null

fun initAndroidContext(context: Context) {
    appContext = context.applicationContext
}

actual object NetworkMonitorFactory {
    actual fun create(): NetworkMonitor {
        val context = appContext ?: throw IllegalStateException("Android context not initialized. Call initAndroidContext() first.")
        return AndroidNetworkMonitor(context)
    }
}
