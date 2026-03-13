package love.bside.app.ui.adaptive

import androidx.compose.ui.unit.dp

/**
 * Android implementation of adaptive configuration
 * Note: This is a simplified stub - real implementation would detect actual device properties
 */
actual fun createAdaptiveConfig(): AdaptiveConfig {
    return AdaptiveConfig(
        platform = Platform.ANDROID_MOBILE,
        formFactor = FormFactor.COMPACT,
        inputModalities = setOf(InputModality.TOUCH),
        screenWidth = 360.dp,
        screenHeight = 800.dp,
        capabilities = DeviceCapabilities(),
        isLandscape = false,
        isDarkMode = false
    )
}
