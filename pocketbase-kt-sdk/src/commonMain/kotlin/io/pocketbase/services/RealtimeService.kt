package io.pocketbase.services

import io.pocketbase.PocketBase
import io.pocketbase.config.RealtimeMode
import io.pocketbase.config.RealtimeTransportKind
import io.pocketbase.models.QueryOptions
import io.pocketbase.models.RealtimeEvent
import io.pocketbase.models.RealtimeEventCallback
import io.pocketbase.models.RealtimeAction
import io.pocketbase.models.UnsubscribeFunc
import io.pocketbase.tools.SSEClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Service for handling PocketBase realtime subscriptions via SSE with
 * a smart polling fallback that mimics realtime delivery when SSE is blocked.
 */
class RealtimeService(client: PocketBase) : BaseService(client) {
    private val config = client.realtimeConfig
    private val useSse = config.mode != RealtimeMode.SMART_POLLING_ONLY
    private val usePolling = config.mode != RealtimeMode.SSE_ONLY
    private var pollingActive = config.mode == RealtimeMode.SMART_POLLING_ONLY

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private var sseClient: SSEClient? = null
    private var clientId: String = ""
    private val subscriptions = mutableMapOf<String, SubscriptionBucket>()
    private val lastSentSubscriptions = mutableListOf<String>()
    private val pendingConnects = mutableListOf<CompletableDeferred<Unit>>()
    private var reconnectAttempts = 0
    private val maxReconnectAttempts = Int.MAX_VALUE
    private val predefinedReconnectIntervals = listOf(200L, 300L, 500L, 1000L, 1200L, 1500L, 2000L)
    private val maxConnectTimeout = 15000L

    private val pollingEngine = if (usePolling) SmartPollingEngine(client, config.smartPolling) else null

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected

    private val _activeTransport = MutableStateFlow(RealtimeTransportKind.INACTIVE)
    val activeTransport: StateFlow<RealtimeTransportKind> = _activeTransport.asStateFlow()

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

    init {
        pollingEngine?.let { engine ->
            scope.launch {
                engine.events.collect { emission ->
                    dispatchEvent(emission.key, RealtimeEvent(emission.action, emission.payload))
                }
            }
        }
    }

    /**
     * Subscribe to realtime changes for a specific topic.
     */
    suspend fun subscribe(
        topic: String,
        callback: RealtimeEventCallback,
        options: QueryOptions? = null
    ): UnsubscribeFunc {
        require(topic.isNotEmpty()) { "topic must be set" }

        val key = buildSubscriptionKey(topic, options)
        val bucket = subscriptions.getOrPut(key) { SubscriptionBucket(key, topic, options) }
        bucket.callbacks.add(callback)

        if (useSse) {
            if (!_isConnected.value) {
                connect()
            } else if (bucket.callbacks.size == 1) {
                submitSubscriptions()
            }
        }

        if (usePolling && pollingActive) {
            updatePollingSubscriptions()
        }

        return { unsubscribeByTopicAndCallback(topic, callback) }
    }

    /**
     * Unsubscribe from all subscriptions with the specified topic.
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
            if (usePolling && pollingActive) {
                updatePollingSubscriptions()
            }
        }
    }

    /**
     * Unsubscribe from subscriptions with the specified topic prefix.
     */
    suspend fun unsubscribeByPrefix(keyPrefix: String) {
        val keysToRemove = subscriptions.keys.filter { ("$it?").startsWith(keyPrefix) }
        if (keysToRemove.isEmpty()) return

        keysToRemove.forEach { subscriptions.remove(it) }

        if (!hasSubscriptionListeners()) {
            disconnect()
        } else {
            submitSubscriptions()
            if (usePolling && pollingActive) {
                updatePollingSubscriptions()
            }
        }
    }

    private suspend fun unsubscribeByTopicAndCallback(topic: String, callback: RealtimeEventCallback) {
        val subs = getSubscriptionsByTopic(topic)
        var needToSubmit = false

        subs.forEach { (key, bucket) ->
            if (bucket.callbacks.remove(callback)) {
                needToSubmit = true
            }
            if (bucket.callbacks.isEmpty()) {
                subscriptions.remove(key)
            }
        }

        if (!hasSubscriptionListeners()) {
            disconnect()
        } else if (needToSubmit) {
            submitSubscriptions()
            if (usePolling && pollingActive) {
                updatePollingSubscriptions()
            }
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
            JsonObject.serializer(),
            JsonObject(queryParams.mapValues { JsonPrimitive(it.value) })
        )

        val separator = if (topic.contains("?")) "&" else "?"
        return "$topic${separator}options=${optionsJson.encodeURLParameter()}"
    }

    private fun getSubscriptionsByTopic(topic: String): Map<String, SubscriptionBucket> {
        val normalizedTopic = if (topic.contains("?")) topic else "$topic?"
        return subscriptions.filter { ("${it.key}?").startsWith(normalizedTopic) }
    }

    private fun hasSubscriptionListeners(key: String? = null): Boolean {
        if (key != null) {
            return subscriptions[key]?.callbacks?.isNotEmpty() == true
        }
        return subscriptions.values.any { it.callbacks.isNotEmpty() }
    }

    private suspend fun submitSubscriptions() {
        if (!useSse || clientId.isEmpty()) return

        val activeKeys = getNonEmptySubscriptionKeys()
        if (activeKeys.isEmpty()) return

        lastSentSubscriptions.clear()
        lastSentSubscriptions.addAll(activeKeys)

        try {
            client.send<JsonObject>(
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
        return subscriptions.values.filter { it.callbacks.isNotEmpty() }.map { it.key }
    }

    private suspend fun connect() {
        if (!useSse) {
            if (usePolling) {
                activatePolling()
            }
            return
        }

        if (reconnectAttempts > 0) {
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

        val connectTimeout = scope.launch {
            delay(maxConnectTimeout)
            connectErrorHandler(Exception("EventSource connect took too long"))
        }

        connectJob = scope.launch {
            try {
                val url = client.buildURL("/api/realtime")
                sseClient = SSEClient(url, client.authStore.token)

                sseClient?.onMessage { eventName, data ->
                    scope.launch {
                        when (eventName) {
                            "PB_CONNECT" -> {
                                connectTimeout.cancel()
                                handleConnect(data)
                            }
                            else -> handleMessage(eventName, data)
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

            var retries = 3
            while (hasUnsentSubscriptions() && retries > 0) {
                retries--
                submitSubscriptions()
            }

            pendingConnects.forEach { it.complete(Unit) }
            pendingConnects.clear()
            reconnectAttempts = 0
            _isConnected.value = true
            _activeTransport.value = RealtimeTransportKind.SSE
            deactivatePolling()
        } catch (e: Exception) {
            clientId = ""
            connectErrorHandler(e)
        }
    }

    private fun handleMessage(eventName: String, data: String) {
        try {
            val jsonElement = json.parseToJsonElement(data)
            val event = json.decodeFromJsonElement(RealtimeEvent.serializer(), jsonElement)
            dispatchEvent(eventName, event)
        } catch (e: Exception) {
            println("Error parsing realtime event: ${e.message}")
        }
    }

    private fun dispatchEvent(subscriptionKey: String, event: RealtimeEvent) {
        val bucket = subscriptions[subscriptionKey] ?: return
        bucket.callbacks.forEach { callback ->
            try {
                callback(event)
            } catch (e: Exception) {
                println("Error in realtime callback: ${e.message}")
            }
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

        if (usePolling && !pollingActive && config.mode == RealtimeMode.HYBRID && reconnectAttempts >= config.smartPolling.activationThreshold) {
            activatePolling()
        }

        if ((!clientId.isEmpty() || reconnectAttempts == 0) && reconnectAttempts <= maxReconnectAttempts) {
            disconnect(fromReconnect = true)
            val timeout = predefinedReconnectIntervals.getOrNull(reconnectAttempts)
                ?: predefinedReconnectIntervals.last()
            reconnectAttempts++

            reconnectJob = scope.launch {
                delay(timeout)
                initConnect()
            }
        } else {
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
            pendingConnects.forEach { it.complete(Unit) }
            pendingConnects.clear()
        }

        if (!hasSubscriptionListeners()) {
            deactivatePolling()
            pollingEngine?.stop()
            _activeTransport.value = RealtimeTransportKind.INACTIVE
        }
    }

    private fun activatePolling() {
        if (!usePolling) return
        pollingActive = true
        updatePollingSubscriptions()
        if (subscriptions.values.any { it.callbacks.isNotEmpty() }) {
            _activeTransport.value = RealtimeTransportKind.SMART_POLLING
        }
    }

    private fun deactivatePolling() {
        if (!usePolling || config.mode == RealtimeMode.SMART_POLLING_ONLY) return
        pollingActive = false
        pollingEngine?.stop()
        if (!_isConnected.value) {
            _activeTransport.value = RealtimeTransportKind.INACTIVE
        }
    }

    private fun updatePollingSubscriptions() {
        val engine = pollingEngine ?: return
        val activeBuckets = subscriptions.values.filter { it.callbacks.isNotEmpty() }
        if (activeBuckets.isEmpty()) {
            engine.stop()
            if (!_isConnected.value) {
                _activeTransport.value = RealtimeTransportKind.INACTIVE
            }
            return
        }

        if (pollingActive) {
            engine.updateSubscriptions(activeBuckets.map { it.toPollingSubscription() })
            _activeTransport.value = RealtimeTransportKind.SMART_POLLING
        }
    }

    private fun SubscriptionBucket.toPollingSubscription(): SmartPollingEngine.Subscription {
        return SmartPollingEngine.Subscription(key, topic, options)
    }

    private data class SubscriptionBucket(
        val key: String,
        val topic: String,
        val options: QueryOptions?,
        val callbacks: MutableList<RealtimeEventCallback> = mutableListOf()
    )
}

private fun String.encodeURLParameter(): String {
    return this.replace(" ", "%20")
        .replace("\"", "%22")
        .replace("{", "%7B")
        .replace("}", "%7D")
        .replace(",", "%2C")
        .replace(":", "%3A")
}
