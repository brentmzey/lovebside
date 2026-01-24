package love.bside.server.routes.api.v1

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import love.bside.server.services.MessagingService
import love.bside.server.utils.toSuccessResponse
import love.bside.server.utils.getUserId
import org.koin.ktor.ext.inject

/**
 * Messaging routes for real-time 1-to-1 messaging
 * 
 * All routes require JWT authentication
 */
fun Route.messagingRoutes() {
    val messagingService by inject<MessagingService>()
    
    authenticate("jwt") {
        route("/conversations") {
            // Get all conversations for current user
            get {
                val userId = call.getUserId()
                val conversations = messagingService.getUserConversations(userId)
                call.respond(HttpStatusCode.OK, conversations.toSuccessResponse())
            }
            
            // Create or get conversation with another user
            post {
                val userId = call.getUserId()
                val request = call.receive<CreateConversationRequest>()
                val conversation = messagingService.getOrCreateConversation(userId, request.otherUserId)
                call.respond(HttpStatusCode.OK, conversation.toSuccessResponse())
            }
            
            // Get specific conversation
            get("/{id}") {
                val userId = call.getUserId()
                val conversationId = call.parameters["id"] ?: throw IllegalArgumentException("Missing conversation ID")
                val conversation = messagingService.getConversation(conversationId, userId)
                call.respond(HttpStatusCode.OK, conversation.toSuccessResponse())
            }
            
            // Get messages in a conversation
            get("/{id}/messages") {
                val userId = call.getUserId()
                val conversationId = call.parameters["id"] ?: throw IllegalArgumentException("Missing conversation ID")
                val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                val perPage = call.request.queryParameters["perPage"]?.toIntOrNull() ?: 50
                
                val messages = messagingService.getConversationMessages(conversationId, userId, page, perPage)
                call.respond(HttpStatusCode.OK, messages.toSuccessResponse())
            }
            
            // Send a message in a conversation
            post("/{id}/messages") {
                val userId = call.getUserId()
                val conversationId = call.parameters["id"] ?: throw IllegalArgumentException("Missing conversation ID")
                val request = call.receive<SendMessageRequest>()
                
                val message = messagingService.sendMessage(
                    conversationId = conversationId,
                    senderId = userId,
                    content = request.content,
                    messageType = request.messageType
                )
                
                call.respond(HttpStatusCode.Created, message.toSuccessResponse())
            }
            
            // Update typing status
            post("/{id}/typing") {
                val userId = call.getUserId()
                val conversationId = call.parameters["id"] ?: throw IllegalArgumentException("Missing conversation ID")
                val request = call.receive<TypingStatusRequest>()
                
                messagingService.updateTypingStatus(conversationId, userId, request.isTyping)
                call.respond(HttpStatusCode.OK, mapOf("success" to true).toSuccessResponse())
            }
        }
        
        route("/messages") {
            // Mark message as read
            post("/{id}/read") {
                val userId = call.getUserId()
                val messageId = call.parameters["id"] ?: throw IllegalArgumentException("Missing message ID")
                
                messagingService.markMessageAsRead(messageId, userId)
                call.respond(HttpStatusCode.OK, mapOf("success" to true).toSuccessResponse())
            }
            
            // Mark multiple messages as read (batch)
            post("/read/batch") {
                val userId = call.getUserId()
                val request = call.receive<BatchReadRequest>()
                
                messagingService.markMessagesAsRead(request.messageIds, userId)
                call.respond(HttpStatusCode.OK, mapOf("success" to true).toSuccessResponse())
            }
            
            // Edit a message
            put("/{id}") {
                val userId = call.getUserId()
                val messageId = call.parameters["id"] ?: throw IllegalArgumentException("Missing message ID")
                val request = call.receive<EditMessageRequest>()
                
                val message = messagingService.editMessage(messageId, userId, request.content)
                call.respond(HttpStatusCode.OK, message.toSuccessResponse())
            }
            
            // Delete a message
            delete("/{id}") {
                val userId = call.getUserId()
                val messageId = call.parameters["id"] ?: throw IllegalArgumentException("Missing message ID")
                
                messagingService.deleteMessage(messageId, userId)
                call.respond(HttpStatusCode.OK, mapOf("success" to true).toSuccessResponse())
            }
        }
    }
}

/**
 * Request models for messaging routes
 */
@kotlinx.serialization.Serializable
data class CreateConversationRequest(
    val otherUserId: String
)

@kotlinx.serialization.Serializable
data class SendMessageRequest(
    val content: String,
    val messageType: String = "text"
)

@kotlinx.serialization.Serializable
data class TypingStatusRequest(
    val isTyping: Boolean
)

@kotlinx.serialization.Serializable
data class EditMessageRequest(
    val content: String
)

@kotlinx.serialization.Serializable
data class BatchReadRequest(
    val messageIds: List<String>
)
