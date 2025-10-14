package love.bside.app.presentation.profile

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import love.bside.app.data.storage.SessionManager
import love.bside.app.domain.models.Profile
import love.bside.app.domain.usecase.GetUserProfileUseCase
import love.bside.app.domain.usecase.LogoutUseCase
import love.bside.app.ui.components.UiState
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

interface ProfileScreenComponent {
    val uiState: StateFlow<ProfileUiState>
    fun logout()
}

data class ProfileUiState(
    override val isLoading: Boolean = false,
    override val data: Profile? = null,
    override val error: String? = null
) : UiState<Profile>

class DefaultProfileScreenComponent(
    componentContext: ComponentContext,
    private val onLogout: () -> Unit,
    private val onNavigateToQuestionnaire: () -> Unit,
    private val onNavigateToValues: () -> Unit,
    private val onNavigateToMatches: () -> Unit
) : ProfileScreenComponent, ComponentContext by componentContext, KoinComponent {

    private val getUserProfileUseCase: GetUserProfileUseCase by inject()
    private val logoutUseCase: LogoutUseCase by inject()
    private val sessionManager: SessionManager by inject()

    private val _uiState = MutableStateFlow(ProfileUiState())
    override val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        val profile = sessionManager.getProfile()
        if (profile != null) {
            _uiState.value = ProfileUiState(data = profile)
        } else {
            // Handle error - user should be logged in to see this screen
        }
    }

    override fun logout() {
        coroutineScope().launch {
            logoutUseCase()
            onLogout()
        }
    }
}
