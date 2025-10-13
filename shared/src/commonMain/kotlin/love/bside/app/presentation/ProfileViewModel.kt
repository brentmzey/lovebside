package love.bside.app.presentation

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import love.bside.app.core.AppException
import love.bside.app.core.logError
import love.bside.app.core.logInfo
import love.bside.app.domain.models.Profile
import love.bside.app.domain.usecase.GetUserProfileUseCase
import love.bside.app.domain.usecase.LogoutUseCase

sealed class ProfileViewModelState {
    data object Idle : ProfileViewModelState()
    data object Loading : ProfileViewModelState()
    data class Success(val profile: Profile) : ProfileViewModelState()
    data class Error(val exception: AppException) : ProfileViewModelState()
}

class ProfileViewModel(
    private val getUserProfileUseCase: GetUserProfileUseCase,
    private val logoutUseCase: LogoutUseCase
) {
    private val viewModelScope = CoroutineScope(Dispatchers.Main)

    private val _uiState = MutableStateFlow<ProfileViewModelState>(ProfileViewModelState.Idle)
    val uiState: StateFlow<ProfileViewModelState> = _uiState.asStateFlow()

    fun fetchProfile(userId: String) {
        _uiState.value = ProfileViewModelState.Loading
        viewModelScope.launch {
            getUserProfileUseCase(userId)
                .onSuccess { profile ->
                    _uiState.value = ProfileViewModelState.Success(profile)
                }
                .onError { exception ->
                    logError("Failed to fetch profile", exception)
                    _uiState.value = ProfileViewModelState.Error(exception)
                }
        }
    }

    fun logout() {
        viewModelScope.launch {
            logInfo("User logging out")
            logoutUseCase()
        }
    }

    fun clearError() {
        if (_uiState.value is ProfileViewModelState.Error) {
            _uiState.value = ProfileViewModelState.Idle
        }
    }
}
