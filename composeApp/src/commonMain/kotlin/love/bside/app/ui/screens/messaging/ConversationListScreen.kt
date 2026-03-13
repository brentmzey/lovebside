package love.bside.app.ui.screens.messaging

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import love.bside.app.data.models.Conversation
import love.bside.app.data.repository.MessagingRepository
import love.bside.app.ui.components.BsideScaffold
import love.bside.app.ui.design.tokens.BsideColors
import love.bside.app.ui.design.tokens.BsideTypography
import org.koin.compose.koinInject

@Composable
fun ConversationListScreen(
    onNavigateToChat: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val repository: MessagingRepository = koinInject()
    var conversations by remember { mutableStateOf<List<Conversation>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        try {
            conversations = repository.getConversations()
        } catch (e: Exception) {
            // Handle error
        } finally {
            isLoading = false
        }
    }

    BsideScaffold(
        title = "Messages",
        onNavigateBack = onNavigateBack,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { /* TODO: New conversation flow */ },
                containerColor = BsideColors.Primary,
                contentColor = BsideColors.OnPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "New Message")
            }
        }
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BsideColors.Primary)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                items(conversations) { conversation ->
                    ConversationItem(
                        conversation = conversation,
                        onClick = { onNavigateToChat(conversation.id) }
                    )
                    HorizontalDivider(color = BsideColors.Divider)
                }
            }
        }
    }
}

@Composable
fun PresenceIndicator(
    isOnline: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.size(12.dp),
        shape = androidx.compose.foundation.shape.CircleShape,
        color = if (isOnline) BsideColors.Success else BsideColors.Neutral400,
        border = androidx.compose.foundation.BorderStroke(2.dp, BsideColors.Background)
    ) {}
}

@Composable
fun ConversationItem(
    conversation: Conversation,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar placeholder
        Box {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = androidx.compose.foundation.shape.CircleShape,
                color = BsideColors.Neutral200
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = conversation.name?.take(1) ?: "U",
                        style = BsideTypography.LabelLarge,
                        color = BsideColors.TextPrimary
                    )
                }
            }
            // TODO: Fetch real presence status
            PresenceIndicator(
                isOnline = true, // Placeholder
                modifier = Modifier.align(Alignment.BottomEnd)
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column {
            Text(
                text = conversation.name ?: "Unknown User",
                style = BsideTypography.BodyLarge,
                color = BsideColors.TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Last message preview...", // TODO: Add last message to Conversation model or fetch
                style = BsideTypography.BodySmall,
                color = BsideColors.TextSecondary
            )
        }
    }
}
