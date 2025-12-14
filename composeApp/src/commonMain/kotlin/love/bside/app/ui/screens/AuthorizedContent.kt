package love.bside.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.runtime.*
import love.bside.app.domain.models.AuthDetails
import love.bside.app.ui.screens.home.DashboardScreen
import love.bside.app.ui.screens.messaging.ChatScreen
import love.bside.app.ui.screens.messaging.ConversationListScreen

sealed class AuthorizedScreen {
    data object Dashboard : AuthorizedScreen()
    data object ConversationList : AuthorizedScreen()
    data class Chat(val conversationId: String, val name: String) : AuthorizedScreen()
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AuthorizedContent(
    authDetails: AuthDetails,
    onLogout: () -> Unit
) {
    var currentScreen by remember { mutableStateOf<AuthorizedScreen>(AuthorizedScreen.Dashboard) }

    AnimatedContent(targetState = currentScreen) { screen ->
        when (screen) {
            AuthorizedScreen.Dashboard -> {
                DashboardScreen(
                    details = authDetails,
                    onLogout = onLogout,
                    onOpenMessaging = { currentScreen = AuthorizedScreen.ConversationList }
                )
            }
            AuthorizedScreen.ConversationList -> {
                ConversationListScreen(
                    userId = authDetails.profile.id, // Assuming Profile has ID mapping to user ID
                    onConversationClick = { id, name ->
                        currentScreen = AuthorizedScreen.Chat(id, name)
                    }
                )
                // Need a way to go back to Dashboard? 
                // ConversationListScreen could have a BackHandler or TopBar.
                // For now, assuming system back or adding a back button.
            }
            is AuthorizedScreen.Chat -> {
                ChatScreen(
                    conversationId = screen.conversationId,
                    conversationName = screen.name,
                    userId = authDetails.profile.id,
                    onBack = { currentScreen = AuthorizedScreen.ConversationList }
                )
            }
        }
    }
}
