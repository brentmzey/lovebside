package love.bside.app.ui.screens.messaging

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.datetime.Clock
import love.bside.app.ui.design.tokens.BsideTypography
import love.bside.app.ui.messaging.ConversationItem
import love.bside.app.ui.messaging.ConversationList
import love.bside.app.ui.messaging.MessageBubble
import love.bside.app.ui.messaging.MessageComposer

/**
 * Demo screen showcasing the new messaging UI components.
 * 
 * This demonstrates:
 * - MessageBubble with gradients and animations
 * - ConversationList with avatars and badges
 * - MessageComposer with send button
 */
@Composable
fun MessagingDemo(
    onBack: () -> Unit = {}
) {
    var currentView by remember { mutableStateOf(DemoView.CONVERSATION_LIST) }
    
    when (currentView) {
        DemoView.CONVERSATION_LIST -> ConversationListDemo(
            onConversationClick = { currentView = DemoView.CHAT }
        )
        DemoView.CHAT -> ChatDemo(
            onBack = { currentView = DemoView.CONVERSATION_LIST }
        )
    }
}

enum class DemoView {
    CONVERSATION_LIST,
    CHAT
}

@Composable
private fun ConversationListDemo(
    onConversationClick: (ConversationItem) -> Unit
) {
    val sampleConversations = remember {
        listOf(
            ConversationItem(
                id = "1",
                title = "Alice Smith",
                lastMessage = "Hey! How are you doing?",
                timestamp = Clock.System.now(),
                avatarInitials = "AS",
                isOnline = true,
                unreadCount = 3
            ),
            ConversationItem(
                id = "2",
                title = "Bob Johnson",
                lastMessage = "Let's meet up tomorrow!",
                timestamp = Clock.System.now(),
                avatarInitials = "BJ",
                isOnline = false,
                unreadCount = 0
            ),
            ConversationItem(
                id = "3",
                title = "Carol Davis",
                lastMessage = "Thanks for your help 🙏",
                timestamp = Clock.System.now(),
                avatarInitials = "CD",
                isOnline = true,
                unreadCount = 1
            ),
            ConversationItem(
                id = "4",
                title = "David Wilson",
                lastMessage = "See you later!",
                timestamp = Clock.System.now(),
                avatarInitials = "DW",
                isOnline = false,
                unreadCount = 0
            )
        )
    }
    
    Scaffold(
        topBar = {
            Text(
                text = "Messages",
                style = BsideTypography.HeadlineLarge,
                modifier = Modifier.padding(24.dp)
            )
        }
    ) { padding ->
        ConversationList(
            conversations = sampleConversations,
            onConversationClick = onConversationClick,
            modifier = Modifier.padding(padding)
        )
    }
}

@Composable
private fun ChatDemo(
    onBack: () -> Unit
) {
    val sampleMessages = remember {
        listOf(
            Triple("Hello! How are you?", false, false),
            Triple("I'm doing great, thanks for asking!", true, true),
            Triple("Would you like to grab coffee sometime?", false, false),
            Triple("Sure, I'd love to! How about tomorrow afternoon?", true, false)
        )
    }
    
    var inputText by remember { mutableStateOf("") }
    val messages = remember { mutableStateListOf(*sampleMessages.toTypedArray()) }
    
    Scaffold(
        topBar = {
            Text(
                text = "Alice Smith",
                style = BsideTypography.HeadlineMedium,
                modifier = Modifier.padding(24.dp)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Messages list
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(messages) { (content, isSent, isRead) ->
                    MessageBubble(
                        content = content,
                        timestamp = Clock.System.now(),
                        isSent = isSent,
                        showAvatar = true,
                        senderInitials = if (isSent) "ME" else "AS",
                        isRead = isRead
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
            
            // Message composer
            MessageComposer(
                value = inputText,
                onValueChange = { inputText = it },
                onSend = {
                    if (inputText.isNotBlank()) {
                        messages.add(Triple(inputText, true, false))
                        inputText = ""
                    }
                },
                showAttachmentButton = true
            )
        }
    }
}
