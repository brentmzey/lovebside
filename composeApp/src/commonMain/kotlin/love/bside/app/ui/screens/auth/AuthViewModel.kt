package love.bside.app.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import love.bside.app.core.AppException
import love.bside.app.core.Result
import love.bside.app.domain.models.AuthDetails
import love.bside.app.domain.models.Profile
import love.bside.app.domain.models.SeekingStatus
import love.bside.app.domain.models.SignUpData
import love.bside.app.domain.usecase.GetDiscoveryUsersUseCase
import love.bside.app.domain.usecase.LoginUseCase
import love.bside.app.domain.usecase.SignUpUseCase

data class AuthUiState(
    val mode: AuthMode = AuthMode.Landing,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val birthDate: String = "",
    val seeking: SeekingStatus = SeekingStatus.BOTH,
    val discoveryProfiles: List<Profile> = emptyList()
) {
    val canSubmit: Boolean
        get() = when (mode) {
            AuthMode.Login -> email.isNotBlank() && password.length >= 6
            AuthMode.SignUp -> email.isNotBlank() &&
                    password.length >= 6 &&
                    password == confirmPassword &&
                    firstName.isNotBlank() &&
                    lastName.isNotBlank() &&
                    birthDate.length >= 10
            else -> false
        }
}

enum class AuthMode {
    Landing, Login, SignUp
}

class AuthViewModel(
    private val loginUseCase: LoginUseCase,
    private val signUpUseCase: SignUpUseCase,
    private val getDiscoveryUsersUseCase: GetDiscoveryUsersUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState = _uiState.asStateFlow()

    init {
        fetchDiscoveryProfiles()
    }

    private fun fetchDiscoveryProfiles() {
        viewModelScope.launch {
            val result = getDiscoveryUsersUseCase()
            if (result is Result.Success) {
                _uiState.update { it.copy(discoveryProfiles = result.data) }
            }
        }
    }

    fun onModeChange(mode: AuthMode) {
        _uiState.update { it.copy(mode = mode, errorMessage = null) }
    }

    fun onEmailChange(email: String) {
        _uiState.update { it.copy(email = email) }
    }

    fun onPasswordChange(password: String) {
        _uiState.update { it.copy(password = password) }
    }

    fun onConfirmPasswordChange(confirmPassword: String) {
        _uiState.update { it.copy(confirmPassword = confirmPassword) }
    }

    fun onFirstNameChange(firstName: String) {
        _uiState.update { it.copy(firstName = firstName) }
    }

    fun onLastNameChange(lastName: String) {
        _uiState.update { it.copy(lastName = lastName) }
    }

    fun onBirthDateChange(birthDate: String) {
        _uiState.update { it.copy(birthDate = birthDate) }
    }

    fun onSeekingChange(seeking: SeekingStatus) {
        _uiState.update { it.copy(seeking = seeking) }
    }

    fun submit(onSuccess: (AuthDetails) -> Unit) {
        val currentState = _uiState.value
        if (currentState.isLoading || !currentState.canSubmit) return

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                val result = when (currentState.mode) {
                    AuthMode.Login -> loginUseCase(currentState.email.trim(), currentState.password)
                    AuthMode.SignUp -> {
                        val birthDate = runCatching { LocalDate.parse(currentState.birthDate.trim()) }.getOrNull()
                        if (birthDate == null) {
                             throw AppException.Validation.InvalidInput("birthDate", "Invalid birth date format (YYYY-MM-DD)")
                        }
                        
                        signUpUseCase(
                            SignUpData(
                                email = currentState.email.trim(),
                                password = currentState.password,
                                passwordConfirm = currentState.confirmPassword,
                                firstName = currentState.firstName.trim(),
                                lastName = currentState.lastName.trim(),
                                birthDate = birthDate,
                                seeking = currentState.seeking
                            )
                        )
                    }
                    else -> throw IllegalStateException("Invalid mode")
                }

                _uiState.update { it.copy(isLoading = false) }
                
                if (result is Result.Success) {
                    onSuccess(result.data)
                } else if (result is Result.Error) {
                    _uiState.update { it.copy(errorMessage = result.exception.message) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message ?: "Unknown error") }
            }
        }
    }
}
