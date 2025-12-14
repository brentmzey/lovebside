package love.bside.app.ui.screens.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
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
import love.bside.app.security.BiometricAvailability
import love.bside.app.security.SecureAuthFactor
import love.bside.app.security.SecureAuthResult
import love.bside.app.security.SecurePromptText
import love.bside.app.security.usecase.BiometricLoginUseCase
import love.bside.app.security.usecase.EnableBiometricLoginUseCase
import love.bside.app.security.usecase.ObserveSecureEnrollmentsUseCase

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AuthScreen(
    modifier: Modifier = Modifier,
    loginUseCase: LoginUseCase,
    signUpUseCase: SignUpUseCase,
    biometricLoginUseCase: BiometricLoginUseCase,
    enableBiometricLoginUseCase: EnableBiometricLoginUseCase,
    observeSecureEnrollmentsUseCase: ObserveSecureEnrollmentsUseCase,
    onAuthenticated: (AuthDetails) -> Unit
) {
    val scope = rememberCoroutineScope()

    var uiState by remember { mutableStateOf(AuthUiState()) }
    val enrollments by observeSecureEnrollmentsUseCase().collectAsState(initial = emptyList())
    val hasBiometricEnrollment = remember(enrollments) { enrollments.any { it.factor is SecureAuthFactor.Biometric } }
    var biometricAvailability by remember { mutableStateOf<BiometricAvailability>(BiometricAvailability.Unknown) }
    var wantsBiometric by remember { mutableStateOf(false) }
    var biometricError by remember { mutableStateOf<String?>(null) }
    var isBiometricLoading by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        biometricAvailability = biometricLoginUseCase.availability()
    }

    fun describeAvailability(availability: BiometricAvailability): String = when (availability) {
        BiometricAvailability.Available -> "Ready to use"
        BiometricAvailability.NoHardware -> "This device lacks biometric hardware"
        BiometricAvailability.NotEnrolled -> "Enroll Face ID or Touch ID in system settings"
        is BiometricAvailability.Unavailable -> availability.reason
        BiometricAvailability.Unknown -> "Biometric status unknown"
    }

    val colorScheme = MaterialTheme.colorScheme
    val gradient = remember(colorScheme) {
        Brush.verticalGradient(
            0f to colorScheme.secondaryContainer,
            0.6f to colorScheme.primaryContainer,
            1f to colorScheme.primary.copy(alpha = 0.9f)
        )
    }
    val scrollState = rememberScrollState()

    fun handleResult(result: Result<AuthDetails>) {
        uiState = uiState.copy(isLoading = false)
        when (result) {
            is Result.Success -> {
                if (uiState.mode == AuthMode.Login && wantsBiometric &&
                    biometricAvailability is BiometricAvailability.Available
                ) {
                    scope.launch {
                        runCatching {
                            enableBiometricLoginUseCase(result.data, uiState.email.trim())
                        }
                        wantsBiometric = false
                    }
                }
                onAuthenticated(result.data)
            }
            is Result.Error -> uiState = uiState.copy(errorMessage = result.exception.message)
            Result.Loading -> {}
        }
    }

    fun attemptBiometricLogin() {
        if (!hasBiometricEnrollment) return
        scope.launch {
            isBiometricLoading = true
            val result = biometricLoginUseCase(SecurePromptText.default())
            isBiometricLoading = false
            when (result) {
                is SecureAuthResult.Success -> onAuthenticated(result.details)
                SecureAuthResult.Canceled -> Unit
                SecureAuthResult.NoEnrollment -> biometricError = "No biometric enrollment found"
                is SecureAuthResult.Error -> biometricError = result.message
                is SecureAuthResult.Unavailable -> biometricError = describeAvailability(result.availability)
            }
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
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient)
        ) {
            val responsiveWidth = remember(maxWidth) {
                val goldenRatio = 1.618f
                if (maxWidth <= 640.dp) {
                    maxWidth
                } else {
                    (maxWidth / goldenRatio).coerceAtMost(960.dp)
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .widthIn(max = responsiveWidth)
                    .fillMaxWidth()
                    .systemBarsPadding()
                    .imePadding()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 24.dp, vertical = 28.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                if (hasBiometricEnrollment && biometricAvailability is BiometricAvailability.Available) {
                    BiometricQuickLoginCard(
                        isLoading = isBiometricLoading,
                        availabilityLabel = describeAvailability(biometricAvailability),
                        onClick = {
                            biometricError = null
                            attemptBiometricLogin()
                        }
                    )
                }

                AuthHeader(uiState.mode)

                AuthModeSwitcher(
                    mode = uiState.mode,
                    onModeChange = { mode ->
                        if (mode != uiState.mode) {
                            uiState = AuthUiState(mode = mode)
                        }
                    }
                )

                AuthFormCard {
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
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
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
                }

                if (uiState.errorMessage != null) {
                    ErrorCard(message = uiState.errorMessage!!)
                }

                if (biometricError != null) {
                    ErrorCard(message = biometricError!!)
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

                if (!hasBiometricEnrollment && biometricAvailability is BiometricAvailability.Available && uiState.mode == AuthMode.Login) {
                    BiometricOptInRow(
                        checked = wantsBiometric,
                        onCheckedChange = { wantsBiometric = it },
                        availabilityLabel = describeAvailability(biometricAvailability)
                    )
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

                AuthFeatureShowcase(
                    mode = uiState.mode,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun AuthHeader(mode: AuthMode) {
    val colorScheme = MaterialTheme.colorScheme
    val heroTitle = if (mode == AuthMode.Login) "Welcome back to your softer side" else "Design your B-Side story"
    val heroSubtitle = if (mode == AuthMode.Login) {
        "We hydrate your latest data from PocketBase and flow it into every screen before you even tap Sign In."
    } else {
        "One setup fuels Android, iOS, Desktop, and Web. Link people, places, and feelings once—own them everywhere."
    }
    val highlightChips = if (mode == AuthMode.Login) {
        listOf("Secure refresh tokens", "Continuity across devices", "Graph-backed matches")
    } else {
        listOf("Emotion graph ready", "PocketBase ownership", "Adaptive spacing on iOS")
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(36.dp),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surface.copy(alpha = 0.92f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            colorScheme.primary.copy(alpha = 0.12f),
                            colorScheme.secondaryContainer.copy(alpha = 0.3f),
                            colorScheme.primaryContainer.copy(alpha = 0.5f)
                        )
                    )
                )
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                BrandGlyph()
                Text(
                    text = heroTitle,
                    style = MaterialTheme.typography.headlineSmall,
                    color = colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = heroSubtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = colorScheme.onSurfaceVariant
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    highlightChips.forEach { highlight ->
                        AuthFeatureChip(text = highlight, tonalElevation = 0.dp)
                    }
                }
            }
        }
    }
}

@Composable
private fun AuthFormCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(36.dp),
        tonalElevation = 12.dp,
        shadowElevation = 4.dp,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun BrandGlyph(modifier: Modifier = Modifier) {
    val colorScheme = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        colorScheme.primary,
                        colorScheme.secondary,
                        colorScheme.primaryContainer
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "B",
            style = MaterialTheme.typography.headlineMedium,
            color = colorScheme.onPrimary,
            fontWeight = FontWeight.Black
        )
    }
}

@Composable
private fun BiometricQuickLoginCard(
    isLoading: Boolean,
    availabilityLabel: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Quick unlock",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = availabilityLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = onClick,
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text(if (isLoading) "Checking…" else "Use biometrics")
            }
        }
    }
}

@Composable
private fun BiometricOptInRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    availabilityLabel: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Remember me with biometrics",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = availabilityLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun AuthFeatureShowcase(
    mode: AuthMode,
    modifier: Modifier = Modifier
) {
    val title = if (mode == AuthMode.Login) "Reconnect with context" else "Build your emotion graph"
    val blurb = if (mode == AuthMode.Login) {
        "We hydrate your existing graph edges, values, and prompts before presenting refreshed layouts."
    } else {
        "Every emotion edge you define is stored in PocketBase with owner-only rules, ready for graph-style queries."
    }
    val features = if (mode == AuthMode.Login) {
        listOf("Resumes PocketBase sessions", "Adaptive Compose spacing", "Desktop ↔ iOS continuity")
    } else {
        listOf("Link people + items", "Own verbs & adverbs", "Exportable DDL migrations")
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = blurb,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                features.forEach { feature ->
                    AuthFeatureChip(text = feature)
                }
            }
        }
    }
}

@Composable
private fun AuthFeatureChip(
    text: String,
    modifier: Modifier = Modifier,
    tonalElevation: Dp = 4.dp
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(48.dp),
        tonalElevation = tonalElevation,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
        )
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
