package love.bside.app.presentation

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import love.bside.app.core.AppException
import love.bside.app.core.logError
import love.bside.app.domain.models.AuthDetails
import love.bside.app.domain.usecase.LoginUseCase
import love.bside.app.domain.usecase.SignUpUseCase

sealed class AuthViewModelState {
    data object Idle : AuthViewModelState()
    data object Loading : AuthViewModelState()
    data class Success(val authDetails: AuthDetails) : AuthViewModelState()
    data class Error(val exception: AppException) : AuthViewModelState()
}

class AuthViewModel(
    private val loginUseCase: LoginUseCase,
    private val signUpUseCase: SignUpUseCase
) {
    private val viewModelScope = CoroutineScope(Dispatchers.Main)

    private val _uiState = MutableStateFlow<AuthViewModelState>(AuthViewModelState.Idle)
    val uiState: StateFlow<AuthViewModelState> = _uiState.asStateFlow()

    fun login(email: String, password: String) {
        _uiState.value = AuthViewModelState.Loading
        viewModelScope.launch {
            loginUseCase(email, password)
                .onSuccess { authDetails ->
                    _uiState.value = AuthViewModelState.Success(authDetails)
                }
                .onError { exception ->
                    logError("Login failed", exception)
                    _uiState.value = AuthViewModelState.Error(exception)
                }
        }
    }

    fun signUp(email: String, password: String) {
        _uiState.value = AuthViewModelState.Loading
        viewModelScope.launch {
            signUpUseCase(email, password, password)
                .onSuccess { authDetails ->
                    _uiState.value = AuthViewModelState.Success(authDetails)
                }
                .onError { exception ->
                    logError("Sign up failed", exception)
                    _uiState.value = AuthViewModelState.Error(exception)
                }
        }
    }

    fun clearError() {
        if (_uiState.value is AuthViewModelState.Error) {
            _uiState.value = AuthViewModelState.Idle
        }
    }
}
