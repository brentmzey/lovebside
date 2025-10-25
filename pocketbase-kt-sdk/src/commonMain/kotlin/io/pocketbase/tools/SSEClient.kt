package io.pocketbase.tools

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Cross-platform Server-Sent Events (SSE) client implementation.
 * Handles streaming responses from PocketBase realtime API.
 */
class SSEClient(
    private val url: String,
    private val authToken: String = ""
) {
    private var job: Job? = null
    private var onMessageCallback: ((eventName: String, data: String) -> Unit)? = null
    private var onErrorCallback: ((Throwable) -> Unit)? = null
    
    /**
     * Register callback for SSE messages.
     */
    fun onMessage(callback: (eventName: String, data: String) -> Unit) {
        onMessageCallback = callback
    }
    
    /**
     * Register callback for errors.
     */
    fun onError(callback: (Throwable) -> Unit) {
        onErrorCallback = callback
    }
    
    /**
     * Start the SSE connection.
     */
    fun connect() {
        job = CoroutineScope(Dispatchers.Default).launch {
            try {
                val client = HttpClient {
                    install(HttpTimeout) {
                        requestTimeoutMillis = Long.MAX_VALUE
                        connectTimeoutMillis = 15000
                        socketTimeoutMillis = Long.MAX_VALUE
                    }
                }
                
                client.prepareGet(url) {
                    header(HttpHeaders.Accept, "text/event-stream")
                    header(HttpHeaders.CacheControl, "no-cache")
                    if (authToken.isNotEmpty()) {
                        header(HttpHeaders.Authorization, authToken)
                    }
                }.execute { response ->
                    if (response.status != HttpStatusCode.OK) {
                        onErrorCallback?.invoke(Exception("HTTP ${response.status.value}"))
                        return@execute
                    }
                    
                    val channel = response.bodyAsChannel()
                    parseSSE(channel).collect { event ->
                        when {
                            event.event != null && event.data != null -> {
                                onMessageCallback?.invoke(event.event, event.data)
                            }
                            event.id != null -> {
                                // Handle connection event with ID (lastEventId)
                                onMessageCallback?.invoke("PB_CONNECT", event.id)
                            }
                        }
                    }
                }
                
                client.close()
            } catch (e: Exception) {
                when (e) {
                    is kotlinx.coroutines.CancellationException -> {
                        // Ignore cancellation
                    }
                    else -> onErrorCallback?.invoke(e)
                }
            }
        }
    }
    
    /**
     * Close the SSE connection.
     */
    fun close() {
        job?.cancel()
        job = null
    }
    
    private fun parseSSE(channel: ByteReadChannel): Flow<SSEEvent> = flow {
        val buffer = StringBuilder()
        var currentEvent = SSEEvent()
        
        while (!channel.isClosedForRead) {
            val line = try {
                channel.readUTF8Line() ?: break
            } catch (e: Exception) {
                break
            }
            
            when {
                line.isEmpty() -> {
                    // Empty line signifies end of event
                    if (currentEvent.hasData()) {
                        emit(currentEvent)
                    }
                    currentEvent = SSEEvent()
                }
                line.startsWith("event:") -> {
                    currentEvent = currentEvent.copy(event = line.substring(6).trim())
                }
                line.startsWith("data:") -> {
                    val data = line.substring(5).trimStart()
                    currentEvent = if (currentEvent.data == null) {
                        currentEvent.copy(data = data)
                    } else {
                        currentEvent.copy(data = "${currentEvent.data}\n$data")
                    }
                }
                line.startsWith("id:") -> {
                    currentEvent = currentEvent.copy(id = line.substring(3).trim())
                }
                line.startsWith("retry:") -> {
                    val retry = line.substring(6).trim().toLongOrNull()
                    if (retry != null) {
                        currentEvent = currentEvent.copy(retry = retry)
                    }
                }
                line.startsWith(":") -> {
                    // Comment, ignore
                }
            }
        }
    }
    
    private data class SSEEvent(
        val event: String? = null,
        val data: String? = null,
        val id: String? = null,
        val retry: Long? = null
    ) {
        fun hasData(): Boolean = data != null || id != null
    }
}
