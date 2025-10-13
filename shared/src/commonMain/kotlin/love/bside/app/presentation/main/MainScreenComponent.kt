package love.bside.app.presentation.main

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.*
import com.arkivanov.decompose.value.Value
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable
import love.bside.app.presentation.matches.DefaultMatchScreenComponent
import love.bside.app.presentation.matches.MatchScreenComponent
import love.bside.app.presentation.profile.DefaultProfileScreenComponent
import love.bside.app.presentation.profile.ProfileScreenComponent
import love.bside.app.presentation.prompts.DefaultPromptScreenComponent
import love.bside.app.presentation.prompts.PromptScreenComponent
import love.bside.app.presentation.questionnaire.DefaultQuestionnaireScreenComponent
import love.bside.app.presentation.questionnaire.QuestionnaireScreenComponent
import love.bside.app.presentation.values.DefaultValuesScreenComponent
import love.bside.app.presentation.values.ValuesScreenComponent

interface MainScreenComponent {
    val childStack: Value<ChildStack<*, Child>>
    fun onProfileTabClicked()
    fun onMatchesTabClicked()
    fun onQuestionnaireTabClicked()
    fun onValuesTabClicked()
    
    sealed class Child {
        data class Profile(val component: ProfileScreenComponent) : Child()
        data class Questionnaire(val component: QuestionnaireScreenComponent) : Child()
        data class Values(val component: ValuesScreenComponent) : Child()
        data class Matches(val component: MatchScreenComponent) : Child()
        data class Prompts(val component: PromptScreenComponent) : Child()
    }
}

class DefaultMainScreenComponent(
    componentContext: ComponentContext,
    private val onLogout: () -> Unit
) : MainScreenComponent, ComponentContext by componentContext {

    private val navigation = StackNavigation<Configuration>()

    override val childStack = childStack(
        source = navigation,
        serializer = Configuration.serializer(),
        initialConfiguration = Configuration.Profile,
        handleBackButton = true,
        childFactory = ::createChild
    )

    private fun createChild(config: Configuration, context: ComponentContext): MainScreenComponent.Child {
        return when (config) {
            is Configuration.Profile -> MainScreenComponent.Child.Profile(profileComponent(context))
            is Configuration.Questionnaire -> MainScreenComponent.Child.Questionnaire(questionnaireComponent(context))
            is Configuration.Values -> MainScreenComponent.Child.Values(valuesComponent(context))
            is Configuration.Matches -> MainScreenComponent.Child.Matches(matchesComponent(context))
            is Configuration.Prompts -> MainScreenComponent.Child.Prompts(promptComponent(context, config.matchId))
        }
    }

    private fun profileComponent(context: ComponentContext): ProfileScreenComponent = 
        DefaultProfileScreenComponent(
            componentContext = context,
            onLogout = onLogout,
            onNavigateToQuestionnaire = { navigation.bringToFront(Configuration.Questionnaire) },
            onNavigateToValues = { navigation.bringToFront(Configuration.Values) },
            onNavigateToMatches = { navigation.bringToFront(Configuration.Matches) }
        )

    private fun questionnaireComponent(context: ComponentContext): QuestionnaireScreenComponent = 
        DefaultQuestionnaireScreenComponent(
            componentContext = context
        )

    private fun valuesComponent(context: ComponentContext): ValuesScreenComponent = 
        DefaultValuesScreenComponent(
            componentContext = context,
            onBack = { navigation.pop() }
        )

    private fun matchesComponent(context: ComponentContext): MatchScreenComponent =
        DefaultMatchScreenComponent(
            componentContext = context,
            onMatchClicked = { matchId -> navigation.push(Configuration.Prompts(matchId)) }
        )

    private fun promptComponent(context: ComponentContext, matchId: String): PromptScreenComponent =
        DefaultPromptScreenComponent(
            componentContext = context,
            matchId = matchId
        )

    override fun onProfileTabClicked() {
        navigation.bringToFront(Configuration.Profile)
    }

    override fun onMatchesTabClicked() {
        navigation.bringToFront(Configuration.Matches)
    }

    override fun onQuestionnaireTabClicked() {
        navigation.bringToFront(Configuration.Questionnaire)
    }

    override fun onValuesTabClicked() {
        navigation.bringToFront(Configuration.Values)
    }

    @Serializable
    sealed class Configuration {
        @Serializable
        data object Profile : Configuration()
        @Serializable
        data object Questionnaire : Configuration()
        @Serializable
        data object Values : Configuration()
        @Serializable
        data object Matches : Configuration()
        @Serializable
        data class Prompts(val matchId: String) : Configuration()
    }
}
