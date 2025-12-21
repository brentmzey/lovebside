package love.bside.app.ui.screens.proust

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import love.bside.app.domain.models.ProustQuestionnaire
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestionnaireScreen(
    onBack: () -> Unit,
    isOnboarding: Boolean = false,
    onFinished: () -> Unit = {},
    viewModel: QuestionnaireViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isOnboarding) "Welcome to B-Side" else "Proust Questionnaire") },
                navigationIcon = {
                    if (!isOnboarding) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    if (isOnboarding) {
                         Text(
                             text = "Answer a few questions to help us find your match.",
                             style = MaterialTheme.typography.bodyMedium,
                             modifier = Modifier.padding(16.dp),
                             color = MaterialTheme.colorScheme.onSurfaceVariant
                         )
                    }
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(state.questions) { question ->
                            QuestionItem(
                                question = question,
                                answer = state.answers[question.id] ?: "",
                                onAnswerChange = { viewModel.onAnswerChanged(question.id, it) },
                                onSave = { viewModel.saveAnswer(question.id, state.answers[question.id] ?: "") }
                            )
                        }
                        
                        item {
                            Spacer(modifier = Modifier.height(32.dp))
                            Button(
                                onClick = if (isOnboarding) onFinished else onBack,
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp)
                            ) {
                                Text(if (isOnboarding) "Finish Setup" else "Done")
                            }
                        }
                    }
                }
            }
            
            state.error?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)
                )
            }
        }
    }
}

@Composable
fun QuestionItem(
    question: ProustQuestionnaire,
    answer: String,
    onAnswerChange: (String) -> Unit,
    onSave: () -> Unit
) {
    Card {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = question.questionText,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = answer,
                onValueChange = onAnswerChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Your answer...") },
                trailingIcon = {
                    IconButton(onClick = onSave) {
                        Icon(Icons.Default.Save, contentDescription = "Save Answer")
                    }
                }
            )
        }
    }
}
