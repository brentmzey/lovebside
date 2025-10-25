package io.pocketbase.services

import io.pocketbase.PocketBase
import io.pocketbase.models.*
import io.pocketbase.tools.SSEClient
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.Json

/**
 * Service for handling PocketBase realtime subscriptions via Server-Sent Events (SSE).
 */
class RealtimeService(client: PocketBase) : BaseService(client) {
    private var sseClient: SSEClient? = null
    private var clientId: String = ""
    private val subscriptions = mutableMapOf<String, MutableList<RealtimeEventCallback>>()
    private val lastSentSubscriptions = mutableListOf<String>()
    private val pendingConnects = mutableListOf<CompletableDeferred<Unit>>()
    private var reconnectAttempts = 0
    private val maxReconnectAttempts = Int.MAX_VALUE
    private val predefinedReconnectIntervals = listOf(200L, 300L, 500L, 1000L, 1200L, 1500L, 2000L)
    private val maxConnectTimeout = 15000L
    
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected
    
    private var connectJob: Job? = null
    private var reconnectJob: Job? = null
    
    private val json = Json { 
        ignoreUnknownKeys = true
        isLenient = true
    }
    
    /**
     * Callback invoked when the realtime connection is disconnected.
     * The parameter contains the list of active subscription keys.
     */
    var onDisconnect: ((List<String>) -> Unit)? = null
    
    /**
     * Subscribe to realtime changes for a specific topic.
     * 
     * @param topic The topic to subscribe to (e.g., "t_message", "t_message/RECORD_ID", or "*")
     * @param callback Function to call when an event is received
     * @param options Optional query options (filter, expand, headers, etc.)
     * @return Unsubscribe function
     */
    suspend fun subscribe(
        topic: String,
        callback: RealtimeEventCallback,
        options: QueryOptions? = null
    ): UnsubscribeFunc {
        require(topic.isNotEmpty()) { "topic must be set" }
        
        val key = buildSubscriptionKey(topic, options)
        
        // Store the callback
        subscriptions.getOrPut(key) { mutableListOf() }.add(callback)
        
        if (!_isConnected.value) {
            // Initialize SSE connection
            connect()
        } else if (subscriptions[key]?.size == 1) {
            // Send updated subscriptions if this is the first callback for the key
            submitSubscriptions()
        }
        
        return { unsubscribeByTopicAndCallback(topic, callback) }
    }
    
    /**
     * Unsubscribe from all subscriptions with the specified topic.
     * If topic is null, unsubscribes from all active subscriptions.
     */
    suspend fun unsubscribe(topic: String? = null) {
        if (topic == null) {
            subscriptions.clear()
        } else {
            val keysToRemove = getSubscriptionsByTopic(topic).keys
            keysToRemove.forEach { subscriptions.remove(it) }
        }
        
        if (!hasSubscriptionListeners()) {
            disconnect()
        } else {
            submitSubscriptions()
        }
    }
    
    /**
     * Unsubscribe from subscriptions with the specified topic prefix.
     */
    suspend fun unsubscribeByPrefix(keyPrefix: String) {
        var hasAtLeastOneTopic = false
        val keysToRemove = mutableListOf<String>()
        
        subscriptions.keys.forEach { key ->
            if (("$key?").startsWith(keyPrefix)) {
                hasAtLeastOneTopic = true
                keysToRemove.add(key)
            }
        }
        
        keysToRemove.forEach { subscriptions.remove(it) }
        
        if (!hasAtLeastOneTopic) return
        
        if (hasSubscriptionListeners()) {
            submitSubscriptions()
        } else {
            disconnect()
        }
    }
    
    private suspend fun unsubscribeByTopicAndCallback(topic: String, callback: RealtimeEventCallback) {
        val subs = getSubscriptionsByTopic(topic)
        var needToSubmit = false
        
        subs.keys.forEach { key ->
            val callbacks = subscriptions[key] ?: return@forEach
            if (callbacks.remove(callback) && !needToSubmit) {
                needToSubmit = true
            }
            if (callbacks.isEmpty()) {
                subscriptions.remove(key)
            }
        }
        
        if (!hasSubscriptionListeners()) {
            disconnect()
        } else if (needToSubmit) {
            submitSubscriptions()
        }
    }
    
    private fun buildSubscriptionKey(topic: String, options: QueryOptions?): String {
        if (options == null) return topic
        
        val queryParams = buildMap {
            options.filter?.let { put("filter", it) }
            options.expand?.let { put("expand", it) }
            options.fields?.let { put("fields", it) }
        }
        
        if (queryParams.isEmpty()) return topic
        
        val optionsJson = json.encodeToString(
            kotlinx.serialization.json.JsonObject.serializer(),
            kotlinx.serialization.json.JsonObject(queryParams.mapValues { 
                kotlinx.serialization.json.JsonPrimitive(it.value) 
            })
        )
        
        val separator = if (topic.contains("?")) "&" else "?"
        return "$topic${separator}options=${optionsJson.encodeURLParameter()}"
    }
    
    private fun String.encodeURLParameter(): String {
        // Basic URL encoding - in production use proper URL encoding
        return this.replace(" ", "%20")
            .replace("\"", "%22")
            .replace("{", "%7B")
            .replace("}", "%7D")
            .replace(",", "%2C")
            .replace(":", "%3A")
    }
    
    private fun getSubscriptionsByTopic(topic: String): Map<String, MutableList<RealtimeEventCallback>> {
        val normalizedTopic = if (topic.contains("?")) topic else "$topic?"
        return subscriptions.filter { ("${it.key}?").startsWith(normalizedTopic) }
    }
    
    private fun hasSubscriptionListeners(key: String? = null): Boolean {
        if (key != null) {
            return subscriptions[key]?.isNotEmpty() == true
        }
        return subscriptions.values.any { it.isNotEmpty() }
    }
    
    private suspend fun submitSubscriptions() {
        if (clientId.isEmpty()) return
        
        lastSentSubscriptions.clear()
        lastSentSubscriptions.addAll(getNonEmptySubscriptionKeys())
        
        try {
            client.send(
                path = "/api/realtime",
                method = "POST",
                body = mapOf(
                    "clientId" to clientId,
                    "subscriptions" to lastSentSubscriptions
                )
            )
        } catch (e: Exception) {
            if (e !is CancellationException) {
                throw e
            }
        }
    }
    
    private fun getNonEmptySubscriptionKeys(): List<String> {
        return subscriptions.filter { it.value.isNotEmpty() }.keys.toList()
    }
    
    private suspend fun connect() {
        if (reconnectAttempts > 0) {
            // Immediately resolve to avoid blocking during reconnection
            return
        }
        
        val deferred = CompletableDeferred<Unit>()
        pendingConnects.add(deferred)
        
        if (pendingConnects.size == 1) {
            initConnect()
        }
        
        deferred.await()
    }
    
    private fun initConnect() {
        disconnect(fromReconnect = true)
        
        val connectTimeout = CoroutineScope(Dispatchers.Default).launch {
            delay(maxConnectTimeout)
            connectErrorHandler(Exception("EventSource connect took too long"))
        }
        
        connectJob = CoroutineScope(Dispatchers.Default).launch {
            try {
                val url = client.buildURL("/api/realtime")
                sseClient = SSEClient(url, client.authStore.token)
                
                sseClient?.onMessage { eventName, data ->
                    CoroutineScope(Dispatchers.Default).launch {
                        when (eventName) {
                            "PB_CONNECT" -> {
                                connectTimeout.cancel()
                                handleConnect(data)
                            }
                            else -> {
                                handleMessage(eventName, data)
                            }
                        }
                    }
                }
                
                sseClient?.onError { error ->
                    connectTimeout.cancel()
                    connectErrorHandler(error)
                }
                
                sseClient?.connect()
            } catch (e: Exception) {
                connectTimeout.cancel()
                connectErrorHandler(e)
            }
        }
    }
    
    private suspend fun handleConnect(lastEventId: String) {
        clientId = lastEventId
        
        try {
            submitSubscriptions()
            
            // Retry unsent subscriptions
            var retries = 3
            while (hasUnsentSubscriptions() && retries > 0) {
                retries--
                submitSubscriptions()
            }
            
            // Resolve all pending connects
            pendingConnects.forEach { it.complete(Unit) }
            pendingConnects.clear()
            reconnectAttempts = 0
            _isConnected.value = true
            
        } catch (e: Exception) {
            clientId = ""
            connectErrorHandler(e)
        }
    }
    
    private fun handleMessage(eventName: String, data: String) {
        try {
            val event = json.decodeFromString<RealtimeEvent>(data)
            subscriptions[eventName]?.forEach { callback ->
                try {
                    callback(event)
                } catch (e: Exception) {
                    // Log error but don't break other callbacks
                    println("Error in realtime callback: ${e.message}")
                }
            }
        } catch (e: Exception) {
            println("Error parsing realtime event: ${e.message}")
        }
    }
    
    private fun hasUnsentSubscriptions(): Boolean {
        val latestTopics = getNonEmptySubscriptionKeys()
        if (latestTopics.size != lastSentSubscriptions.size) return true
        return latestTopics.any { it !in lastSentSubscriptions }
    }
    
    private fun connectErrorHandler(error: Throwable) {
        connectJob?.cancel()
        reconnectJob?.cancel()
        
        if ((!clientId.isEmpty() || reconnectAttempts == 0) && 
            reconnectAttempts <= maxReconnectAttempts) {
            // Reconnect in background
            disconnect(fromReconnect = true)
            val timeout = predefinedReconnectIntervals.getOrNull(reconnectAttempts)
                ?: predefinedReconnectIntervals.last()
            reconnectAttempts++
            
            reconnectJob = CoroutineScope(Dispatchers.Default).launch {
                delay(timeout)
                initConnect()
            }
        } else {
            // Direct reject - couldn't connect or max retries reached
            pendingConnects.forEach { it.completeExceptionally(error) }
            pendingConnects.clear()
            disconnect()
        }
    }
    
    private fun disconnect(fromReconnect: Boolean = false) {
        if (clientId.isNotEmpty() && onDisconnect != null) {
            onDisconnect?.invoke(subscriptions.keys.toList())
        }
        
        connectJob?.cancel()
        reconnectJob?.cancel()
        sseClient?.close()
        sseClient = null
        clientId = ""
        _isConnected.value = false
        
        if (!fromReconnect) {
            reconnectAttempts = 0
            // Resolve remaining connects to avoid errors
            pendingConnects.forEach { it.complete(Unit) }
            pendingConnects.clear()
        }
    }
}
