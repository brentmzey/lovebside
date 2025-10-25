package love.bside.server.services

import kotlinx.datetime.Clock
import love.bside.app.core.AppException
import love.bside.app.data.models.messaging.*
import love.bside.server.repositories.MessagingRepository

/**
 * Service for messaging business logic
 * Handles conversations, messages, typing indicators, and read receipts
 */
class MessagingService(
    private val repository: MessagingRepository
) {
    
    // ===== Conversations =====
    
    /**
     * Get all conversations for a user
     */
    suspend fun getUserConversations(userId: String): List<Conversation> {
        return repository.getUserConversations(userId)
    }
    
    /**
     * Get or create a conversation between two users
     * Ensures consistent participant ordering (lower ID first)
     */
    suspend fun getOrCreateConversation(userId1: String, userId2: String): Conversation {
        // Prevent self-conversation
        if (userId1 == userId2) {
            throw AppException.Validation.InvalidInput("otherUserId", "Cannot create conversation with yourself")
        }
        
        // Order participants consistently (alphabetically by ID)
        val (participant1Id, participant2Id) = if (userId1 < userId2) {
            userId1 to userId2
        } else {
            userId2 to userId1
        }
        
        // Try to find existing conversation
        val existing = repository.findConversationByParticipants(participant1Id, participant2Id)
        if (existing != null) {
            return existing
        }
        
        // Create new conversation
        return repository.createConversation(participant1Id, participant2Id)
    }
    
    /**
     * Get a conversation by ID (with permission check)
     */
    suspend fun getConversation(conversationId: String, userId: String): Conversation {
        val conversation = repository.getConversationById(conversationId)
            ?: throw AppException.Business.ResourceNotFound("Conversation", conversationId)
        
        // Verify user is a participant
        if (!conversation.isParticipant(userId)) {
            throw AppException.Auth.Unauthorized()
        }
        
        return conversation
    }
    
    // ===== Messages =====
    
    /**
     * Get messages in a conversation with pagination
     */
    suspend fun getConversationMessages(
        conversationId: String,
        userId: String,
        page: Int,
        perPage: Int
    ): love.bside.app.data.repository.Page<Message> {
        // Verify user has access to conversation
        val conversation = getConversation(conversationId, userId)
        
        // Get paginated messages
        return repository.getMessages(conversationId, page, perPage)
    }
    
    /**
     * Send a message in a conversation
     */
    suspend fun sendMessage(
        conversationId: String,
        senderId: String,
        content: String,
        messageType: String = "text"
    ): Message {
        // Get conversation and verify access
        val conversation = getConversation(conversationId, senderId)
        
        // Validate message content
        validateMessageContent(content)
        
        // Determine receiver
        val receiverId = conversation.getOtherParticipantId(senderId)
        
        // Create message
        val now = Clock.System.now().toString()
        val message = repository.createMessage(
            conversationId = conversationId,
            senderId = senderId,
            receiverId = receiverId,
            content = content,
            messageType = MessageType.valueOf(messageType.uppercase()),
            sentAt = now
        )
        
        // Update conversation metadata
        repository.updateConversationLastMessage(
            conversationId = conversationId,
            lastMessageText = content,
            lastMessageAt = now,
            incrementUnreadFor = receiverId
        )
        
        return message
    }
    
    /**
     * Mark a message as read
     */
    suspend fun markMessageAsRead(messageId: String, userId: String) {
        val message = repository.getMessageById(messageId)
            ?: throw AppException.Business.ResourceNotFound("Message", messageId)
        
        // Only receiver can mark as read
        if (message.receiverId != userId) {
            throw AppException.Auth.Unauthorized()
        }
        
        // Don't mark if already read
        if (message.status == MessageStatus.READ) {
            return
        }
        
        val now = Clock.System.now().toString()
        
        // Update message status
        repository.updateMessageStatus(messageId, MessageStatus.READ, readAt = now)
        
        // Create read receipt
        repository.createReadReceipt(messageId, userId, now)
        
        // Decrement unread count for conversation
        repository.decrementUnreadCount(message.conversationId, userId)
    }
    
    /**
     * Mark multiple messages as read (batch operation)
     */
    suspend fun markMessagesAsRead(messageIds: List<String>, userId: String) {
        if (messageIds.isEmpty()) return
        
        messageIds.forEach { messageId ->
            try {
                markMessageAsRead(messageId, userId)
            } catch (e: Exception) {
                // Continue with other messages if one fails
            }
        }
    }
    
    /**
     * Edit a message
     */
    suspend fun editMessage(messageId: String, userId: String, newContent: String): Message {
        val message = repository.getMessageById(messageId)
            ?: throw AppException.Business.ResourceNotFound("Message", messageId)
        
        // Only sender can edit
        if (message.senderId != userId) {
            throw AppException.Auth.Unauthorized()
        }
        
        // Can only edit sent messages (not read yet ideally, but allowing for now)
        if (message.status == MessageStatus.FAILED) {
            throw AppException.Validation.InvalidInput("message", "Cannot edit failed message")
        }
        
        // Validate new content
        validateMessageContent(newContent)
        
        val now = Clock.System.now().toString()
        
        // Update message
        return repository.updateMessage(messageId, newContent, editedAt = now)
    }
    
    /**
     * Delete a message (soft delete)
     */
    suspend fun deleteMessage(messageId: String, userId: String) {
        val message = repository.getMessageById(messageId)
            ?: throw AppException.Business.ResourceNotFound("Message", messageId)
        
        // Only sender can delete
        if (message.senderId != userId) {
            throw AppException.Auth.Unauthorized()
        }
        
        val now = Clock.System.now().toString()
        
        // Soft delete
        repository.deleteMessage(messageId, deletedAt = now)
    }
    
    // ===== Typing Indicators =====
    
    /**
     * Update typing status for a user in a conversation
     */
    suspend fun updateTypingStatus(conversationId: String, userId: String, isTyping: Boolean) {
        // Verify user has access to conversation
        getConversation(conversationId, userId)
        
        val now = Clock.System.now().toString()
        
        // Update or create typing indicator
        repository.upsertTypingIndicator(conversationId, userId, isTyping, now)
    }
    
    // ===== Validation =====
    
    private fun validateMessageContent(content: String) {
        when {
            content.isBlank() -> 
                throw AppException.Validation.RequiredField("content")
            content.length > 5000 -> 
                throw AppException.Validation.InvalidInput("content", "Message too long (max 5000 characters)")
            containsForbiddenContent(content) -> 
                throw AppException.Validation.InvalidInput("content", "Message contains forbidden content")
        }
    }
    
    private fun containsForbiddenContent(content: String): Boolean {
        val forbiddenPatterns = listOf(
            "<script",
            "javascript:",
            "onerror=",
            "onclick="
        )
        
        return forbiddenPatterns.any { pattern ->
            content.lowercase().contains(pattern)
        }
    }
}
