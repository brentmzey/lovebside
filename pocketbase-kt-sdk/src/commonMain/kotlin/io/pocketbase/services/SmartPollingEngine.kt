package io.pocketbase.services

import io.pocketbase.PocketBase
import io.pocketbase.config.SmartPollingConfig
import io.pocketbase.models.ClientResponseException
import io.pocketbase.models.QueryOptions
import io.pocketbase.models.RealtimeAction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.contentOrNull
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/**
 * A cross-platform smart polling engine that mimics SSE semantics when native
 * realtime streams are blocked. Each subscription runs in its own lightweight
 * coroutine and emits [RealtimeEvent]s whenever a delta is detected.
 */
internal class SmartPollingEngine(
    private val client: PocketBase,
    private val config: SmartPollingConfig
) {
    data class Subscription(
        val key: String,
        val topic: String,
        val options: QueryOptions?
    )

    data class Emission(
        val key: String,
        val action: RealtimeAction,
        val payload: JsonObject
    )

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val jobs = mutableMapOf<String, Job>()
    private val snapshots = mutableMapOf<String, PollSnapshot>()
    private val subscriptions = mutableMapOf<String, Subscription>()

    private val _events = MutableSharedFlow<Emission>(extraBufferCapacity = 64)
    val events: SharedFlow<Emission> = _events.asSharedFlow()

    fun updateSubscriptions(items: Collection<Subscription>) {
        val newKeys = items.map { it.key }.toSet()
        val removed = subscriptions.keys - newKeys
        removed.forEach { key ->
            subscriptions.remove(key)
            jobs.remove(key)?.cancel()
            snapshots.remove(key)
        }

        items.forEach { subscription ->
            subscriptions[subscription.key] = subscription
            if (jobs[subscription.key]?.isActive != true) {
                jobs[subscription.key] = scope.launch { pollLoop(subscription.key) }
            }
        }
    }

    fun stop() {
        subscriptions.clear()
        snapshots.clear()
        jobs.values.forEach { it.cancel() }
        jobs.clear()
    }

    private suspend fun pollLoop(key: String) {
        var delayMs = config.initialDelayMs
        val snapshot = snapshots.getOrPut(key) { PollSnapshot() }

        while (scope.isActive) {
            val subscription = subscriptions[key] ?: break
            val emitted = try {
                pollOnce(subscription, snapshot)
            } catch (e: Exception) {
                println("[PocketBase] Smart polling error for $key → ${e.message}")
                false
            }

            delayMs = nextDelay(delayMs, emitted)
            delay(withJitter(delayMs))
        }
    }

    private suspend fun pollOnce(subscription: Subscription, snapshot: PollSnapshot): Boolean {
        val topic = subscription.topic.substringBefore("?")
        val segments = topic.split("/")
        val collection = segments.firstOrNull()?.takeIf { it.isNotBlank() } ?: return false
        val recordId = segments.getOrNull(1)?.takeUnless { it.isBlank() || it == "*" }

        return if (recordId == null) {
            pollCollection(subscription, collection, snapshot)
        } else {
            pollRecord(subscription, collection, recordId, snapshot)
        }
    }

    private suspend fun pollCollection(
        subscription: Subscription,
        collection: String,
        snapshot: PollSnapshot
    ): Boolean {
        val service = client.collection(collection)
        val options = subscription.options.optimize(config.batchSize)
        val listResult = service.getList(options)
        val seen = mutableSetOf<String>()
        var emitted = false

        listResult.items.forEach { record ->
            val id = record["id"]?.jsonPrimitive?.contentOrNull ?: return@forEach
            val updated = record["updated"]?.jsonPrimitive?.contentOrNull ?: ""
            seen += id

            val cached = snapshot.records[id]
            if (cached == null) {
                emitted = emit(subscription.key, RealtimeAction.create, record) || emitted
            } else if (cached.updated != updated) {
                emitted = emit(subscription.key, RealtimeAction.update, record) || emitted
            }

            snapshot.records[id] = RecordShadow(record, updated, 0)
        }

        val iterator = snapshot.records.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (entry.key in seen) continue

            val misses = entry.value.misses + 1
            if (misses >= config.deleteAfterMisses) {
                emitted = emit(
                    subscription.key,
                    RealtimeAction.delete,
                    buildJsonObject { put("id", JsonPrimitive(entry.key)) }
                ) || emitted
                iterator.remove()
            } else {
                entry.value.misses = misses
            }
        }

        return emitted
    }

    private suspend fun pollRecord(
        subscription: Subscription,
        collection: String,
        recordId: String,
        snapshot: PollSnapshot
    ): Boolean {
        val service = client.collection(collection)
        val record = try {
            service.getOne(recordId, subscription.options)
        } catch (error: ClientResponseException) {
            if (error.statusCode == 404) null else throw error
        }

        val cached = snapshot.records[recordId]
        return when {
            record == null && cached != null -> {
                snapshot.records.remove(recordId)
                emit(
                    subscription.key,
                    RealtimeAction.delete,
                    buildJsonObject { put("id", JsonPrimitive(recordId)) }
                )
            }
            record != null && cached == null -> {
                snapshot.records[recordId] = RecordShadow(record, record["updated"]?.jsonPrimitive?.contentOrNull ?: "", 0)
                emit(subscription.key, RealtimeAction.create, record)
            }
            record != null && cached != null -> {
                val updated = record["updated"]?.jsonPrimitive?.contentOrNull
                val changed = updated != null && updated != cached.updated
                snapshot.records[recordId] = RecordShadow(record, updated ?: cached.updated, 0)
                if (changed) emit(subscription.key, RealtimeAction.update, record) else false
            }
            else -> false
        }
    }

    private suspend fun emit(key: String, action: RealtimeAction, payload: JsonObject): Boolean {
        val emission = Emission(key, action, payload)
        return if (_events.tryEmit(emission)) {
            true
        } else {
            _events.emit(emission)
            true
        }
    }

    private fun nextDelay(current: Long, emitted: Boolean): Long {
        if (emitted) return config.minDelayMs
        val doubled = if (current <= 0) config.minDelayMs else current * 2
        return min(max(doubled, config.minDelayMs), config.maxDelayMs)
    }

    private fun withJitter(delay: Long): Long {
        if (config.jitterRatio <= 0.0) return delay
        val jitter = max(1L, (delay * config.jitterRatio).toLong())
        return delay - jitter + Random.nextLong(jitter * 2)
    }

    private fun QueryOptions?.optimize(batchSize: Int): QueryOptions {
        val targetSort = this?.sort ?: "-updated"
        val targetPerPage = this?.perPage ?: batchSize
        return (this ?: QueryOptions()).copy(sort = targetSort, perPage = targetPerPage)
    }

    private data class PollSnapshot(
        val records: MutableMap<String, RecordShadow> = mutableMapOf()
    )

    private data class RecordShadow(
        val payload: JsonObject,
        val updated: String,
        var misses: Int
    )
}
