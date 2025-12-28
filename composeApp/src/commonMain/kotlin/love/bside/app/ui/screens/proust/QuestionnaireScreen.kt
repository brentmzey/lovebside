package love.bside.app.ui.screens.proust

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import love.bside.app.domain.models.ProustQuestionnaire
import love.bside.app.ui.theme.BsideBrand
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
    var currentQuestionIndex by remember { mutableStateOf(0) }
    
    // Premium Background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.2f),
                        BsideBrand.TealTileLight.copy(alpha=0.1f)
                    )
                )
            )
            .statusBarsPadding()
    ) {
        if (state.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = BsideBrand.TealTile
            )
        } else if (state.questions.isNotEmpty()) {
            val totalQuestions = state.questions.size
            val currentQuestion = state.questions.getOrNull(currentQuestionIndex)
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .imePadding()
            ) {
                // Header / Navigator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (currentQuestionIndex > 0) {
                        IconButton(onClick = { currentQuestionIndex-- }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous")
                        }
                    } else if (!isOnboarding) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    } else {
                        Spacer(Modifier.size(48.dp))
                    }

                    Text(
                        text = "${currentQuestionIndex + 1} of $totalQuestions",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (currentQuestionIndex < totalQuestions - 1) {
                         TextButton(onClick = { 
                             // Save current and move next
                             val q = state.questions[currentQuestionIndex]
                             viewModel.saveAnswer(q.id, state.answers[q.id] ?: "")
                             currentQuestionIndex++ 
                         }) {
                             Text("Skip", color = MaterialTheme.colorScheme.outline)
                         }
                    } else {
                         Spacer(Modifier.size(48.dp))
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { (currentQuestionIndex + 1) / totalQuestions.toFloat() },
                    modifier = Modifier.fillMaxWidth().height(4.dp).background(Color.Transparent, RoundedCornerShape(2.dp)),
                    color = BsideBrand.TealTile,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                
                Spacer(modifier = Modifier.height(40.dp))

                // Question Card (Animated)
                currentQuestion?.let { question ->
                    AnimatedContent(
                        targetState = question,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(300)) + slideInHorizontally { width -> width } togetherWith
                            fadeOut(animationSpec = tween(300)) + slideOutHorizontally { width -> -width }
                        }
                    ) { targetQuestion ->
                         QuestionCard(
                             question = targetQuestion,
                             answer = state.answers[targetQuestion.id] ?: "",
                             onAnswerChange = { viewModel.onAnswerChanged(targetQuestion.id, it) }
                         )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))
                
                // Bottom Button
                val isLast = currentQuestionIndex == totalQuestions - 1
                
                Button(
                    onClick = {
                        // Save Answer
                        currentQuestion?.let { q ->
                             viewModel.saveAnswer(q.id, state.answers[q.id] ?: "")
                        }
                        
                        if (isLast) {
                            onFinished()
                        } else {
                            currentQuestionIndex++
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BsideBrand.TealTile,
                        contentColor = BsideBrand.PlumHeartDark
                    )
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (isLast) "Finish Profile" else "Next Question",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (!isLast) {
                            Spacer(Modifier.width(8.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                        } else {
                            Spacer(Modifier.width(8.dp))
                            Icon(Icons.Default.Check, contentDescription = null)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
fun QuestionCard(
    question: ProustQuestionnaire,
    answer: String,
    onAnswerChange: (String) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.verticalScroll(rememberScrollState())
    ) {
        Text(
            text = question.questionText,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        OutlinedTextField(
            value = answer,
            onValueChange = onAnswerChange,
            modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
            placeholder = { 
                Text(
                    "Share your thoughts...",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=0.5f)
                ) 
            },
            textStyle = MaterialTheme.typography.bodyLarge,
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = BsideBrand.TealTile,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha=0.5f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha=0.5f)
            ),
            maxLines = 10
        )
    }
}
