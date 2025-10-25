package love.bside.app.ui.screens.messages

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import love.bside.app.data.mock.MockData
import love.bside.app.data.models.messaging.Conversation
import love.bside.app.ui.components.ConversationListItem
import love.bside.app.ui.components.EmptyState
import love.bside.app.ui.components.LoadingIndicator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationsListScreen(
    conversations: List<Conversation>,
    currentUserId: String,
    onConversationClick: (Conversation) -> Unit,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Messages") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        when {
            isLoading -> {
                LoadingIndicator(
                    message = "Loading conversations...",
                    modifier = Modifier.padding(paddingValues)
                )
            }
            conversations.isEmpty() -> {
                EmptyState(
                    icon = Icons.Default.Chat,
                    title = "No conversations yet",
                    message = "Start a conversation by discovering new people!",
                    actionLabel = "Discover",
                    onAction = { /* TODO: Navigate to discover tab */ },
                    modifier = Modifier.padding(paddingValues)
                )
            }
            else -> {
                LazyColumn(
                    modifier = modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    items(
                        items = conversations,
                        key = { it.id }
                    ) { conversation ->
                        // Get the other user's name from mock data
                        val otherUserId = conversation.getOtherParticipantId(currentUserId)
                        val otherUserName = MockData.getUserNameForConversation(otherUserId)
                        
                        ConversationListItem(
                            conversation = conversation,
                            currentUserId = currentUserId,
                            otherUserName = otherUserName,
                            onClick = { onConversationClick(conversation) }
                        )
                    }
                }
            }
        }
    }
}
