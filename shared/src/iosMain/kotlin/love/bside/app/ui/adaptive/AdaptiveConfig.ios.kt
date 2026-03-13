package love.bside.app.ui.adaptive

import androidx.compose.ui.unit.dp

actual fun createAdaptiveConfig(): AdaptiveConfig {
    // iOS default configuration
    return AdaptiveConfig(
        platform = Platform.IOS_PHONE,
        formFactor = FormFactor.COMPACT,
        inputModalities = setOf(InputModality.TOUCH),
        screenWidth = 390.dp,
        screenHeight = 844.dp,
        capabilities = DeviceCapabilities(
            hasPhysicalKeyboard = false,
            hasTouchScreen = true,
            hasHaptics = true
        ),
        isLandscape = false,
        isDarkMode = false
    )
}
