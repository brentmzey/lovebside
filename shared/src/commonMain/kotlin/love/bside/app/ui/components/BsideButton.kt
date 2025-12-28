package love.bside.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import love.bside.app.ui.design.tokens.*

/**
 * BSide primary button with beautiful gradient and animations.
 * KMP-compatible, works on all platforms.
 */
@Composable
fun BsideButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    gradient: Boolean = true,
    colors: ButtonColors = ButtonColors.Primary
) {
    // Scale animation on press
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "button_scale"
    )
    
    val interactionSource = remember { MutableInteractionSource() }
    
    Box(
        modifier = modifier
            .height(56.dp)
            .clip(BsideShapes.Button)
            .background(
                if (gradient) {
                    Brush.horizontalGradient(
                        colors = listOf(
                            colors.background,
                            colors.backgroundGradientEnd ?: colors.background
                        )
                    )
                } else {
                    Brush.linearGradient(listOf(colors.background, colors.background))
                }
            )
            .clickable(
                onClick = onClick,
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null  // Remove ripple for now, works cross-platform
            )
            .scale(scale),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = BsideTypography.ButtonText,
            color = if (enabled) colors.content else colors.content.copy(alpha = 0.5f)
        )
    }
}

/**
 * Button color schemes
 */
data class ButtonColors(
    val background: Color,
    val backgroundGradientEnd: Color? = null,
    val content: Color
) {
    companion object {
        val Primary = ButtonColors(
            background = BsideColors.Primary,
            backgroundGradientEnd = BsideColors.PrimaryVariant,
            content = BsideColors.OnPrimary
        )
        
        val Secondary = ButtonColors(
            background = BsideColors.Secondary,
            backgroundGradientEnd = BsideColors.SecondaryVariant,
            content = BsideColors.OnSecondary
        )
        
        val Outlined = ButtonColors(
            background = Color.Transparent,
            content = BsideColors.Primary
        )
        
        val Text = ButtonColors(
            background = Color.Transparent,
            content = BsideColors.Primary
        )
    }
}
