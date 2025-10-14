package love.bside.app.presentation.login

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import love.bside.app.core.AppException
import love.bside.app.core.logError
import love.bside.app.domain.models.AuthDetails
import love.bside.app.domain.usecase.LoginUseCase
import love.bside.app.domain.usecase.SignUpUseCase
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

interface LoginScreenComponent {
    val uiState: StateFlow<AuthUiState>
    fun login(email: String, password: String)
    fun signUp(email: String, password: String)
    fun clearError()
}

sealed class AuthUiState {
    data object Idle : AuthUiState()
    data object Loading : AuthUiState()
    data class Success(val authDetails: AuthDetails) : AuthUiState()
    data class Error(val exception: AppException) : AuthUiState()
}

class DefaultLoginScreenComponent(
    componentContext: ComponentContext,
    private val onLoginSuccess: () -> Unit
) : LoginScreenComponent, ComponentContext by componentContext, KoinComponent {

    private val loginUseCase: LoginUseCase by inject()
    private val signUpUseCase: SignUpUseCase by inject()

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    override val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        coroutineScope().launch {
            uiState.collect { state ->
                if (state is AuthUiState.Success) {
                    onLoginSuccess()
                }
            }
        }
    }

    override fun login(email: String, password: String) {
        _uiState.value = AuthUiState.Loading
        coroutineScope().launch {
            loginUseCase(email, password)
                .onSuccess { authDetails ->
                    _uiState.value = AuthUiState.Success(authDetails)
                }
                .onError { exception ->
                    logError("Login failed", exception)
                    _uiState.value = AuthUiState.Error(exception)
                }
        }
    }

    override fun signUp(email: String, password: String) {
        _uiState.value = AuthUiState.Loading
        coroutineScope().launch {
            signUpUseCase(email, password, password)
                .onSuccess { authDetails ->
                    _uiState.value = AuthUiState.Success(authDetails)
                }
                .onError { exception ->
                    logError("Sign up failed", exception)
                    _uiState.value = AuthUiState.Error(exception)
                }
        }
    }

    override fun clearError() {
        if (_uiState.value is AuthUiState.Error) {
            _uiState.value = AuthUiState.Idle
        }
    }
}
