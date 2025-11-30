package love.bside.app.ui.screens.auth

import love.bside.app.domain.models.SeekingStatus

enum class AuthMode { Login, SignUp }

data class AuthUiState(
    val mode: AuthMode = AuthMode.Login,
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val birthDate: String = "",
    val seeking: SeekingStatus = SeekingStatus.BOTH,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
) {
    val canSubmit: Boolean
        get() = email.isNotBlank() && password.length >= 6 && when (mode) {
            AuthMode.Login -> true
            AuthMode.SignUp -> firstName.isNotBlank() && lastName.isNotBlank() &&
                confirmPassword == password && birthDate.isNotBlank()
        }
}
