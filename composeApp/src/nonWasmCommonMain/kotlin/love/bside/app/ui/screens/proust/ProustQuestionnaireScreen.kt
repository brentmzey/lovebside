package love.bside.app.ui.screens.proust

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import io.pocketbase.config.RealtimeTransportKind
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import love.bside.app.core.appConfig

@Composable
fun ProustQuestionnaireScreen(
    modifier: Modifier = Modifier,
    controller: ProustQuestionnaireController = rememberProustController()
) {
    val uiState by controller.state.collectAsState()
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        RealtimeHeader(uiState = uiState, controller = controller)

        when {
            uiState.isLoading -> {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            uiState.errorMessage != null && uiState.questions.isEmpty() -> {
                ErrorCard(message = uiState.errorMessage ?: "Unable to load prompts", onRetry = controller::refresh)
            }

            else -> {
                QuestionnaireContent(uiState = uiState, controller = controller)
                if (uiState.errorMessage != null) {
                    InlineError(message = uiState.errorMessage ?: "", onRetry = controller::refresh)
                }
            }
        }

        if (uiState.answers.isNotEmpty()) {
            AnswerHistory(uiState = uiState, onJump = controller::jumpToQuestion)
        }
    }
}

@Composable
fun rememberProustController(
    baseUrl: String = appConfig().pocketBaseUrl
): ProustQuestionnaireController {
    val sanitized = remember(baseUrl) { sanitizePocketBaseBaseUrl(baseUrl) }
    val controller = remember(sanitized) { ProustQuestionnaireController(sanitized) }
    DisposableEffect(controller) {
        onDispose { controller.dispose() }
    }
    return controller
}

@Composable
private fun RealtimeHeader(
    uiState: ProustQuestionnaireUiState,
    controller: ProustQuestionnaireController
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Proust Questionnaire",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Explore the stories, memories, and values that define you.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        TransportBadge(transport = uiState.activeTransport, isConnected = uiState.isRealtimeConnected)

        RowWithSwitch(
            checked = uiState.isRealtimeEnabled,
            onCheckedChange = controller::setRealtimeEnabled
        )
    }
}

@Composable
private fun RowWithSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = "Realtime updates", fontWeight = FontWeight.Medium)
            Text(
                text = "Live SSE with smart polling fallback",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun TransportBadge(transport: RealtimeTransportKind, isConnected: Boolean) {
    val label = when {
        !isConnected -> "Realtime paused"
        transport == RealtimeTransportKind.SMART_POLLING -> "Live · Smart polling"
        transport == RealtimeTransportKind.SSE -> "Live · SSE"
        else -> "Realtime inactive"
    }
    SuggestionChip(
        onClick = {},
        label = { Text(label) },
        enabled = false
    )
}

@Composable
private fun QuestionnaireContent(
    uiState: ProustQuestionnaireUiState,
    controller: ProustQuestionnaireController
) {
    val question = uiState.currentQuestion
    if (question == null) {
        Text("Prompts will appear here once they're published.")
        return
    }

    LinearProgressIndicator(
        progress = { uiState.completionPercent },
        modifier = Modifier.fillMaxWidth()
    )
    Text(
        text = "Progress ${uiState.progressLabel}",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = question.category,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = question.text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }

    OutlinedTextField(
        value = uiState.currentAnswer,
        onValueChange = controller::updateCurrentAnswer,
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp),
        label = { Text("Your reflection") },
        supportingText = {
            Text("These drafts stay on-device until you share them.")
        }
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        TextButton(
            onClick = controller::goToPreviousQuestion,
            enabled = uiState.currentIndex > 0
        ) {
            Text("Previous")
        }
        Button(onClick = controller::goToNextQuestion) {
            Text(if (uiState.currentIndex >= uiState.questions.lastIndex) "Review" else "Next")
        }
    }
}

@Composable
private fun ErrorCard(message: String, onRetry: () -> Unit) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "We couldn't reach PocketBase",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
            Button(onClick = onRetry) {
                Text("Retry")
            }
        }
    }
}

@Composable
private fun InlineError(message: String, onRetry: () -> Unit) {
    AssistChip(
        onClick = onRetry,
        label = {
            Text(
                text = message,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            labelColor = MaterialTheme.colorScheme.onErrorContainer
        )
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AnswerHistory(
    uiState: ProustQuestionnaireUiState,
    onJump: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Answer timeline",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            uiState.answers.entries
                .sortedByDescending { it.value.updatedAt }
                .forEach { (questionId, draft) ->
                    val questionTitle = uiState.questions.firstOrNull { it.id == questionId }?.category ?: "Prompt"
                    SuggestionChip(
                        onClick = { onJump(questionId) },
                        label = {
                            Column {
                                Text(questionTitle, style = MaterialTheme.typography.labelSmall)
                                Text(
                                    text = if (draft.text.isBlank()) "Draft saved" else draft.text.take(32),
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    )
                }
        }
    }
    Spacer(modifier = Modifier.height(8.dp))
}
