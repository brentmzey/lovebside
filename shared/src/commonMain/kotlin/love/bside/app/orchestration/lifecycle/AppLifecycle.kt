package love.bside.app.orchestration.lifecycle

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Application lifecycle manager
 */
class AppLifecycle {
    private val _state = MutableStateFlow<LifecycleState>(LifecycleState.Foreground)
    val state: StateFlow<LifecycleState> = _state.asStateFlow()

    fun onForeground() {
        _state.value = LifecycleState.Foreground
    }

    fun onBackground() {
        _state.value = LifecycleState.Background
    }

    fun onTerminate() {
        _state.value = LifecycleState.Terminated
    }
}

sealed class LifecycleState {
    data object Foreground : LifecycleState()
    data object Background : LifecycleState()
    data object Terminated : LifecycleState()
}
