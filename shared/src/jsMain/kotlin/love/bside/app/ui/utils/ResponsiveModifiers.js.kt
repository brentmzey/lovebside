package love.bside.app.ui.utils

import androidx.compose.runtime.Composable

@Composable
actual fun isDesktopPlatform(): Boolean = false

@Composable
actual fun isWebPlatform(): Boolean = true

@Composable
actual fun isMobilePlatform(): Boolean = false
