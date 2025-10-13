package love.bside.app.presentation.main

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.slide
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import love.bside.app.presentation.matches.MatchesScreen
import love.bside.app.presentation.profile.ProfileScreen
import love.bside.app.presentation.prompts.PromptsScreen
import love.bside.app.presentation.questionnaire.QuestionnaireScreen
import love.bside.app.presentation.values.ValuesScreen

@Composable
fun MainScreen(component: MainScreenComponent) {
    val childStack by component.childStack.subscribeAsState()
    
    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.AccountCircle, contentDescription = "Profile") },
                    label = { Text("Profile") },
                    selected = childStack.active.instance is MainScreenComponent.Child.Profile,
                    onClick = { component.onProfileTabClicked() }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Favorite, contentDescription = "Matches") },
                    label = { Text("Matches") },
                    selected = childStack.active.instance is MainScreenComponent.Child.Matches,
                    onClick = { component.onMatchesTabClicked() }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Questionnaire") },
                    label = { Text("Questionnaire") },
                    selected = childStack.active.instance is MainScreenComponent.Child.Questionnaire,
                    onClick = { component.onQuestionnaireTabClicked() }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Values") },
                    label = { Text("Values") },
                    selected = childStack.active.instance is MainScreenComponent.Child.Values,
                    onClick = { component.onValuesTabClicked() }
                )
            }
        }
    ) { paddingValues ->
        Children(
            stack = component.childStack,
            animation = stackAnimation(slide()),
            modifier = Modifier.padding(paddingValues)
        ) {
            when (val child = it.instance) {
                is MainScreenComponent.Child.Profile -> ProfileScreen(child.component)
                is MainScreenComponent.Child.Questionnaire -> QuestionnaireScreen(child.component)
                is MainScreenComponent.Child.Values -> ValuesScreen(child.component)
                is MainScreenComponent.Child.Matches -> MatchesScreen(child.component)
                is MainScreenComponent.Child.Prompts -> PromptsScreen(child.component)
            }
        }
    }
}
