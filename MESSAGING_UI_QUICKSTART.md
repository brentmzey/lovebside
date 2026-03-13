# 🎨 BSide Messaging UI - Quick Start Guide

## ✅ Backend Verified - Ready to Build UI

Your backend is **fully functional**. This guide shows you how to use it from the UI.

## 📦 What You Have

### Repository (Shared KMM Code)
Location: `shared/src/commonMain/kotlin/love/bside/app/data/repository/MessagingRepository.kt`

All methods work and are tested:
```kotlin
class MessagingRepository(val pb: PocketBase) {
    // ✅ Conversations
    suspend fun getConversations(): List<Conversation>
    suspend fun createConversation(participants: List<String>, type: ConversationType)
    
    // ✅ Messages
    suspend fun getMessages(conversationId: String): List<Message>
    suspend fun sendMessage(conversationId: String, text: String): Message
    suspend fun sendMessage(conversationId, text, replyToId, threadRootId): Message
    fun observeMessages(conversationId: String): Flow<Message>
    
    // ✅ Reactions
    suspend fun addReaction(messageId: String, reaction: String): Reaction
    suspend fun removeReaction(messageId: String, reaction: String)
    
    // ✅ Presence
    suspend fun setPresence(status: PresenceStatus, activityMessage: String?): Presence
    suspend fun getPresence(userId: String): Presence?
    
    // ✅ Attachments
    suspend fun uploadAttachment(conversationId, file, type): Message
}
```

### UI Components (Already Created)
Location: `shared/src/commonMain/kotlin/love/bside/app/ui/messaging/`
- `MessageBubble.kt` - Individual message display
- `MessageComposer.kt` - Input field
- `ConversationList.kt` - Conversation list

## 🚀 Quick Integration Example

### 1. Create ViewModel
```kotlin
class ConversationViewModel(
    private val conversationId: String,
    private val messagingRepo: MessagingRepository
) : ViewModel() {
    
    // Real-time message flow
    val messages = messagingRepo
        .observeMessages(conversationId)
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    
    fun sendMessage(text: String) {
        viewModelScope.launch {
            messagingRepo.sendMessage(conversationId, text)
        }
    }
    
    fun sendReply(text: String, replyTo: Message) {
        viewModelScope.launch {
            messagingRepo.sendMessage(
                conversationId = conversationId,
                text = text,
                replyToId = replyTo.id,
                threadRootId = replyTo.threadRootId ?: replyTo.id
            )
        }
    }
    
    fun addReaction(messageId: String, emoji: String) {
        viewModelScope.launch {
            messagingRepo.addReaction(messageId, emoji)
        }
    }
}
```

### 2. Create Screen
```kotlin
@Composable
fun ConversationScreen(
    viewModel: ConversationViewModel
) {
    val messages by viewModel.messages.collectAsState()
    var replyingTo by remember { mutableStateOf<Message?>(null) }
    
    Column(Modifier.fillMaxSize()) {
        // Messages List
        LazyColumn(
            modifier = Modifier.weight(1f),
            reverseLayout = true
        ) {
            items(messages) { message ->
                MessageBubble(
                    message = message,
                    onReply = { replyingTo = message },
                    onReact = { emoji -> viewModel.addReaction(message.id, emoji) }
                )
            }
        }
        
        // Reply Preview
        replyingTo?.let { message ->
            ReplyPreview(
                message = message,
                onDismiss = { replyingTo = null }
            )
        }
        
        // Message Composer
        MessageComposer(
            onSend = { text ->
                if (replyingTo != null) {
                    viewModel.sendReply(text, replyingTo!!)
                    replyingTo = null
                } else {
                    viewModel.sendMessage(text)
                }
            }
        )
    }
}
```

### 3. Enhanced MessageBubble with Threading
```kotlin
@Composable
fun MessageBubble(
    message: Message,
    onReply: () -> Unit,
    onReact: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        // Show reply reference if threaded
        if (message.replyToMessageId != null) {
            Row(
                modifier = Modifier.padding(start = 32.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Reply,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    "Reply to message",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        // Message bubble
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.combinedClickable(
                onClick = {},
                onLongClick = onReply
            )
        ) {
            Column(Modifier.padding(12.dp)) {
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyLarge
                )
                
                // Reactions
                if (message.reactions.isNotEmpty()) {
                    FlowRow(
                        modifier = Modifier.padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        message.reactions.forEach { (emoji, count) ->
                            ReactionChip(emoji, count) { onReact(emoji) }
                        }
                    }
                }
            }
        }
    }
}
```

## 🎨 UI Features to Implement

### Phase 1: Core Screens (1-2 days)
- [ ] `ConversationsListScreen` - Show all conversations
- [ ] `ConversationScreen` - Message thread view
- [ ] Navigation between screens

### Phase 2: Threading UI (1 day)
- [ ] Visual thread indicators (lines, indentation)
- [ ] Reply preview when composing
- [ ] Thread collapse/expand
- [ ] Jump to parent message

### Phase 3: Reactions (1 day)
- [ ] Reaction picker bottom sheet
- [ ] Animated reaction addition
- [ ] Reaction summary dialog
- [ ] Quick reactions (long-press menu)

### Phase 4: Presence & Typing (1 day)
- [ ] Online/offline indicators
- [ ] "Last seen" timestamps
- [ ] Typing indicator animation
- [ ] Activity status display

### Phase 5: Polish (2-3 days)
- [ ] Smooth scroll animations
- [ ] Message send animation
- [ ] Swipe gestures
- [ ] Haptic feedback
- [ ] Loading states
- [ ] Error states
- [ ] Pull-to-refresh

## 📱 Testing Your UI

1. **Start backend:**
   ```bash
   ./scripts/verify-messaging-backend.sh
   ```

2. **Run your app:**
   ```bash
   ./gradlew :composeApp:run
   ```

3. **Verify features:**
   - Send messages ✓
   - See real-time updates ✓
   - Create threads ✓
   - Add reactions ✓
   - Check presence ✓

## 🎯 Example: Beautiful Thread Visualization

```kotlin
@Composable
fun ThreadedMessageList(messages: List<Message>) {
    LazyColumn {
        messages.forEach { message ->
            item {
                ThreadedMessageItem(
                    message = message,
                    depth = message.threadDepth ?: 0
                )
            }
        }
    }
}

@Composable
fun ThreadedMessageItem(message: Message, depth: Int) {
    Row {
        // Thread line indicators
        repeat(depth) {
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.outline)
            )
            Spacer(Modifier.width(16.dp))
        }
        
        // Message content
        MessageBubble(message)
    }
}
```

## 🚀 Real-Time Updates

Messages arrive automatically via Flow:
```kotlin
// In your ViewModel
val messages = messagingRepo
    .observeMessages(conversationId)
    .map { message -> 
        // Transform or enrich message
        message.copy(reactions = getReactions(message.id))
    }
    .stateIn(scope, SharingStarted.Lazily, emptyList())
```

No polling needed! SSE handles real-time updates. 🎉

## 📚 Resources

- **Backend Status:** `README_MESSAGING_STATUS.md`
- **Full Verification:** `MESSAGING_COMPLETE_VERIFICATION.md`
- **Test Scripts:** `scripts/verify-messaging-backend.sh`
- **Repository Code:** `shared/src/commonMain/.../MessagingRepository.kt`
- **UI Components:** `shared/src/commonMain/kotlin/love/bside/app/ui/messaging/`

## ✨ You're All Set!

The backend works, the SDK works, the repository works. Now go make it beautiful! 🎨✨

**Happy coding! 🚀**
