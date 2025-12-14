package love.bside.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.composeapp.generated.resources.Res
import app.composeapp.generated.resources.bside_logo
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource

private const val LOGO_ASPECT_RATIO = 541f / 756f

/**
 * Logo component for consistent branding across the app
 * Displays the B-Side brand art with optional size control
 */
@OptIn(ExperimentalResourceApi::class)
@Composable
fun BsideLogo(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    showText: Boolean = true,
    tintColor: Color? = null
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(Res.drawable.bside_logo),
            contentDescription = "B-Side logo",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .height(size)
                .aspectRatio(LOGO_ASPECT_RATIO)
        )
        if (showText) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "thoughtful dating",
                style = MaterialTheme.typography.bodySmall,
                color = tintColor ?: MaterialTheme.colorScheme.onSurfaceVariant
            )
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
