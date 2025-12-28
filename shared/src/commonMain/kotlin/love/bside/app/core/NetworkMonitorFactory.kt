package love.bside.app.core

/**
 * Factory for creating platform-specific NetworkMonitor
 */
expect object NetworkMonitorFactory {
    fun create(): NetworkMonitor
}
