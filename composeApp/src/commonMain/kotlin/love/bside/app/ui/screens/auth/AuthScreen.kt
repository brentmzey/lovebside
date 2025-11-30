package love.bside.app.ui.screens.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import love.bside.app.core.AppException
import love.bside.app.core.Result
import love.bside.app.domain.models.AuthDetails
import love.bside.app.domain.models.SeekingStatus
import love.bside.app.domain.models.SignUpData
import love.bside.app.domain.usecase.LoginUseCase
import love.bside.app.domain.usecase.SignUpUseCase

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AuthScreen(
    modifier: Modifier = Modifier,
    loginUseCase: LoginUseCase,
    signUpUseCase: SignUpUseCase,
    onAuthenticated: (AuthDetails) -> Unit
) {
    val scope = rememberCoroutineScope()

    var uiState by remember { mutableStateOf(AuthUiState()) }

    val colorScheme = MaterialTheme.colorScheme
    val gradient = remember(colorScheme) {
        Brush.verticalGradient(
            0f to colorScheme.secondaryContainer,
            0.6f to colorScheme.primaryContainer,
            1f to colorScheme.primary.copy(alpha = 0.9f)
        )
    }

    fun handleResult(result: Result<AuthDetails>) {
        uiState = uiState.copy(isLoading = false)
        when (result) {
            is Result.Success -> onAuthenticated(result.data)
            is Result.Error -> uiState = uiState.copy(errorMessage = result.exception.message)
            Result.Loading -> {}
        }
    }

    fun submit() {
        if (!uiState.canSubmit || uiState.isLoading) return

        val birthDate = if (uiState.mode == AuthMode.SignUp) {
            runCatching { LocalDate.parse(uiState.birthDate.trim()) }
                .onFailure {
                    uiState = uiState.copy(errorMessage = "Use YYYY-MM-DD for birth date")
                }
                .getOrNull() ?: return
        } else null

        uiState = uiState.copy(isLoading = true, errorMessage = null)
        scope.launch {
            try {
                val result = when (uiState.mode) {
                    AuthMode.Login -> loginUseCase(uiState.email.trim(), uiState.password)
                    AuthMode.SignUp -> signUpUseCase(
                        SignUpData(
                            email = uiState.email.trim(),
                            password = uiState.password,
                            passwordConfirm = uiState.confirmPassword,
                            firstName = uiState.firstName.trim(),
                            lastName = uiState.lastName.trim(),
                            birthDate = birthDate!!,
                            seeking = uiState.seeking
                        )
                    )
                }
                handleResult(result)
            } catch (ce: CancellationException) {
                uiState = uiState.copy(isLoading = false)
                throw ce
            } catch (e: AppException) {
                uiState = uiState.copy(isLoading = false, errorMessage = e.message)
            } catch (e: Exception) {
                uiState = uiState.copy(isLoading = false, errorMessage = e.message ?: "Unknown error")
            }
        }
    }

    Surface(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                AuthHeader(uiState.mode)
                AuthModeSwitcher(
                    mode = uiState.mode,
                    onModeChange = { mode ->
                        if (mode != uiState.mode) {
                            uiState = AuthUiState(mode = mode)
                        }
                    }
                )

                AuthTextField(
                    label = "Email",
                    value = uiState.email,
                    onValueChange = { uiState = uiState.copy(email = it) },
                    keyboardType = KeyboardType.Email
                )
                AuthTextField(
                    label = "Password",
                    value = uiState.password,
                    onValueChange = { uiState = uiState.copy(password = it) },
                    keyboardType = KeyboardType.Password,
                    isPassword = true
                )

                AnimatedVisibility(uiState.mode == AuthMode.SignUp) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        AuthTextField(
                            label = "Confirm Password",
                            value = uiState.confirmPassword,
                            onValueChange = { uiState = uiState.copy(confirmPassword = it) },
                            keyboardType = KeyboardType.Password,
                            isPassword = true
                        )
                        AuthTextField(
                            label = "First Name",
                            value = uiState.firstName,
                            onValueChange = { uiState = uiState.copy(firstName = it) }
                        )
                        AuthTextField(
                            label = "Last Name",
                            value = uiState.lastName,
                            onValueChange = { uiState = uiState.copy(lastName = it) }
                        )
                        AuthTextField(
                            label = "Birth Date (YYYY-MM-DD)",
                            value = uiState.birthDate,
                            onValueChange = { uiState = uiState.copy(birthDate = it) }
                        )
                        Text(
                            text = "Seeking",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SeekingStatus.values().forEach { status ->
                                FilterChip(
                                    selected = uiState.seeking == status,
                                    label = { Text(status.name.lowercase().replaceFirstChar { it.titlecase() }) },
                                    onClick = { uiState = uiState.copy(seeking = status) },
                                    leadingIcon = if (uiState.seeking == status) {
                                        { Icon(Icons.Default.Check, contentDescription = null) }
                                    } else null,
                                    shape = RoundedCornerShape(50)
                                )
                            }
                        }
                    }
                }

                if (uiState.errorMessage != null) {
                    ErrorCard(message = uiState.errorMessage!!)
                }

                Button(
                    onClick = { submit() },
                    enabled = uiState.canSubmit && !uiState.isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(32.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    if (uiState.isLoading) {
                        Text("One moment…", style = MaterialTheme.typography.titleMedium)
                    } else {
                        Text(
                            text = if (uiState.mode == AuthMode.Login) "Sign In" else "Create Account",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }

                TextButton(
                    onClick = {
                        val nextMode = if (uiState.mode == AuthMode.Login) AuthMode.SignUp else AuthMode.Login
                        uiState = AuthUiState(mode = nextMode, email = uiState.email)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (uiState.mode == AuthMode.Login) "Need an account? Sign up" else "Have an account? Sign in",
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun AuthHeader(mode: AuthMode) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)),
        shape = RoundedCornerShape(32.dp),
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = if (mode == AuthMode.Login) "Welcome back" else "Create your B-Side",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = if (mode == AuthMode.Login)
                    "Pick up the conversation where you left off across Android, iOS, Desktop, and Web." else
                    "Craft a caring profile once and we’ll sync it everywhere, including your Windows and macOS builds.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AuthModeSwitcher(mode: AuthMode, onModeChange: (AuthMode) -> Unit) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AuthMode.values().forEach { entry ->
                val selected = entry == mode
                Surface(
                    color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                    shape = RoundedCornerShape(50),
                    tonalElevation = if (selected) 6.dp else 0.dp,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = if (entry == AuthMode.Login) "Sign in" else "Sign up",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                            .clickable { onModeChange(entry) },
                        textAlign = TextAlign.Center,
                        color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun AuthTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = true,
        visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
private fun ErrorCard(message: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.onErrorContainer,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(16.dp)
        )
    }
}
