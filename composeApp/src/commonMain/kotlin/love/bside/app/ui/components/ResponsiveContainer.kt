package love.bside.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import love.bside.app.ui.utils.isDesktopPlatform
import love.bside.app.ui.utils.isMobilePlatform

/**
 * Responsive Container for adaptive layouts
 * 
 * Mobile: Full width, edge-to-edge
 * Tablet/Desktop: Max width with center alignment
 * 
 * Prevents awkward wide layouts on large screens
 */

object ResponsiveBreakpoints {
    val Mobile = 640.dp
    val Tablet = 1024.dp
    val Desktop = 1440.dp
    
    val ContentMaxWidth = 800.dp
    val WideContentMaxWidth = 1200.dp
    val FormMaxWidth = 480.dp
}

@Composable
fun ResponsiveContainer(
    modifier: Modifier = Modifier,
    maxWidth: androidx.compose.ui.unit.Dp = ResponsiveBreakpoints.ContentMaxWidth,
    horizontalAlignment: Alignment.Horizontal = Alignment.CenterHorizontally,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = maxWidth),
            horizontalAlignment = horizontalAlignment,
            content = content
        )
    }
}

@Composable
fun ResponsiveRow(
    modifier: Modifier = Modifier,
    forceColumn: Boolean = false,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalAlignment: Alignment.Vertical = Alignment.Top,
    content: @Composable () -> Unit
) {
    val useColumn = forceColumn || isMobilePlatform()
    
    if (useColumn) {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            content = { content() }
        )
    } else {
        Row(
            modifier = modifier,
            horizontalArrangement = horizontalArrangement,
            verticalAlignment = verticalAlignment,
            content = { content() }
        )
    }
}

@Composable
fun AdaptiveGrid(
    modifier: Modifier = Modifier,
    columns: GridColumns = GridColumns.Adaptive,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.spacedBy(16.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(16.dp),
    content: @Composable () -> Unit
) {
    val columnCount = when (columns) {
        is GridColumns.Fixed -> columns.count
        GridColumns.Adaptive -> {
            when {
                isMobilePlatform() -> 1
                isDesktopPlatform() -> 3
                else -> 2 // Tablet
            }
        }
    }
    
    // For now, use FlowRow - can be enhanced with LazyVerticalGrid
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = horizontalArrangement,
        verticalArrangement = verticalArrangement,
        maxItemsInEachRow = columnCount,
        content = { content() }
    )
}

sealed class GridColumns {
    data class Fixed(val count: Int) : GridColumns()
    object Adaptive : GridColumns()
}
