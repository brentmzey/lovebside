package love.bside.app.core

import kotlinx.coroutines.flow.StateFlow

/**
 * Network connectivity monitor interface
 * Platform-specific implementations monitor network state
 */
interface NetworkMonitor {
    /**
     * Flow emitting current network connectivity state
     * true = online, false = offline
     */
    val isOnline: StateFlow<Boolean>
    
    /**
     * Start monitoring network changes
     */
    fun startMonitoring()
    
    /**
     * Stop monitoring network changes
     */
    fun stopMonitoring()
    
    /**
     * Check current connectivity status synchronously
     */
    fun checkConnectivity(): Boolean
}

/**
 * Network connectivity state
 */
enum class NetworkState {
    ONLINE,
    OFFLINE,
    METERED,  // On cellular/limited data
    UNKNOWN
}

/**
 * Detailed network info
 */
data class NetworkInfo(
    val state: NetworkState,
    val isMetered: Boolean,
    val connectionType: ConnectionType
)

enum class ConnectionType {
    WIFI,
    CELLULAR,
    ETHERNET,
    UNKNOWN
}
