package love.bside.app.presentation.login

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import love.bside.app.core.validation.Validators
import love.bside.app.ui.components.*

@Composable
fun LoginScreen(component: LoginScreenComponent) {
    val uiState by component.uiState.collectAsState()

    when (val state = uiState) {
        is AuthUiState.Loading -> {
            LoadingView(message = "Authenticating...")
        }
        is AuthUiState.Error -> {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                ErrorView(
                    exception = state.exception,
                    onRetry = { component.clearError() },
                    modifier = Modifier.padding(16.dp)
                )
                LoginForm(component)
            }
        }
        is AuthUiState.Idle, is AuthUiState.Success -> {
            LoginForm(component)
        }
    }
}

@Composable
private fun LoginForm(component: LoginScreenComponent) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var isSignUpMode by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (isSignUpMode) "Create Account" else "Welcome Back",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        ValidatedTextField(
            value = email,
            onValueChange = { 
                email = it
                emailError = Validators.validateEmail(it).getErrorOrNull()?.getUserMessage()
            },
            label = "Email",
            errorMessage = emailError,
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Next,
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        PasswordTextField(
            value = password,
            onValueChange = { 
                password = it
                if (isSignUpMode) {
                    passwordError = Validators.validatePassword(it).getErrorOrNull()?.getUserMessage()
                }
            },
            label = "Password",
            errorMessage = passwordError,
            imeAction = ImeAction.Done,
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                    if (isSignUpMode) {
                        component.signUp(email, password)
                    } else {
                        component.login(email, password)
                    }
                }
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        LoadingButton(
            onClick = {
                if (isSignUpMode) {
                    component.signUp(email, password)
                } else {
                    component.login(email, password)
                }
            },
            text = if (isSignUpMode) "Sign Up" else "Login",
            enabled = email.isNotBlank() && password.isNotBlank() &&
                    emailError == null && (passwordError == null || !isSignUpMode),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(
            onClick = { 
                isSignUpMode = !isSignUpMode
                passwordError = null
            }
        ) {
            Text(
                if (isSignUpMode) 
                    "Already have an account? Login" 
                else 
                    "Don't have an account? Sign Up"
            )
        }
    }
}
