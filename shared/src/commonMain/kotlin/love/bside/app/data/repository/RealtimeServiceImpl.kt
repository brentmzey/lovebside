package love.bside.app.data.repository

import io.pocketbase.PocketBase
import io.pocketbase.models.RecordModel
import io.pocketbase.models.RealtimeAction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import love.bside.app.core.AppException
import love.bside.app.core.Result
import love.bside.app.domain.models.Message
import love.bside.app.domain.models.TypingStatus
import love.bside.app.data.repository.PocketBaseMessagingRepository

class RealtimeServiceImpl(
    private val pocketBase: PocketBase,
    private val repo: PocketBaseMessagingRepository
) : RealtimeService {

    override fun subscribeToConversation(conversationId: String): Flow<Message> = callbackFlow {
        val unsubscribe = pocketBase.realtime.subscribe("m_messages", callback = { event ->
            // Filter for this conversation and ensure it's a create or update event
            val record = event.record.jsonObject
            if (record["conversationId"]?.jsonPrimitive?.content == conversationId &&
                (event.action == RealtimeAction.create || event.action == RealtimeAction.update)) {
                
                try {
                    val message = repo.mapRecordToMessage(event.record)
                    trySend(message)
                } catch (e: Exception) {
                    println("Error mapping realtime message: ${e.message}")
                }
            }
        })

        awaitClose {
            // Unsubscribe when the flow collector is cancelled
            launch {
                unsubscribe()
            }
        }
    }

    override fun subscribeToTypingIndicators(conversationId: String): Flow<TypingStatus> = callbackFlow {
        val unsubscribe = pocketBase.realtime.subscribe("t_typing_status", callback = { event ->
             val record = event.record.jsonObject
             if (record["conversationId"]?.jsonPrimitive?.content == conversationId &&
                 (event.action == RealtimeAction.create || event.action == RealtimeAction.update)) {
                 
                 val userId = record["userId"]?.jsonPrimitive?.content ?: ""
                 val isTyping = record["isTyping"]?.jsonPrimitive?.content?.toBoolean() ?: false
                 
                 if (userId.isNotEmpty()) {
                     trySend(TypingStatus(
                         conversationId = conversationId,
                         userId = userId,
                         isTyping = isTyping,
                         lastTyped = Clock.System.now()
                     ))
                 }
             }
        })
        
        awaitClose {
            launch {
                unsubscribe()
            }
        }
    }

    // Debounce/Tracking for typing status to avoid flood
    private var lastTypingSent = Instant.DISTANT_PAST
    
    override suspend fun setTypingStatus(conversationId: String, isTyping: Boolean) {
        val now = Clock.System.now()
        // Simple rate limiting: don't send updates more than once every 2 seconds unless state changed
        // But for "isTyping=false", send immediately to clear.
        
        if (isTyping && (now - lastTypingSent).inWholeSeconds < 2) {
             return
        }

        try {
            // Check auth store model type. It might be JsonObject or RecordModel depending on SDK version/implementations.
            // Based on AuthResponse, it's JsonObject.
            val model = pocketBase.authStore.model
            val userId = when (model) {
                 is RecordModel -> model.id
                 // If it's a JsonObject or other map-like structure
                 else -> (model as? kotlinx.serialization.json.JsonObject)?.get("id")?.jsonPrimitive?.content
            } ?: return
            
            // We need to upsert. Since PB doesn't verify upsert by composite key easily without custom logic,
            // we will just create a new ephemeral record or update if we tracked the ID.
            // For simplicity in this architecture (stateless), we create a new status event.
            // A better approach requires a known record ID for the user's typing status in this conversation.
            // Assuming "t_typing_status" is a high-churn collection or just events.
            
            // NOTE: Ideally we find an existing record:
            // filter="conversationId='...' && userId='...'"
            
            val existing = try {
                pocketBase.collection("t_typing_status")
                    .getFirstListItem("conversationId='$conversationId' && userId='$userId'")
            } catch (e: Exception) { null }

            val body = mapOf(
                "conversationId" to conversationId,
                "userId" to userId,
                "isTyping" to isTyping,
                 "updated" to now.toString()
            )

            if (existing != null) {
                // existing is JsonObject
                val id = existing["id"]?.jsonPrimitive?.content ?: return
                pocketBase.collection("t_typing_status").update(id, body)
            } else {
                pocketBase.collection("t_typing_status").create(body)
            }
            
            lastTypingSent = now
        } catch (e: Exception) {
            // Log error
            println("Failed to set typing status: ${e.message}")
        }
    }
}
