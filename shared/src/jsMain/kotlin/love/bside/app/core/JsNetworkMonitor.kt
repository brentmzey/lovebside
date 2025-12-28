package love.bside.app.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.browser.window
import org.w3c.dom.events.Event

/**
 * JS/Browser implementation of NetworkMonitor
 */
class JsNetworkMonitor : NetworkMonitor {
    private val _isOnline = MutableStateFlow(checkConnectivity())
    override val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()
    
    private var onlineListener: ((Event) -> Unit)? = null
    private var offlineListener: ((Event) -> Unit)? = null
    
    override fun startMonitoring() {
        onlineListener = { _isOnline.value = true }
        offlineListener = { _isOnline.value = false }
        
        window.addEventListener("online", onlineListener!!)
        window.addEventListener("offline", offlineListener!!)
    }
    
    override fun stopMonitoring() {
        onlineListener?.let { window.removeEventListener("online", it) }
        offlineListener?.let { window.removeEventListener("offline", it) }
        onlineListener = null
        offlineListener = null
    }
    
    override fun checkConnectivity(): Boolean {
        return js("navigator.onLine") as? Boolean ?: true
    }
}
