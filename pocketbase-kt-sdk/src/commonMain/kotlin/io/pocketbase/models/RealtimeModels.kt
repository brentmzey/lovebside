package io.pocketbase.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Represents a realtime event received from PocketBase SSE connection.
 */
@Serializable
data class RealtimeEvent(
    val action: RealtimeAction,
    val record: JsonElement
)

/**
 * The type of action that occurred on a record.
 */
@Serializable
enum class RealtimeAction {
    create,
    update,
    delete
}

/**
 * Callback for handling realtime events.
 */
typealias RealtimeEventCallback = (RealtimeEvent) -> Unit

/**
 * Function to unsubscribe from a realtime subscription.
 */
typealias UnsubscribeFunc = suspend () -> Unit
