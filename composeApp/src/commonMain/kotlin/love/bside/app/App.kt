package love.bside.app

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import love.bside.app.domain.models.AuthDetails
import love.bside.app.domain.usecase.LoginUseCase
import love.bside.app.domain.usecase.LogoutUseCase
import love.bside.app.domain.usecase.SignUpUseCase
import love.bside.app.routing.RootComponent
import love.bside.app.ui.screens.auth.AuthScreen
import love.bside.app.ui.screens.home.DashboardScreen
import love.bside.app.ui.theme.BsideTheme

data class AppDependencies(
    val loginUseCase: LoginUseCase,
    val signUpUseCase: SignUpUseCase,
    val logoutUseCase: LogoutUseCase
)

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun App(
    root: RootComponent,
    dependencies: AppDependencies
) {
    BsideTheme {
        val logoutUseCase = dependencies.logoutUseCase
        val scope = rememberCoroutineScope()
        var authDetails by remember { mutableStateOf<AuthDetails?>(null) }
        var logoutError by remember { mutableStateOf<String?>(null) }

        Surface(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxSize()) {
                AnimatedContent(targetState = authDetails) { current ->
                    if (current == null) {
                        AuthScreen(
                            loginUseCase = dependencies.loginUseCase,
                            signUpUseCase = dependencies.signUpUseCase,
                            onAuthenticated = { details ->
                                logoutError = null
                                authDetails = details
                            }
                        )
                    } else {
                        DashboardScreen(
                            details = current,
                            onLogout = {
                                scope.launch {
                                    runCatching { logoutUseCase() }
                                        .onSuccess {
                                            authDetails = null
                                            logoutError = null
                                        }
                                        .onFailure { error ->
                                            logoutError = error.message ?: "Unable to log out"
                                        }
                                }
                            }
                        )
                    }
                }

                logoutError?.let { message ->
                    androidx.compose.material3.Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(16.dp),
                        tonalElevation = 4.dp,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth(0.9f)
                            .padding(bottom = 24.dp)
                    ) {
                        Text(
                            text = message,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                        )
                    }
                }
            }
        }
    }
}
