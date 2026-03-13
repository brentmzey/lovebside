package love.bside.app.ui.adaptive

import androidx.compose.ui.unit.dp

actual fun createAdaptiveConfig(): AdaptiveConfig {
    // Basic mock for web environment
    // In a real implementation, we would use WindowInfo or Media queries
    return AdaptiveConfig(
        platform = Platform.WEB_DESKTOP,
        formFactor = FormFactor.MEDIUM,
        inputModalities = setOf(InputModality.MOUSE, InputModality.KEYBOARD),
        screenWidth = 1920.dp,
        screenHeight = 1080.dp,
        capabilities = DeviceCapabilities(
            hasTouchScreen = false,
            hasHaptics = false
        ),
        isLandscape = true,
        isDarkMode = false
    )
}
