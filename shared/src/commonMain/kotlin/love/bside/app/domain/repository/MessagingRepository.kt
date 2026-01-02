package love.bside.app.domain.repository

import love.bside.app.core.Result
import love.bside.app.domain.models.Conversation
import love.bside.app.domain.models.ConversationParticipant
import love.bside.app.domain.models.Message
import love.bside.app.domain.models.TypingStatus
import love.bside.app.domain.models.ProustQuestionnaire
import love.bside.app.domain.models.UserAnswer
import love.bside.app.domain.models.Match
import love.bside.app.domain.models.MessagingSettings
import kotlinx.coroutines.flow.Flow

data class AttachmentData(
    val fileName: String,
    val data: ByteArray,
    val mimeType: String
)

interface MessagingRepository {
    // Conversations
    suspend fun getConversations(userId: String): Result<List<Conversation>>
    suspend fun getConversation(conversationId: String): Result<Conversation>
    suspend fun createDirectConversation(participantIds: List<String>): Result<Conversation>
    suspend fun createGroupConversation(name: String, participantIds: List<String>): Result<Conversation>
    
    // Participants
    suspend fun getParticipants(conversationId: String): Result<List<ConversationParticipant>>
    suspend fun addParticipants(conversationId: String, userIds: List<String>): Result<Unit>
    suspend fun removeParticipant(conversationId: String, userId: String): Result<Unit>
    suspend fun updateParticipantSettings(
        conversationId: String,
        isMuted: Boolean? = null,
        isPinned: Boolean? = null
    ): Result<Unit>
    
    // Messages
    suspend fun getMessages(conversationId: String, page: Int = 1, perPage: Int = 50): Result<List<Message>>
    suspend fun sendMessage(
        conversationId: String,
        content: String,
        replyToMessageId: String? = null,
        attachments: List<AttachmentData>? = null
    ): Result<Message>
    suspend fun deleteMessage(messageId: String): Result<Unit>
    suspend fun markAsRead(conversationId: String): Result<Unit>
    
    // Threading
    suspend fun getReplies(messageId: String): Result<List<Message>>
    suspend fun getThreadRoot(messageId: String): Result<Message>
    suspend fun getFullThread(rootMessageId: String): Result<List<Message>>
    suspend fun countReplies(messageId: String): Result<Int>
    
    // Advanced Queries
    suspend fun searchMessages(query: String, conversationId: String): Result<List<Message>>
    suspend fun getMessagesAfter(
        conversationId: String,
        timestamp: kotlinx.datetime.Instant,
        limit: Int = 50
    ): Result<List<Message>>
    suspend fun getMessagesBefore(
        conversationId: String,
        timestamp: kotlinx.datetime.Instant,
        limit: Int = 50
    ): Result<List<Message>>
    
    // Real-time (Flow-based)
    fun subscribeToConversation(conversationId: String): Flow<Message>
    fun subscribeToTypingIndicators(conversationId: String): Flow<TypingStatus>
    suspend fun setTypingStatus(conversationId: String, isTyping: Boolean): Result<Unit>

    // Proust Questionnaire
    suspend fun getQuestionnaire(): Result<List<ProustQuestionnaire>>
    suspend fun getUserAnswers(): Result<List<UserAnswer>>
    suspend fun submitQuestionnaireResponse(questionId: String, answer: String): Result<UserAnswer>
    
    // Matching
    suspend fun getMatches(): Result<List<Match>>
    
    // Settings
    suspend fun getGlobalSettings(): Result<MessagingSettings>
    suspend fun updateGlobalSettings(settings: MessagingSettings): Result<Unit>
}
