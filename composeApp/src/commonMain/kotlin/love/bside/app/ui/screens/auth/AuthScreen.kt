package love.bside.app.ui.screens.auth

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
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
import love.bside.app.security.BiometricAvailability
import love.bside.app.security.SecureAuthFactor
import love.bside.app.security.SecureAuthResult
import love.bside.app.security.SecurePromptText
import love.bside.app.security.usecase.BiometricLoginUseCase
import love.bside.app.security.usecase.EnableBiometricLoginUseCase
import love.bside.app.security.usecase.ObserveSecureEnrollmentsUseCase
import love.bside.app.ui.theme.BsideBackground
import love.bside.app.ui.design.tokens.BsideColors
import love.bside.app.domain.usecase.GetDiscoveryUsersUseCase
import love.bside.app.domain.models.Profile
import love.bside.app.ui.design.tokens.BsideShapes

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AuthScreen(
    modifier: Modifier = Modifier,
    viewModel: AuthViewModel = org.koin.compose.viewmodel.koinViewModel(),
    biometricLoginUseCase: BiometricLoginUseCase,
    enableBiometricLoginUseCase: EnableBiometricLoginUseCase,
    observeSecureEnrollmentsUseCase: ObserveSecureEnrollmentsUseCase,
    onAuthenticated: (AuthDetails) -> Unit
) {
    val scope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsState()

    val enrollments by observeSecureEnrollmentsUseCase().collectAsState(initial = emptyList())
    val hasBiometricEnrollment = remember(enrollments) { enrollments.any { it.factor is SecureAuthFactor.Biometric } }
    var biometricAvailability by remember { mutableStateOf<BiometricAvailability>(BiometricAvailability.Unknown) }
    var wantsBiometric by remember { mutableStateOf(false) }
    var biometricError by remember { mutableStateOf<String?>(null) }
    var isBiometricLoading by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        biometricAvailability = biometricLoginUseCase.availability()
    }

    val describeAvailability = remember(biometricAvailability) {
        when (biometricAvailability) {
            BiometricAvailability.Available -> "Ready to use"
            BiometricAvailability.NoHardware -> "This device lacks biometric hardware"
            BiometricAvailability.NotEnrolled -> "Enroll Face ID or Touch ID in system settings"
            is BiometricAvailability.Unavailable -> (biometricAvailability as BiometricAvailability.Unavailable).reason
            BiometricAvailability.Unknown -> "Biometric status unknown"
        }
    }

    val attemptBiometricLogin = remember {
        {
            if (hasBiometricEnrollment) {
                scope.launch {
                    isBiometricLoading = true
                    val result = biometricLoginUseCase(SecurePromptText.default())
                    isBiometricLoading = false
                    when (result) {
                        is SecureAuthResult.Success -> onAuthenticated(result.details)
                        SecureAuthResult.Canceled -> Unit
                        SecureAuthResult.NoEnrollment -> biometricError = "No biometric enrollment found"
                        is SecureAuthResult.Error -> biometricError = result.message
                        is SecureAuthResult.Unavailable -> biometricError =
                            describeAvailability
                    }
                }
            }
        }
    }

    val submit = remember {
        {
            viewModel.submit { authDetails ->
                if (uiState.mode == AuthMode.Login && wantsBiometric &&
                    biometricAvailability is BiometricAvailability.Available
                ) {
                    scope.launch {
                        runCatching {
                            enableBiometricLoginUseCase(authDetails, uiState.email.trim())
                        }
                        wantsBiometric = false
                    }
                }
                onAuthenticated(authDetails)
            }
        }
    }
    
    AnimatedContent(
        targetState = uiState.mode,
        transitionSpec = {
            if (targetState == AuthMode.Landing || initialState == AuthMode.Landing) {
                fadeIn(animationSpec = tween(500)) togetherWith fadeOut(animationSpec = tween(500))
            } else if (targetState == AuthMode.SignUp) {
                slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut()
            } else {
                slideInHorizontally { -it } + fadeIn() togetherWith slideOutHorizontally { it } + fadeOut()
            }
        }
    ) { currentMode ->
        if (currentMode == AuthMode.Landing) {
            LandingScreen(
                profiles = uiState.discoveryProfiles,
                onLoginOptions = { viewModel.onModeChange(AuthMode.Login) },
                onSignUp = { viewModel.onModeChange(AuthMode.SignUp) }
            )
        } else {
            AuthFormContent(
                uiState = uiState,
                onStateChange = {
                    if (it.email != uiState.email) {
                        viewModel.onEmailChange(it.email)
                    }
                    if (it.password != uiState.password) {
                        viewModel.onPasswordChange(it.password)
                    }
                    if (it.confirmPassword != uiState.confirmPassword) {
                        viewModel.onConfirmPasswordChange(it.confirmPassword)
                    }
                    if (it.firstName != uiState.firstName) {
                        viewModel.onFirstNameChange(it.firstName)
                    }
                    if (it.lastName != uiState.lastName) {
                        viewModel.onLastNameChange(it.lastName)
                    }
                    if (it.birthDate != uiState.birthDate) {
                        viewModel.onBirthDateChange(it.birthDate)
                    }
                    if (it.seeking != uiState.seeking) {
                        viewModel.onSeekingChange(it.seeking)
                    }
                },
                onModeChange = { viewModel.onModeChange(it) },
                onBack = { viewModel.onModeChange(AuthMode.Landing) },
                onSubmit = submit,
                hasBiometricEnrollment = hasBiometricEnrollment,
                biometricAvailability = biometricAvailability,
                isBiometricLoading = isBiometricLoading,
                biometricError = biometricError,
                wantsBiometric = wantsBiometric,
                onWantsBiometricChange = { wantsBiometric = it },
                onAttemptBiometric = {
                    biometricError = null
                    attemptBiometricLogin()
                },
                modifier = modifier
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AuthFormContent(
    uiState: AuthUiState,
    onStateChange: (AuthUiState) -> Unit,
    onModeChange: (AuthMode) -> Unit,
    onBack: () -> Unit,
    onSubmit: () -> Unit,
    hasBiometricEnrollment: Boolean,
    biometricAvailability: BiometricAvailability,
    isBiometricLoading: Boolean,
    biometricError: String?,
    wantsBiometric: Boolean,
    onWantsBiometricChange: (Boolean) -> Unit,
    onAttemptBiometric: () -> Unit,
    modifier: Modifier
) {
    val scrollState = rememberScrollState()

    BsideBackground {
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(vertical = 40.dp, horizontal = 24.dp)
                .systemBarsPadding()
                .imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            
            // Header Section
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = onBack, modifier = Modifier.align(Alignment.Start)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                 
                BrandGlyph(modifier = Modifier.size(80.dp))
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = if (uiState.mode == AuthMode.Login) "Welcome Back" else "Create Account",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (uiState.mode == AuthMode.Login) "Sign in to continue your journey" else "Sign up to start matching",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }

            if (hasBiometricEnrollment && biometricAvailability is BiometricAvailability.Available) {
                BiometricQuickLoginCard(
                    isLoading = isBiometricLoading,
                    availabilityLabel = "Tap to use Face ID / Touch ID",
                    onClick = onAttemptBiometric
                )
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.fillMaxWidth().widthIn(max = 480.dp)
            ) {
                AuthModeSwitcher(
                    mode = uiState.mode,
                    onModeChange = onModeChange
                )
                
                AuthTextField(
                    label = "Email",
                    value = uiState.email,
                    onValueChange = { onStateChange(uiState.copy(email = it)) },
                    keyboardType = KeyboardType.Email
                )
                AuthTextField(
                    label = "Password",
                    value = uiState.password,
                    onValueChange = { onStateChange(uiState.copy(password = it)) },
                    keyboardType = KeyboardType.Password,
                    isPassword = true
                )

                AnimatedVisibility(uiState.mode == AuthMode.SignUp) {
                    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        AuthTextField(
                            label = "Confirm Password",
                            value = uiState.confirmPassword,
                            onValueChange = { onStateChange(uiState.copy(confirmPassword = it)) },
                            keyboardType = KeyboardType.Password,
                            isPassword = true
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Box(modifier = Modifier.weight(1f)) {
                                AuthTextField(
                                    label = "First Name",
                                    value = uiState.firstName,
                                    onValueChange = { onStateChange(uiState.copy(firstName = it)) }
                                )
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                AuthTextField(
                                    label = "Last Name",
                                    value = uiState.lastName,
                                    onValueChange = { onStateChange(uiState.copy(lastName = it)) }
                                )
                            }
                        }
                        
                        AuthTextField(
                            label = "Birth Date (YYYY-MM-DD)",
                            value = uiState.birthDate,
                            onValueChange = { onStateChange(uiState.copy(birthDate = it)) }
                        )

                        Text(
                            text = "I am seeking...",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            SeekingStatus.values().forEach { status ->
                                FilterChip(
                                    selected = uiState.seeking == status,
                                    label = { Text(status.name.lowercase().replaceFirstChar { it.titlecase() }) },
                                    onClick = { onStateChange(uiState.copy(seeking = status)) },
                                    leadingIcon = if (uiState.seeking == status) {
                                        { Icon(Icons.Default.Check, contentDescription = null) }
                                    } else null,
                                    shape = BsideShapes.Chip,
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = BsideColors.Teal,
                                        selectedLabelColor = Color.White,
                                        selectedLeadingIconColor = Color.White
                                    )
                                )
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
                    onClick = onSubmit,
                    enabled = uiState.canSubmit && !uiState.isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(top = 16.dp),
                    shape = BsideShapes.Button,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BsideColors.Primary,
                        contentColor = BsideColors.OnPrimary,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                   if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = BsideColors.OnPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = if (uiState.mode == AuthMode.Login) "Sign In" else "Create Account",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                if (!hasBiometricEnrollment && biometricAvailability is BiometricAvailability.Available && uiState.mode == AuthMode.Login) {
                     BiometricOptInRow(
                         checked = wantsBiometric,
                         onCheckedChange = onWantsBiometricChange
                     )
                }
            }
        }
    }
}


@Composable
private fun BrandGlyph(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(BsideColors.Primary, BsideColors.Secondary)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "B",
            style = MaterialTheme.typography.displayMedium,
            color = Color.White,
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
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = BsideShapes.Card,
        elevation = CardDefaults.cardElevation(0.dp),
        colors = CardDefaults.cardColors(containerColor = BsideColors.GlassySurface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                 Text(
                     text = "Quick Unlock",
                     style = MaterialTheme.typography.titleMedium,
                     fontWeight = FontWeight.SemiBold
                 )
                 Text(
                     text = availabilityLabel,
                     style = MaterialTheme.typography.bodySmall,
                     color = MaterialTheme.colorScheme.onSurfaceVariant
                 )
            }
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            } else {
                Text("Use", color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}


@Composable
private fun BiometricOptInRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Enable Biometrics",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Securely log in next time",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked, 
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = BsideColors.Primary,
                checkedTrackColor = BsideColors.Teal
            )
        )
    }
}

@Composable
private fun AuthModeSwitcher(mode: AuthMode, onModeChange: (AuthMode) -> Unit) {
    Card(
        shape = BsideShapes.Full,
        colors = CardDefaults.cardColors(containerColor = BsideColors.GlassySurface),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(modifier = Modifier.padding(4.dp)) {
             listOf(AuthMode.Login, AuthMode.SignUp).forEach { entry ->
                 val selected = entry == mode
                 Box(
                     modifier = Modifier
                         .weight(1f)
                         .clip(BsideShapes.Full)
                         .background(if (selected) BsideColors.Surface else Color.Transparent)
                         .clickable { onModeChange(entry) }
                         .padding(vertical = 10.dp),
                     contentAlignment = Alignment.Center
                 ) {
                     Text(
                         text = if (entry == AuthMode.Login) "Sign In" else "Sign Up",
                         style = MaterialTheme.typography.titleSmall,
                         fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                         color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
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
        shape = BsideShapes.TextField,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = BsideColors.Primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha=0.3f),
            focusedLabelColor = BsideColors.Primary
        )
    )
}

@Composable
private fun ErrorCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = BsideShapes.Card,
        colors = CardDefaults.cardColors(containerColor = BsideColors.Error.copy(alpha = 0.2f))
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(16.dp),
            color = BsideColors.Error,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}