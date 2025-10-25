package love.bside.app.ui.screens.chat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import love.bside.app.data.models.Message
import love.bside.app.ui.components.*
import kotlinx.coroutines.launch

/**
 * Chat detail screen showing full conversation with message history
 * Features: message bubbles, typing indicator, send message, scroll to bottom
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    conversationId: String,
    otherUserName: String,
    messages: List<Message>,
    currentUserId: String,
    isTyping: Boolean = false,
    onSendMessage: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    
    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Avatar
                        Surface(
                            modifier = Modifier.size(40.dp),
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        Column {
                            Text(
                                text = otherUserName,
                                style = MaterialTheme.typography.titleMedium
                            )
                            if (isTyping) {
                                Text(
                                    text = "typing...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { /* TODO: Video call */ }) {
                        Icon(
                            imageVector = Icons.Default.VideoCall,
                            contentDescription = "Video call"
                        )
                    }
                    IconButton(onClick = { /* TODO: Voice call */ }) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "Voice call"
                        )
                    }
                    IconButton(onClick = { /* TODO: More options */ }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            MessageInputBar(
                message = messageText,
                onMessageChange = { messageText = it },
                onSend = {
                    if (messageText.isNotBlank()) {
                        onSendMessage(messageText)
                        messageText = ""
                    }
                },
                enabled = true
            )
        }
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (messages.isEmpty()) {
                // Empty state
                EmptyState(
                    icon = Icons.Default.Chat,
                    title = "Start the conversation",
                    message = "Send a message to ${otherUserName}",
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                // Messages list
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Group messages by date
                    val groupedMessages = messages.groupBy { message ->
                        formatDate(message.sentAt)
                    }
                    
                    groupedMessages.forEach { (date, messagesForDate) ->
                        // Date divider
                        item(key = "date_$date") {
                            MessageDateDivider(date = date)
                        }
                        
                        // Messages for this date
                        items(
                            items = messagesForDate,
                            key = { it.id }
                        ) { message ->
                            if (message.senderId == currentUserId) {
                                SentMessageBubble(
                                    message = message.content,
                                    timestamp = formatTime(message.sentAt),
                                    isSeen = message.isRead
                                )
                            } else {
                                ReceivedMessageBubble(
                                    message = message.content,
                                    timestamp = formatTime(message.sentAt)
                                )
                            }
                        }
                    }
                    
                    // Typing indicator
                    if (isTyping) {
                        item(key = "typing") {
                            TypingIndicator()
                        }
                    }
                }
                
                // Scroll to bottom button (when not at bottom)
                val showScrollToBottom by remember {
                    derivedStateOf {
                        listState.firstVisibleItemIndex > 5
                    }
                }
                
                if (showScrollToBottom) {
                    FloatingActionButton(
                        onClick = {
                            coroutineScope.launch {
                                listState.animateScrollToItem(messages.size - 1)
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp),
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Scroll to bottom"
                        )
                    }
                }
            }
        }
    }
}

/**
 * Message input bar at bottom of chat
 */
@Composable
private fun MessageInputBar(
    message: String,
    onMessageChange: (String) -> Unit,
    onSend: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Attachment button
            IconButton(
                onClick = { /* TODO: Attach photo */ },
                enabled = enabled
            ) {
                Icon(
                    imageVector = Icons.Default.AttachFile,
                    contentDescription = "Attach file",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Text field
            OutlinedTextField(
                value = message,
                onValueChange = onMessageChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Message...") },
                enabled = enabled,
                maxLines = 4,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )
            
            // Send button
            FilledIconButton(
                onClick = onSend,
                enabled = enabled && message.isNotBlank(),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send"
                )
            }
        }
    }
}

/**
 * Format date for divider (e.g., "Today", "Yesterday", "Jan 15")
 */
private fun formatDate(timestamp: String): String {
    // TODO: Implement proper date formatting
    // For now, return a placeholder
    return "Today"
}

/**
 * Format time for message timestamp (e.g., "2:30 PM", "Yesterday 3:45 PM")
 */
private fun formatTime(timestamp: String): String {
    // TODO: Implement proper time formatting
    // For now, return a placeholder
    return "2:30 PM"
}
