package love.bside.app.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.net.InetSocketAddress
import java.net.Socket

/**
 * JVM/Desktop implementation of NetworkMonitor
 */
class JvmNetworkMonitor : NetworkMonitor {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    private val _isOnline = MutableStateFlow(checkConnectivity())
    override val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()
    
    private var isMonitoring = false
    
    override fun startMonitoring() {
        if (isMonitoring) {
            return
        }
        isMonitoring = true
        
        scope.launch {
            while (isActive && isMonitoring) {
                _isOnline.value = checkConnectivity()
                delay(3000) // Check every 3 seconds
            }
        }
    }
    
    override fun stopMonitoring() {
        isMonitoring = false
    }
    
    override fun checkConnectivity(): Boolean {
        return try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress("8.8.8.8", 53), 1500)
                true
            }
        } catch (e: Exception) {
            false
        }
    }
}
