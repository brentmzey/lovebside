package love.bside.app.ui.screens.messaging

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.vinceglb.filekit.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.core.PickerType
import kotlinx.coroutines.launch
import love.bside.app.domain.repository.AttachmentData
import love.bside.app.domain.repository.AuthRepository
import love.bside.app.domain.repository.MessagingRepository
import love.bside.app.ui.components.BsideScaffold
import love.bside.app.ui.messaging.MessageBubble
import love.bside.app.ui.design.tokens.BsideColors
import love.bside.app.ui.messaging.MessageComposer
import org.koin.compose.koinInject

@Composable
fun ChatScreen(conversationId: String, onNavigateBack: () -> Unit) {
    val repository: MessagingRepository = koinInject()
    val authRepository: AuthRepository = koinInject()

    // Get userId for ViewModel injection
    var currentUserId by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { currentUserId = authRepository.getCurrentUserId() ?: "" }

    // Inject ViewModel with parameters
    // We pass currentUserId. usage relies on it being available or handled if empty initially.
    val viewModel =
            org.koin.compose.viewmodel.koinViewModel<love.bside.app.presentation.ChatViewModel> {
                org.koin.core.parameter.parametersOf(currentUserId)
            }

    val messages by viewModel.messages.collectAsState()
    val typingStatus by viewModel.typingStatus.collectAsState()

    // Derived state for typing
    val isTyping = typingStatus[conversationId] ?: false

    // Setup ViewModel
    LaunchedEffect(conversationId) { viewModel.loadConversation(conversationId) }

    // File launcher
    val scope = rememberCoroutineScope()
    val launcher =
            rememberFilePickerLauncher(type = PickerType.ImageAndVideo) { file ->
                file?.let {
                    scope.launch {
                        try {
                            val bytes = it.readBytes()
                            val mimeType = "application/octet-stream" // Default/Fallback
                            val attachment =
                                    AttachmentData(
                                            fileName = it.name,
                                            data = bytes,
                                            mimeType = mimeType
                                    )
                            viewModel.sendAttachment(attachment)
                        } catch (e: Exception) {
                            println("Failed to upload: $e")
                        }
                    }
                }
            }

    val listState = rememberLazyListState()
    var replyToMessage by remember { mutableStateOf<love.bside.app.domain.models.Message?>(null) }

    // Auto-scroll on new message
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1) // Using size-1 for last index
        }
    }

    BsideScaffold(
            title = "Chat", // TODO: Fetch conversation name
            onNavigateBack = onNavigateBack
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages) { message ->
                    val isMe = message.senderId == currentUserId
                    
                    if (!isMe && !message.isRead) {
                        LaunchedEffect(message.id) {
                            viewModel.markAsRead(message.id)
                        }
                    }

                    MessageBubble(
                            modifier =
                                    Modifier.padding(
                                            start =
                                                    if (!isMe) ((message.threadDepth ?: 0) * 12).dp
                                                    else 0.dp
                                    ),
                            message = message,
                            isMyMessage = isMe,
                            onReplyClick = { replyToMessage = message }
                    )
                }

                // Typing indicator
                if (isTyping) {
                    item {
                        Text(
                                text = "typing...",
                                style =
                                        androidx.compose.material3.MaterialTheme.typography
                                                .bodySmall,
                                color = BsideColors.Primary,
                                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                        )
                    }
                }
            }

            // Reply preview
            replyToMessage?.let { message ->
                ReplyPreview(
                    message = message,
                    onCancel = { replyToMessage = null }
                )
            }

            var messageInput by remember { mutableStateOf("") }

            MessageComposer(
                    value = messageInput,
                    onValueChange = {
                        messageInput = it
                        viewModel.onTyping(it)
                    },
                    onAttachClick = { launcher.launch() },
                    onSend = {
                        if (messageInput.isNotBlank()) {
                            viewModel.sendMessage(
                                content = messageInput,
                                replyToId = replyToMessage?.id
                            )
                            messageInput = ""
                            replyToMessage = null // Clear reply after sending
                        }
                    }
            )
        }
    }
}

@Composable
fun ReplyPreview(
    message: love.bside.app.domain.models.Message,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .background(BsideColors.Background, shape = RoundedCornerShape(8.dp))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Reply,
            contentDescription = "Replying",
            tint = BsideColors.TextSecondary,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text("Replying to ${message.senderId}", style = MaterialTheme.typography.labelMedium, color = BsideColors.Primary)
            Text(message.content, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        IconButton(onClick = onCancel) {
            Icon(imageVector = Icons.Default.Close, contentDescription = "Cancel Reply", tint = BsideColors.TextSecondary)
        }
    }
}
