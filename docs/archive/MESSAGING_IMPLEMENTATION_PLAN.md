# Real-Time Messaging Implementation Plan

**Feature**: 1-to-1 Real-Time Messaging  
**Technology**: PocketBase Real-time (SSE)  
**Date**: January 19, 2025

---

## 🎯 Overview

Implementing a complete real-time messaging system with:
- ✅ 1-to-1 direct messages
- ✅ Real-time delivery (Server-Sent Events)
- ✅ Read receipts
- ✅ Typing indicators (debounced)
- ✅ Message history & pagination
- ✅ Online/offline status
- ✅ Hungarian notation schema prefix: `m_` for messaging

---

## 📊 Database Schema (PocketBase)

### Collections with Prefix Convention

| Collection | Prefix | Purpose | Real-time |
|------------|--------|---------|-----------|
| `m_conversations` | m_ | 1-to-1 conversation metadata | ✅ Subscribe |
| `m_messages` | m_ | Individual messages | ✅ Subscribe |
| `m_typing_indicators` | m_ | Typing status (ephemeral) | ✅ Subscribe |
| `m_read_receipts` | m_ | Message read tracking | ✅ Subscribe |

### Schema Details

#### m_conversations
```javascript
{
  "name": "m_conversations",
  "fields": [
    { "name": "participant1Id", "type": "relation", "required": true },  // User 1
    { "name": "participant2Id", "type": "relation", "required": true },  // User 2
    { "name": "lastMessageId", "type": "relation" },                     // Latest message
    { "name": "lastMessageAt", "type": "date" },                         // Timestamp
    { "name": "participant1UnreadCount", "type": "number", "default": 0 },
    { "name": "participant2UnreadCount", "type": "number", "default": 0 }
  ],
  "indexes": [
    "CREATE UNIQUE INDEX idx_conversation_participants ON m_conversations(participant1Id, participant2Id)",
    "CREATE INDEX idx_conversation_lastMessage ON m_conversations(lastMessageAt DESC)"
  ]
}
```

#### m_messages
```javascript
{
  "name": "m_messages",
  "fields": [
    { "name": "conversationId", "type": "relation", "required": true },  // Parent conversation
    { "name": "senderId", "type": "relation", "required": true },        // Who sent it
    { "name": "receiverId", "type": "relation", "required": true },      // Who receives it
    { "name": "content", "type": "text", "required": true },             // Message text
    { "name": "messageType", "type": "select", "values": ["text", "image", "system"] },
    { "name": "status", "type": "select", "values": ["sent", "delivered", "read"] },
    { "name": "sentAt", "type": "date", "required": true },
    { "name": "deliveredAt", "type": "date" },
    { "name": "readAt", "type": "date" }
  ],
  "indexes": [
    "CREATE INDEX idx_message_conversation ON m_messages(conversationId, sentAt DESC)",
    "CREATE INDEX idx_message_sender ON m_messages(senderId)",
    "CREATE INDEX idx_message_status ON m_messages(status)"
  ]
}
```

#### m_typing_indicators
```javascript
{
  "name": "m_typing_indicators",
  "fields": [
    { "name": "conversationId", "type": "relation", "required": true },
    { "name": "userId", "type": "relation", "required": true },
    { "name": "isTyping", "type": "bool", "default": false },
    { "name": "lastUpdated", "type": "date", "required": true }
  ],
  "indexes": [
    "CREATE UNIQUE INDEX idx_typing_user ON m_typing_indicators(conversationId, userId)"
  ]
}
```

#### m_read_receipts
```javascript
{
  "name": "m_read_receipts",
  "fields": [
    { "name": "messageId", "type": "relation", "required": true },
    { "name": "userId", "type": "relation", "required": true },
    { "name": "readAt", "type": "date", "required": true }
  ],
  "indexes": [
    "CREATE UNIQUE INDEX idx_receipt_message_user ON m_read_receipts(messageId, userId)"
  ]
}
```

---

## 🔄 Real-time Event Flow

```
User A Types Message
        ↓
Client sends typing indicator (debounced 300ms)
        ↓
PocketBase m_typing_indicators updated
        ↓
User B subscribed → receives typing event
        ↓
User B sees "User A is typing..."
        ↓
User A sends message
        ↓
Server validates & creates m_messages record
        ↓
PocketBase real-time event triggered
        ↓
User B subscribed → receives new message
        ↓
User B sees message instantly
        ↓
User B reads message
        ↓
m_read_receipts created
        ↓
User A receives read receipt event
        ↓
User A sees "Read" status
```

---

## 🏗️ Implementation Architecture

### 1. Kotlin Models (Shared)

```kotlin
shared/src/commonMain/kotlin/love/bside/app/data/models/messaging/
├── Conversation.kt
├── Message.kt
├── TypingIndicator.kt
├── ReadReceipt.kt
└── MessageStatus.kt
```

### 2. API Client (Shared)

```kotlin
shared/src/commonMain/kotlin/love/bside/app/data/api/
└── MessagingApiClient.kt
    - suspend fun getConversations(): Result<List<Conversation>>
    - suspend fun getOrCreateConversation(otherUserId): Result<Conversation>
    - suspend fun sendMessage(conversationId, content): Result<Message>
    - suspend fun getMessages(conversationId, page): Result<Page<Message>>
    - suspend fun markAsRead(messageId): Result<Unit>
    - suspend fun updateTypingStatus(conversationId, isTyping): Result<Unit>
    - fun subscribeToConversation(conversationId): Flow<MessageEvent>
    - fun subscribeToTypingIndicators(conversationId): Flow<TypingEvent>
```

### 3. Repository (Shared)

```kotlin
shared/src/commonMain/kotlin/love/bside/app/data/repository/
└── MessagingRepository.kt
    - Uses MessagingApiClient
    - Caches conversations
    - Manages real-time subscriptions
    - Handles offline queue
```

### 4. Server Routes

```kotlin
server/src/main/kotlin/love/bside/server/routes/api/v1/
└── MessagingRoutes.kt
    - GET    /api/v1/conversations
    - GET    /api/v1/conversations/:id
    - POST   /api/v1/conversations
    - GET    /api/v1/conversations/:id/messages
    - POST   /api/v1/conversations/:id/messages
    - POST   /api/v1/messages/:id/read
    - POST   /api/v1/conversations/:id/typing
```

### 5. Server Services

```kotlin
server/src/main/kotlin/love/bside/server/services/
└── MessagingService.kt
    - createConversation(user1Id, user2Id)
    - sendMessage(conversationId, senderId, content)
    - markMessageAsRead(messageId, userId)
    - updateTypingStatus(conversationId, userId, isTyping)
    - getConversationMessages(conversationId, page)
```

### 6. Server Repositories

```kotlin
server/src/main/kotlin/love/bside/server/repositories/
└── MessagingRepository.kt
    - Interacts with PocketBase
    - Manages real-time subscriptions
    - Updates conversation metadata
```

### 7. ViewModels (Shared)

```kotlin
shared/src/commonMain/kotlin/love/bside/app/presentation/messaging/
├── ConversationListViewModel.kt
│   - conversationsFlow: StateFlow<List<Conversation>>
│   - loadConversations()
│   - subscribeToUpdates()
│
└── ChatViewModel.kt
    - messagesFlow: StateFlow<List<Message>>
    - typingStatusFlow: StateFlow<TypingStatus>
    - sendMessage(text)
    - markAsRead(messageId)
    - updateTypingStatus(isTyping)
    - subscribeToMessages()
    - subscribeToTypingIndicators()
```

### 8. UI Screens (Platform-Specific)

```kotlin
composeApp/src/commonMain/kotlin/love/bside/app/ui/messaging/
├── ConversationListScreen.kt
│   - List of conversations
│   - Last message preview
│   - Unread count badges
│   - Online status indicators
│
└── ChatScreen.kt
    - Message list (reversed)
    - Message input field
    - Send button
    - Typing indicator ("User is typing...")
    - Read receipts (checkmarks)
    - Auto-scroll to bottom
```

---

## 🎨 Features Implementation

### Typing Indicator (Debounced)

```kotlin
// Client-side debouncing
class ChatViewModel {
    private val typingDebouncer = Debouncer(300.milliseconds)
    
    fun onTextChanged(text: String) {
        if (text.isNotEmpty()) {
            typingDebouncer.debounce {
                updateTypingStatus(true)
            }
        } else {
            updateTypingStatus(false)
        }
    }
}

// Debouncer utility
class Debouncer(private val delay: Duration) {
    private var job: Job? = null
    
    fun debounce(action: suspend () -> Unit) {
        job?.cancel()
        job = viewModelScope.launch {
            delay(delay)
            action()
        }
    }
}
```

### Read Receipts

```kotlin
// Automatic read receipt on message view
@Composable
fun MessageItem(message: Message) {
    LaunchedEffect(message.id) {
        if (!message.isRead && message.receiverId == currentUserId) {
            chatViewModel.markAsRead(message.id)
        }
    }
    
    // Display message with status
    Row {
        Text(message.content)
        when (message.status) {
            MessageStatus.SENT -> Icon(Icons.Default.Check)
            MessageStatus.DELIVERED -> Icon(Icons.Default.DoneAll)
            MessageStatus.READ -> Icon(Icons.Default.DoneAll, tint = Color.Blue)
        }
    }
}
```

### Real-time Subscriptions

```kotlin
// PocketBase real-time subscription
fun subscribeToConversation(conversationId: String): Flow<MessageEvent> = flow {
    val subscription = pocketBaseClient.collection("m_messages")
        .subscribe("*", filter = "conversationId='$conversationId'")
    
    subscription.collect { event ->
        when (event.action) {
            "create" -> emit(MessageEvent.New(event.record.toMessage()))
            "update" -> emit(MessageEvent.Updated(event.record.toMessage()))
            "delete" -> emit(MessageEvent.Deleted(event.record.id))
        }
    }
}.flowOn(Dispatchers.IO)
```

---

## 🔐 Security & Validation

### Access Rules (PocketBase)

```javascript
// m_conversations
{
  "listRule": "@request.auth.id = participant1Id || @request.auth.id = participant2Id",
  "viewRule": "@request.auth.id = participant1Id || @request.auth.id = participant2Id",
  "createRule": "@request.auth.id != ''",
  "updateRule": "@request.auth.id = participant1Id || @request.auth.id = participant2Id",
  "deleteRule": null  // Only admins can delete
}

// m_messages
{
  "listRule": "@request.auth.id = senderId || @request.auth.id = receiverId",
  "viewRule": "@request.auth.id = senderId || @request.auth.id = receiverId",
  "createRule": "@request.auth.id = senderId",
  "updateRule": "@request.auth.id = senderId && status = 'sent'",  // Can only edit sent messages
  "deleteRule": "@request.auth.id = senderId"  // Can delete own messages
}
```

### Validation Rules

```kotlin
// Message content validation
object MessageValidators {
    fun validateMessage(content: String): ValidationResult {
        return when {
            content.isBlank() -> 
                ValidationResult.Invalid(AppException.Validation.RequiredField("Message"))
            content.length > 5000 -> 
                ValidationResult.Invalid(AppException.Validation.InvalidInput("Message", "max 5000 characters"))
            content.contains("<script>") -> 
                ValidationResult.Invalid(AppException.Validation.InvalidInput("Message", "contains forbidden content"))
            else -> ValidationResult.Valid
        }
    }
}
```

---

## 📊 Performance Optimizations

### 1. Message Pagination
- Load 50 messages at a time
- Infinite scroll for history
- Cache recent messages locally

### 2. Typing Indicator Debouncing
- 300ms debounce on typing events
- Auto-clear after 3 seconds of inactivity
- Only send if status changes

### 3. Read Receipt Batching
- Batch multiple read receipts
- Send max once per second
- Only for visible messages

### 4. Connection Management
- Reconnect on network restore
- Queue messages while offline
- Sync on reconnection

---

## 🧪 Testing Strategy

### Unit Tests
```kotlin
class MessagingRepositoryTest {
    @Test fun `send message creates record and updates conversation`()
    @Test fun `typing indicator debounces correctly`()
    @Test fun `read receipt marks message as read`()
    @Test fun `messages are paginated correctly`()
}
```

### Integration Tests
```kotlin
class MessagingIntegrationTest {
    @Test fun `user can send message to another user`()
    @Test fun `real-time events are received`()
    @Test fun `typing indicator shows and hides correctly`()
    @Test fun `read receipts update message status`()
}
```

---

## 🚀 Implementation Steps

### Phase 1: Database & Models (1-2 hours)
1. Create PocketBase migration
2. Create Kotlin data models
3. Create model mappers
4. Test schema locally

### Phase 2: Server Implementation (2-3 hours)
5. Create server repositories
6. Create messaging service
7. Create API routes
8. Add validation middleware
9. Test with Postman/curl

### Phase 3: Client API (2-3 hours)
10. Create MessagingApiClient
11. Implement real-time subscriptions
12. Create MessagingRepository
13. Add caching layer
14. Test API calls

### Phase 4: ViewModels (2-3 hours)
15. Create ConversationListViewModel
16. Create ChatViewModel
17. Implement typing debouncer
18. Add state management
19. Test with mock data

### Phase 5: UI Implementation (3-4 hours)
20. Create ConversationListScreen
21. Create ChatScreen
22. Add typing indicator UI
23. Add read receipt indicators
24. Style and polish

### Phase 6: Real-time Integration (2-3 hours)
25. Wire up PocketBase subscriptions
26. Test real-time messaging
27. Test typing indicators
28. Test read receipts
29. Handle edge cases

### Phase 7: Testing & Polish (2-3 hours)
30. Write unit tests
31. Write integration tests
32. Test on multiple platforms
33. Performance optimization
34. Bug fixes

**Total Estimated Time**: 14-21 hours

---

## 📝 Success Criteria

✅ Users can send/receive messages in real-time  
✅ Typing indicators work with debouncing  
✅ Read receipts update correctly  
✅ Message history loads with pagination  
✅ Works on all platforms (iOS, Android, Web, Desktop)  
✅ Offline messages queue and send when online  
✅ Real-time events have <500ms latency  
✅ UI is responsive and smooth  
✅ All tests passing  

---

**Next Step**: Create the PocketBase migration file!
