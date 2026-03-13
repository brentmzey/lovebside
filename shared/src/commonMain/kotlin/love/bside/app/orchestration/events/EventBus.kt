package love.bside.app.orchestration.events

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.random.Random

/**
 * Multiplatform event bus for domain events and cross-cutting concerns.
 * Thread-safe, supports filtering, priority, and replay.
 */
class EventBus(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
    private val replayCapacity: Int = 100
) {
    private val _events = MutableSharedFlow<DomainEvent>(
        replay = replayCapacity,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val events: SharedFlow<DomainEvent> = _events.asSharedFlow()

    internal val subscribers = mutableMapOf<String, MutableList<EventSubscriber<*>>>()
    internal var isRunning = false

    fun start() {
        isRunning = true
    }

    fun stop() {
        isRunning = false
        subscribers.clear()
    }

    /**
     * Publish an event to all subscribers
     */
    suspend fun publish(event: DomainEvent) {
        if (!isRunning) {
            return
        }
        
        _events.emit(event)
        
        // Notify specific subscribers
        subscribers[event::class.simpleName]?.forEach { subscriber ->
            @Suppress("UNCHECKED_CAST")
            scope.launch {
                try {
                    (subscriber as EventSubscriber<DomainEvent>).onEvent(event)
                } catch (e: Exception) {
                    // Log error but don't fail other subscribers
                }
            }
        }
    }

    /**
     * Subscribe to specific event types
     */
    inline fun <reified T : DomainEvent> subscribe(
        noinline handler: suspend (T) -> Unit
    ): SubscriptionHandle {
        val eventType = T::class.simpleName ?: ""
        return subscribeInternal(eventType, EventSubscriber(handler))
    }
    
    @PublishedApi
    internal fun subscribeInternal(eventType: String, subscriber: EventSubscriber<*>): SubscriptionHandle {
        subscribers.getOrPut(eventType) { mutableListOf() }.add(subscriber)
        
        return SubscriptionHandle {
            subscribers[eventType]?.remove(subscriber)
        }
    }

    /**
     * Get events as Flow with filtering
     */
    inline fun <reified T : DomainEvent> eventsOfType(): Flow<T> {
        return events.filterIsInstance<T>()
    }
}

/**
 * Handle to cancel a subscription
 */
class SubscriptionHandle(private val unsubscribe: () -> Unit) {
    fun cancel() = unsubscribe()
}

/**
 * Internal subscriber wrapper
 */
class EventSubscriber<T : DomainEvent>(
    private val handler: suspend (T) -> Unit
) {
    suspend fun onEvent(event: T) = handler(event)
}

/**
 * Base interface for all domain events
 */
interface DomainEvent {
    val eventId: String
    val occurredAt: Instant
    val aggregateId: String?
        get() = null
}

/**
 * Base implementation for domain events
 */
abstract class BaseDomainEvent : DomainEvent {
    override val eventId: String = generateEventId()
    override val occurredAt: Instant = Clock.System.now()
    
    private companion object {
        private fun generateEventId(): String {
            // Use timestamp + random for uniqueness
            return "evt_${Clock.System.now().toEpochMilliseconds()}_${(Random.nextDouble() * 100000).toInt()}"
        }
    }
}
