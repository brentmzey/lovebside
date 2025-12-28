package love.bside.app.core

actual object NetworkMonitorFactory {
    actual fun create(): NetworkMonitor {
        return JvmNetworkMonitor()
    }
}
