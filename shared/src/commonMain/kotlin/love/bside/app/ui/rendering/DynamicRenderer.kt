package love.bside.app.ui.rendering

import androidx.compose.runtime.*
import love.bside.app.ui.adaptive.AdaptiveConfig

/**
 * Enterprise dynamic rendering engine
 */

sealed interface RenderStrategy {
    data object SinglePane : RenderStrategy
    data object DualPane : RenderStrategy
    data object TriplePane : RenderStrategy
    data class Grid(val columns: Int) : RenderStrategy
}

sealed interface NavigationPattern {
    data object BottomNavigation : NavigationPattern
    data object NavigationRail : NavigationPattern
    data object NavigationDrawer : NavigationPattern
    data object TopTabBar : NavigationPattern
}

class DynamicRenderer(private val config: AdaptiveConfig) {
    
    fun getRenderStrategy(contentCount: Int): RenderStrategy {
        return when {
            config.isCompact -> RenderStrategy.SinglePane
            config.isTablet && config.isLandscape -> RenderStrategy.DualPane
            config.isExpanded && config.screenWidth.value > 1400 -> RenderStrategy.TriplePane
            config.isDesktop && contentCount > 10 -> RenderStrategy.Grid(calculateOptimalColumns())
            else -> RenderStrategy.SinglePane
        }
    }
    
    fun getNavigationPattern(itemCount: Int): NavigationPattern {
        return when {
            config.isMobile && itemCount in 3..5 -> NavigationPattern.BottomNavigation
            config.isMobile && itemCount > 5 -> NavigationPattern.NavigationDrawer
            config.isTablet && !config.isLandscape -> NavigationPattern.NavigationRail
            config.isTablet -> NavigationPattern.NavigationDrawer
            config.isDesktop -> NavigationPattern.NavigationDrawer
            else -> NavigationPattern.BottomNavigation
        }
    }
    
    private fun calculateOptimalColumns(): Int {
        val width = config.screenWidth.value
        return when {
            width < 600 -> 1
            width < 840 -> 2
            width < 1200 -> 3
            width < 1600 -> 4
            else -> 6
        }
    }
    
    fun shouldUseVirtualScrolling(itemCount: Int): Boolean {
        return when {
            config.capabilities.isLowEnd -> itemCount > 20
            config.isMobile -> itemCount > 50
            config.isTablet -> itemCount > 100
            config.isDesktop -> itemCount > 500
            else -> itemCount > 50
        }
    }
    
    fun getAnimationDuration(defaultMs: Int): Int {
        return when {
            config.capabilities.isLowEnd -> (defaultMs * 0.5).toInt()
            config.capabilities.batteryOptimized -> (defaultMs * 0.7).toInt()
            else -> defaultMs
        }
    }
}

@Composable
fun rememberDynamicRenderer(config: AdaptiveConfig): DynamicRenderer {
    return remember(config) { DynamicRenderer(config) }
}
