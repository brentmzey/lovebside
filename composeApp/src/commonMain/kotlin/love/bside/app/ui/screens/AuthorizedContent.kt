package love.bside.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import love.bside.app.domain.models.AuthDetails
import love.bside.app.ui.screens.home.DashboardScreen
import love.bside.app.ui.screens.messaging.ChatScreen
import love.bside.app.ui.screens.messaging.ConversationListScreen
import love.bside.app.ui.screens.proust.QuestionnaireScreen

sealed class AuthorizedScreen {
    data object Checking : AuthorizedScreen()
    data object Onboarding : AuthorizedScreen()
    data object Dashboard : AuthorizedScreen()
    data object ConversationList : AuthorizedScreen()
    data object Proust : AuthorizedScreen()
    data class Chat(val conversationId: String, val name: String) : AuthorizedScreen()
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AuthorizedContent(
    authDetails: AuthDetails,
    onLogout: () -> Unit
) {
    var currentScreen by remember { mutableStateOf<AuthorizedScreen>(AuthorizedScreen.Checking) }
    
    // Inject Repository (using Koin)
    val messagingRepository = org.koin.compose.koinInject<love.bside.app.domain.repository.MessagingRepository>()
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        // Check if user has completed onboarding (Proust)
        try {
            val result = messagingRepository.getUserAnswers()
            if (result is love.bside.app.core.Result.Success && result.data.isEmpty()) {
                currentScreen = AuthorizedScreen.Onboarding
            } else {
                currentScreen = AuthorizedScreen.Dashboard
            }
        } catch (e: Exception) {
            // Fallback to Dashboard on error
            currentScreen = AuthorizedScreen.Dashboard
        }
    }

    AnimatedContent(targetState = currentScreen) { screen ->
        when (screen) {
            AuthorizedScreen.Checking -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    androidx.compose.material3.CircularProgressIndicator()
                }
            }
            AuthorizedScreen.Onboarding -> {
                QuestionnaireScreen(
                    onBack = { /* No back from onboarding */ },
                    isOnboarding = true,
                    onFinished = { currentScreen = AuthorizedScreen.Dashboard }
                )
            }
            AuthorizedScreen.Dashboard -> {
                val dashboardViewModel = org.koin.compose.koinInject<love.bside.app.ui.screens.home.DashboardViewModel>()
                val matches by dashboardViewModel.matches.collectAsState()
                
                DashboardScreen(
                    details = authDetails,
                    matches = matches,
                    onLogout = onLogout,
                    onOpenMessaging = { currentScreen = AuthorizedScreen.ConversationList },
                    onOpenProust = { currentScreen = AuthorizedScreen.Proust },
                    onOpenMatch = { match ->
                        // Start conversation with match
                        // For now just go to conversation list or create?
                        // Ideally: Create Direct Conversation -> Go to Chat.
                        // But create logic is in MessagingViewModel?
                        // Or we can pass callback to handle creation.
                        // Let's defer creation to DashboardViewModel? Or handle here.
                        // Simple: Navigate to Chat if existing?
                        // TODO: Implement "Start Chat with Match"
                    }
                )
            }
            AuthorizedScreen.Proust -> {
                QuestionnaireScreen(
                    onBack = { currentScreen = AuthorizedScreen.Dashboard },
                    isOnboarding = false
                )
            }
            AuthorizedScreen.ConversationList -> {
                ConversationListScreen(
                    onNavigateToChat = { conversationId ->
                         // Need conversation name? Fetch or pass?
                         // Ideally we just pass ID and ChatScreen fetches name.
                         // For now, passing "Chat" as placeholder name if needed, or changing ChatScreen signature.
                         // ChatScreen already updated to take ID.
                         currentScreen = AuthorizedScreen.Chat(conversationId, "Chat") 
                    },
                    onNavigateBack = { currentScreen = AuthorizedScreen.Dashboard }
                )
            }
            is AuthorizedScreen.Chat -> {
                ChatScreen(
                    conversationId = screen.conversationId,
                    onNavigateBack = { currentScreen = AuthorizedScreen.ConversationList }
                )
            }
        }
    }
}
