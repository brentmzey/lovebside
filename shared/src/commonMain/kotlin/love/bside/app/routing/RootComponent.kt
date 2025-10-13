package love.bside.app.routing

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.*
import com.arkivanov.decompose.value.Value
import kotlinx.serialization.Serializable
import love.bside.app.data.storage.TokenStorage
import love.bside.app.presentation.login.DefaultLoginScreenComponent
import love.bside.app.presentation.login.LoginScreenComponent
import love.bside.app.presentation.main.DefaultMainScreenComponent
import love.bside.app.presentation.main.MainScreenComponent

class RootComponent(
    componentContext: ComponentContext,
    private val tokenStorage: TokenStorage
) : ComponentContext by componentContext {

    private val navigation = StackNavigation<Configuration>()

    val childStack: Value<ChildStack<*, Child>> = childStack(
        source = navigation,
        serializer = Configuration.serializer(),
        initialConfiguration = if (tokenStorage.hasToken()) Configuration.Main else Configuration.Login,
        handleBackButton = true,
        childFactory = ::createChild
    )

    private fun createChild(config: Configuration, context: ComponentContext): Child {
        return when (config) {
            is Configuration.Login -> Child.Login(
                DefaultLoginScreenComponent(
                    componentContext = context,
                    onLoginSuccess = { navigation.replaceAll(Configuration.Main) }
                )
            )
            is Configuration.Main -> Child.Main(
                DefaultMainScreenComponent(
                    componentContext = context,
                    onLogout = {
                        tokenStorage.clearToken()
                        navigation.replaceAll(Configuration.Login)
                    }
                )
            )
        }
    }

    sealed class Child {
        data class Login(val component: LoginScreenComponent) : Child()
        data class Main(val component: MainScreenComponent) : Child()
    }

    @Serializable
    sealed class Configuration {
        @Serializable
        data object Login : Configuration()
        @Serializable
        data object Main : Configuration()
    }
}
