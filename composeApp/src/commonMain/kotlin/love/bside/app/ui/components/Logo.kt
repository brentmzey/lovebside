package love.bside.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Logo component for consistent branding across the app
 * Displays the bside brand text with optional size control
 */
@Composable
fun BsideLogo(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    showText: Boolean = true,
    tintColor: androidx.compose.ui.graphics.Color? = null
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // For now, use text logo until we set up image resources properly
        Column {
            Text(
                text = "bside",
                style = when {
                    size >= 48.dp -> MaterialTheme.typography.headlineLarge
                    size >= 32.dp -> MaterialTheme.typography.headlineMedium
                    else -> MaterialTheme.typography.titleLarge
                },
                fontWeight = FontWeight.Bold,
                color = tintColor ?: MaterialTheme.colorScheme.primary
            )
            if (showText) {
                Text(
                    text = "thoughtful dating",
                    style = MaterialTheme.typography.bodySmall,
                    color = tintColor ?: MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Compact logo for smaller spaces (like settings header)
 */
@Composable
fun BsideLogoCompact(
    modifier: Modifier = Modifier,
    size: Dp = 32.dp
) {
    BsideLogo(
        modifier = modifier,
        size = size,
        showText = false
    )
}
