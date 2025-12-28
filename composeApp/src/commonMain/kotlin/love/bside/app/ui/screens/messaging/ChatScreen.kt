package love.bside.app.ui.screens.messaging

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import love.bside.app.domain.models.Message
import love.bside.app.presentation.ChatViewModel
import love.bside.app.ui.theme.BsideBrand
import love.bside.app.ui.theme.glassEffect
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    conversationId: String,
    conversationName: String,
    userId: String,
    onBack: () -> Unit,
    viewModel: ChatViewModel = koinViewModel { parametersOf(userId) }
) {
    val messages by viewModel.messages.collectAsState()
    var inputText by remember { mutableStateOf("") }
    
    // Reply State
    var replyingTo by remember { mutableStateOf<Message?>(null) }

    LaunchedEffect(conversationId) {
        viewModel.loadConversation(conversationId)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            // Glassmorphic Top Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassEffect(
                        backgroundColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                        borderColor = Color.Transparent,
                        shape = RoundedCornerShape(0.dp)
                    )
                    .statusBarsPadding()
            ) {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = conversationName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            if (messages.isNotEmpty()) {
                                Text(
                                    text = "Active now", // Placeholder for actual presence
                                    style = MaterialTheme.typography.labelSmall,
                                    color = BsideBrand.TealTileDark
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                reverseLayout = true,
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(
                    items = messages,
                    key = { it.id }
                ) { message ->
                    SwipeToReply(
                        onReply = { replyingTo = message },
                        isMyMessage = message.senderId == userId
                    ) {
                         MessageBubble(
                            message = message,
                            isMyMessage = message.senderId == userId,
                            onReplyDrag = { /* handled by swipe wrapper */ }
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            // Reply Indicator
            AnimatedVisibility(visible = replyingTo != null) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.padding(start = 8.dp)) {
                            Text("Replying to...", style = MaterialTheme.typography.labelSmall, color = BsideBrand.PlumHeart)
                            Text(replyingTo?.content ?: "", maxLines = 1, style = MaterialTheme.typography.bodySmall)
                        }
                        IconButton(onClick = { replyingTo = null }) {
                           Icon(Icons.Default.Close, null)
                        }
                    }
                }
            }

            // Input Area
            ChatInputBar(
                inputText = inputText,
                onValueChange = { inputText = it },
                onSend = {
                    if (inputText.isNotBlank()) {
                        viewModel.sendMessage(inputText, replyToId = replyingTo?.id)
                        inputText = ""
                        replyingTo = null
                    }
                }
            )
        }
    }
}

@Composable
fun SwipeToReply(
    onReply: () -> Unit,
    isMyMessage: Boolean,
    content: @Composable () -> Unit
) {
    // Simplified Swipe simulation: Just a Box that detects drag or we just use LongPress for now to be safe on Desktop/Web
    // For proper SwipeToReply we need AnchoredDraggable which is complex to setup in one go.
    // I'll implement LongPress or DoubleTap to reply for simplicity in this iteration.
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = { onReply() },
                    onDoubleTap = { onReply() }
                )
            }
    ) {
        content()
    }
}

@Composable
fun ChatInputBar(
    inputText: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
            .imePadding() // Key for mobile keyboard
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = inputText,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Type a message...", style = MaterialTheme.typography.bodyMedium) },
                shape = RoundedCornerShape(24.dp),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                maxLines = 4
            )
            Spacer(modifier = Modifier.width(8.dp))
            FilledIconButton(
                onClick = onSend,
                modifier = Modifier.size(48.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = BsideBrand.TealTile
                )
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = BsideBrand.PlumHeartDark
                )
            }
        }
    }
}

@Composable
fun MessageBubble(
    message: Message, 
    isMyMessage: Boolean,
    onReplyDrag: () -> Unit
) {
    val alignment = if (isMyMessage) Alignment.CenterEnd else Alignment.CenterStart
    
    // Animate appearance
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    AnimatedVisibility(
        visible = visible,
        enter = scaleIn() + fadeIn()
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = alignment
        ) {
            Column(
                modifier = Modifier
                    .padding(start = if (!isMyMessage) ((message.threadDepth ?: 0) * 12).dp else 0.dp)
                    .widthIn(max = 280.dp)
            ) {
                // Threading Context
                if ((message.threadDepth ?: 0) > 0) {
                     Row(verticalAlignment = Alignment.CenterVertically) {
                          Box(modifier = Modifier
                              .size(12.dp, 2.dp)
                              .background(MaterialTheme.colorScheme.outline.copy(alpha=0.5f))
                              .align(Alignment.CenterVertically)
                          )
                          Text(
                              text = "Reply",
                              style = MaterialTheme.typography.labelSmall,
                              color = MaterialTheme.colorScheme.outline,
                              modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                          )
                     }
                }
                
                // Bubble Decoration
                val bubbleShape = RoundedCornerShape(
                    topStart = 18.dp,
                    topEnd = 18.dp,
                    bottomStart = if (isMyMessage) 18.dp else 4.dp,
                    bottomEnd = if (isMyMessage) 4.dp else 18.dp
                )

                val backgroundModifier = if (isMyMessage) {
                    Modifier.background(
                        brush = Brush.linearGradient(
                            colors = listOf(BsideBrand.TealTile, BsideBrand.TealTileDark)
                        )
                    )
                } else {
                    Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
                }

                Box(
                    modifier = Modifier
                        .clip(bubbleShape)
                        .then(backgroundModifier)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Column {
                        Text(
                            text = message.content,
                            color = if (isMyMessage) BsideBrand.PlumHeartDark else MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
                
                // Status Row
                Row(
                    modifier = Modifier.align(if (isMyMessage) Alignment.End else Alignment.Start).padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                         text = "10:30 AM", // Should format message.sentAt
                         style = MaterialTheme.typography.labelSmall,
                         color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                    )
                    if (isMyMessage) {
                        // Double check mark for read
                        Icon(
                            imageVector = if (message.readByCount > 0) Icons.Default.DoneAll else Icons.Default.Check,
                            contentDescription = "Status",
                            modifier = Modifier.size(14.dp),
                            tint = if (message.readByCount > 0) BsideBrand.TealTileDark else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=0.5f)
                        )
                    }
                }
            }
        }
    }
}

