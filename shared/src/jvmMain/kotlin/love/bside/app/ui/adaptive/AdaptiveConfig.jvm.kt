package love.bside.app.ui.adaptive

import androidx.compose.ui.unit.dp

actual fun createAdaptiveConfig(): AdaptiveConfig {
    // Desktop default configuration
    return AdaptiveConfig(
        platform = Platform.WINDOWS, // Default to Windows, could be detected
        formFactor = FormFactor.EXPANDED,
        inputModalities = setOf(InputModality.MOUSE, InputModality.KEYBOARD),
        screenWidth = 1920.dp,
        screenHeight = 1080.dp,
        capabilities = DeviceCapabilities(
            hasPhysicalKeyboard = true,
            hasTouchScreen = false,
            hasHaptics = false
        ),
        isLandscape = true,
        isDarkMode = false
    )
}
