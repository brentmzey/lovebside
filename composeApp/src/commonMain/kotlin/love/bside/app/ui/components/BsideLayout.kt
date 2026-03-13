package love.bside.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Responsive layout helpers following Apple HIG spacing guidelines
 * Consistent spacing creates visual rhythm and hierarchy
 */

// Standard spacing values (following 8dp grid system)
object BsideSpacing {
    val none = 0.dp
    val extraSmall = 4.dp
    val small = 8.dp
    val medium = 16.dp
    val large = 24.dp
    val extraLarge = 32.dp
    val huge = 48.dp
}

/**
 * Adaptive Container - Adjusts layout based on screen size
 */
@Composable
fun AdaptiveContainer(
    modifier: Modifier = Modifier,
    maxWidth: Dp = 600.dp,
    horizontalPadding: Dp = BsideSpacing.medium,
    verticalPadding: Dp = BsideSpacing.medium,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
        contentAlignment = Alignment.TopCenter
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = maxWidth)
                .fillMaxWidth()
        ) {
            content()
        }
    }
}

/**
 * Responsive Row - Switches to column on narrow screens
 */
@Composable
fun ResponsiveRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalAlignment: Alignment.Vertical = Alignment.Top,
    spacing: Dp = BsideSpacing.medium,
    breakpoint: Dp = 600.dp,
    content: @Composable RowScope.() -> Unit
) {
    // For now, always use Row
    // TODO: Add window size detection for true responsive behavior
    Row(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        verticalAlignment = verticalAlignment
    ) {
        content()
    }
}

/**
 * Spacers with semantic names
 */
@Composable
fun SmallSpacer(modifier: Modifier = Modifier) {
    Spacer(modifier = modifier.height(BsideSpacing.small))
}

@Composable
fun MediumSpacer(modifier: Modifier = Modifier) {
    Spacer(modifier = modifier.height(BsideSpacing.medium))
}

@Composable
fun LargeSpacer(modifier: Modifier = Modifier) {
    Spacer(modifier = modifier.height(BsideSpacing.large))
}

@Composable
fun HorizontalSmallSpacer(modifier: Modifier = Modifier) {
    Spacer(modifier = modifier.width(BsideSpacing.small))
}

@Composable
fun HorizontalMediumSpacer(modifier: Modifier = Modifier) {
    Spacer(modifier = modifier.width(BsideSpacing.medium))
}

/**
 * Section Header - For grouping content with proper spacing
 */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = BsideSpacing.small),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        androidx.compose.material3.Text(
            text = title,
            style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
            color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground
        )
        action?.invoke()
    }
}

/**
 * Divider with proper spacing
 */
@Composable
fun BsideDivider(
    modifier: Modifier = Modifier,
    thickness: Dp = 1.dp,
    color: androidx.compose.ui.graphics.Color = androidx.compose.material3.MaterialTheme.colorScheme.outlineVariant
) {
    androidx.compose.material3.HorizontalDivider(
        modifier = modifier.padding(vertical = BsideSpacing.small),
        thickness = thickness,
        color = color
    )
}

/**
 * Bottom Sheet Container - For modal content
 */
@Composable
fun BottomSheetContainer(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(BsideSpacing.large)
    ) {
        // Handle indicator
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(4.dp)
                .align(Alignment.CenterHorizontally)
                .padding(bottom = BsideSpacing.medium)
        )
        content()
    }
}
