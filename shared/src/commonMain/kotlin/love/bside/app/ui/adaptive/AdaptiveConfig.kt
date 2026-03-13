package love.bside.app.ui.adaptive

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Platform-aware UI adaptation system.
 * Automatically adapts UI based on platform, screen size, and capabilities.
 */

enum class Platform {
    ANDROID_MOBILE, ANDROID_TABLET, ANDROID_TV, ANDROID_WEAR,
    IOS_PHONE, IOS_TABLET, MACOS,
    WINDOWS, LINUX,
    WEB_MOBILE, WEB_TABLET, WEB_DESKTOP
}

enum class FormFactor {
    COMPACT,      // Phone, Watch
    MEDIUM,       // Tablet, Small Desktop
    EXPANDED,     // Large Desktop, TV
    EXTRA_LARGE   // Ultra-wide, Multi-monitor
}

enum class InputModality {
    TOUCH, MOUSE, KEYBOARD, GAMEPAD, VOICE, STYLUS
}

data class DeviceCapabilities(
    val hasNotch: Boolean = false,
    val hasDynamicIsland: Boolean = false,
    val hasPhysicalKeyboard: Boolean = false,
    val hasTouchScreen: Boolean = true,
    val hasHaptics: Boolean = false,
    val supportsBiometrics: Boolean = false,
    val supportsNFC: Boolean = false,
    val supportsAR: Boolean = false,
    val batteryOptimized: Boolean = false,
    val isLowEnd: Boolean = false
)

data class AdaptiveConfig(
    val platform: Platform,
    val formFactor: FormFactor,
    val inputModalities: Set<InputModality>,
    val screenWidth: Dp,
    val screenHeight: Dp,
    val capabilities: DeviceCapabilities,
    val isLandscape: Boolean,
    val isDarkMode: Boolean,
    val scale: Float = 1f
) {
    val isCompact: Boolean get() = formFactor == FormFactor.COMPACT
    val isMedium: Boolean get() = formFactor == FormFactor.MEDIUM
    val isExpanded: Boolean get() = formFactor == FormFactor.EXPANDED
    
    val isMobile: Boolean get() = platform in setOf(Platform.ANDROID_MOBILE, Platform.IOS_PHONE, Platform.WEB_MOBILE)
    val isTablet: Boolean get() = platform in setOf(Platform.ANDROID_TABLET, Platform.IOS_TABLET, Platform.WEB_TABLET)
    val isDesktop: Boolean get() = platform in setOf(Platform.MACOS, Platform.WINDOWS, Platform.LINUX, Platform.WEB_DESKTOP)
    
    val minTouchTargetSize: Dp get() = when {
        platform == Platform.ANDROID_WEAR -> 32.dp
        isMobile -> 48.dp
        isTablet -> 44.dp
        isDesktop && InputModality.TOUCH in inputModalities -> 44.dp
        isDesktop -> 24.dp
        platform == Platform.ANDROID_TV -> 64.dp
        else -> 48.dp
    }
}

val LocalAdaptiveConfig = staticCompositionLocalOf<AdaptiveConfig> {
    error("No AdaptiveConfig provided")
}

@Composable
fun AdaptiveConfigProvider(config: AdaptiveConfig, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalAdaptiveConfig provides config) {
        content()
    }
}

expect fun createAdaptiveConfig(): AdaptiveConfig
