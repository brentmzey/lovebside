package love.bside.app.ui.screens.auth

import love.bside.app.domain.models.SeekingStatus
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AuthUiStateTest {

    @Test
    fun `login mode requires valid email and password length`() {
        val emptyEmail = AuthUiState(mode = AuthMode.Login, email = "", password = "Secret1")
        assertFalse(emptyEmail.canSubmit)

        val shortPassword = AuthUiState(mode = AuthMode.Login, email = "user@example.com", password = "123")
        assertFalse(shortPassword.canSubmit)

        val valid = AuthUiState(mode = AuthMode.Login, email = "user@example.com", password = "Secret1")
        assertTrue(valid.canSubmit)
    }

    @Test
    fun `sign up mode requires additional profile fields`() {
        val base = AuthUiState(
            mode = AuthMode.SignUp,
            email = "user@example.com",
            password = "Password123",
            confirmPassword = "Password123",
            firstName = "Test",
            lastName = "User",
            birthDate = "1990-01-01",
            seeking = SeekingStatus.BOTH
        )
        assertTrue(base.canSubmit)

        assertFalse(base.copy(firstName = "").canSubmit)
        assertFalse(base.copy(lastName = "").canSubmit)
        assertFalse(base.copy(birthDate = "").canSubmit)
    }

    @Test
    fun `sign up mode blocks mismatched passwords`() {
        val state = AuthUiState(
            mode = AuthMode.SignUp,
            email = "user@example.com",
            password = "Password123",
            confirmPassword = "Different",
            firstName = "Test",
            lastName = "User",
            birthDate = "1990-01-01"
        )

        assertFalse(state.canSubmit)
        assertTrue(state.copy(confirmPassword = "Password123").canSubmit)
    }
}
