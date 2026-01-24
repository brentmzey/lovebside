package love.bside.app.ui.utils

import androidx.compose.runtime.Composable

@Composable
actual fun isDesktopPlatform(): Boolean = false

@Composable
actual fun isWebPlatform(): Boolean = false

@Composable
actual fun isMobilePlatform(): Boolean = true
