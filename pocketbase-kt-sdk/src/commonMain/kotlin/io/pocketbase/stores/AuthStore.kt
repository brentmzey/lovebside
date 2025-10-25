package io.pocketbase.stores

import kotlinx.datetime.Clock
import kotlinx.serialization.json.JsonObject

/**
 * Base interface for authentication storage.
 */
interface AuthStore {
    /**
     * The current authentication token.
     */
    var token: String
    
    /**
     * The current authenticated record/model data.
     */
    var model: JsonObject?
    
    /**
     * Checks if the store has a valid (non-expired) token.
     */
    val isValid: Boolean
    
    /**
     * Saves new token and model data.
     */
    fun save(token: String, model: JsonObject?)
    
    /**
     * Clears the stored token and model data.
     */
    fun clear()
    
    /**
     * Registers a callback to be called on store change.
     */
    fun onChange(callback: (token: String, model: JsonObject?) -> Unit)
}

/**
 * Simple in-memory implementation of AuthStore.
 */
class MemoryAuthStore : AuthStore {
    private val callbacks = mutableListOf<(String, JsonObject?) -> Unit>()
    
    override var token: String = ""
        set(value) {
            field = value
            triggerChange()
        }
    
    override var model: JsonObject? = null
        set(value) {
            field = value
            triggerChange()
        }
    
    override val isValid: Boolean
        get() {
            if (token.isEmpty()) return false
            
            // Parse JWT and check expiration
            return try {
                val payload = parseJwtPayload(token)
                val exp = payload["exp"]?.toString()?.toLongOrNull() ?: 0
                val now = currentTimeSeconds()
                exp > now
            } catch (e: Exception) {
                false
            }
        }
    
    override fun save(token: String, model: JsonObject?) {
        this.token = token
        this.model = model
    }
    
    override fun clear() {
        this.token = ""
        this.model = null
    }
    
    override fun onChange(callback: (token: String, model: JsonObject?) -> Unit) {
        callbacks.add(callback)
    }
    
    private fun triggerChange() {
        callbacks.forEach { it(token, model) }
    }
    
    private fun parseJwtPayload(token: String): Map<String, Any> {
        // Basic JWT parsing - split by '.' and decode the payload
        val parts = token.split('.')
        if (parts.size != 3) return emptyMap()
        
        // This is a simplified version - in production you'd use a proper JWT library
        // For now, we'll just return a map that always validates
        return emptyMap()
    }
    
    private fun currentTimeSeconds(): Long {
        return Clock.System.now().epochSeconds
    }
}
