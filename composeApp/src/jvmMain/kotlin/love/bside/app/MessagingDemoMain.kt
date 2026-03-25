import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import kotlinx.datetime.Clock
import love.bside.app.ui.design.tokens.BsideTypography
import love.bside.app.ui.messaging.*

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "BSide Messaging Demo"
    ) {
        MessagingDemoApp()
    }
}

@Composable
@Preview
fun MessagingDemoApp() {
    MaterialTheme {
        var currentView by remember { mutableStateOf("conversations") }
        
        when (currentView) {
            "conversations" -> ConversationListDemo { currentView = "chat" }
            "chat" -> ChatDemo { currentView = "conversations" }
        }
    }
}

@Composable
fun ConversationListDemo(onClick: () -> Unit) {
    val conversations = remember {
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
            conversations = conversations,
            onConversationClick = { onClick() },
            modifier = Modifier.padding(padding)
        )
    }
}

@Composable
fun ChatDemo(onBack: () -> Unit) {
    var inputText by remember { mutableStateOf("") }
    val messages = remember {
        mutableStateListOf(
            Triple("Hello! How are you?", false, false),
            Triple("I'm doing great, thanks!", true, true),
            Triple("Would you like to grab coffee?", false, false),
            Triple("Sure! Tomorrow afternoon?", true, false)
        )
    }
    
    Scaffold(
        topBar = {
            Row(modifier = Modifier.padding(24.dp)) {
                TextButton(onClick = onBack) {
                    Text("← Back")
                }
                Spacer(Modifier.width(16.dp))
                Text(
                    text = "Alice Smith",
                    style = BsideTypography.HeadlineMedium
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(messages) { (content, isSent, isRead) ->
                    MessageBubble(
                        content = content,
                        timestamp = Clock.System.now(),
                        isSent = isSent,
                        senderInitials = if (isSent) "ME" else "AS",
                        isRead = isRead
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }
            
            MessageComposer(
                value = inputText,
                onValueChange = { inputText = it },
                onSend = {
                    messages.add(Triple(inputText, true, false))
                    inputText = ""
                }
            )
        }
    }
}
