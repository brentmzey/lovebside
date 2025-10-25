package love.bside.app.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import love.bside.app.data.mock.MockData
import love.bside.app.data.models.Profile
import love.bside.app.data.models.messaging.Conversation
import love.bside.app.ui.components.BsideBottomNavBar
import love.bside.app.ui.components.MainTab
import love.bside.app.ui.screens.discover.UserGridScreen
import love.bside.app.ui.screens.messages.ConversationsListScreen
import love.bside.app.ui.screens.profile.ProfileScreen as ProfileViewScreen
import love.bside.app.ui.screens.settings.SettingsScreen

@Composable
fun MainContent(
    currentUserId: String = "user_001",
    currentProfile: Profile? = MockData.currentUser,
    conversations: List<Conversation> = MockData.conversations,
    matchProfiles: List<Profile> = MockData.discoverProfiles,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(MainTab.MESSAGES) }
    var isDarkTheme by remember { mutableStateOf(false) }
    
    Scaffold(
        modifier = modifier,
        bottomBar = {
            BsideBottomNavBar(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )
        }
    ) { paddingValues ->
        when (selectedTab) {
            MainTab.MESSAGES -> {
                ConversationsListScreen(
                    conversations = conversations,
                    currentUserId = currentUserId,
                    onConversationClick = { conversation ->
                        // TODO: Navigate to chat detail screen
                        println("Clicked conversation: ${conversation.id}")
                    },
                    isLoading = false,
                    modifier = Modifier.padding(paddingValues)
                )
            }
            
            MainTab.DISCOVER -> {
                UserGridScreen(
                    profiles = matchProfiles,
                    onProfileClick = { profile ->
                        // TODO: Navigate to user profile detail
                        println("Clicked profile: ${profile.userId}")
                    },
                    isLoading = false,
                    modifier = Modifier.padding(paddingValues)
                )
            }
            
            MainTab.PROFILE -> {
                ProfileViewScreen(
                    profile = currentProfile,
                    onEditClick = {
                        // TODO: Navigate to edit profile
                        println("Edit profile clicked")
                    },
                    isLoading = false,
                    modifier = Modifier.padding(paddingValues)
                )
            }
            
            MainTab.SETTINGS -> {
                SettingsScreen(
                    isDarkTheme = isDarkTheme,
                    onThemeToggle = { isDarkTheme = it },
                    onLogout = onLogout,
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }
}
