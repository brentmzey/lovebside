package love.bside.app.ui.accessibility

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver

/**
 * Enterprise accessibility system
 */

/**
 * Accessibility configuration
 */
data class AccessibilityConfig(
    val screenReaderEnabled: Boolean = false,
    val hapticFeedbackEnabled: Boolean = true,
    val soundFeedbackEnabled: Boolean = true,
    val highContrastEnabled: Boolean = false,
    val reduceMotionEnabled: Boolean = false,
    val largeTextEnabled: Boolean = false,
    val colorBlindMode: ColorBlindMode = ColorBlindMode.NONE
)

enum class ColorBlindMode {
    NONE,
    PROTANOPIA,    // Red-blind
    DEUTERANOPIA,  // Green-blind
    TRITANOPIA,    // Blue-blind
    MONOCHROMACY   // Total color blindness
}

/**
 * Custom semantics properties
 */
val SemanticRole = SemanticsPropertyKey<String>("SemanticRole")
val SemanticCategory = SemanticsPropertyKey<String>("SemanticCategory")
val SemanticImportance = SemanticsPropertyKey<Int>("SemanticImportance")

fun SemanticsPropertyReceiver.semanticRole(role: String) {
    this[SemanticRole] = role
}

fun SemanticsPropertyReceiver.semanticCategory(category: String) {
    this[SemanticCategory] = category
}

fun SemanticsPropertyReceiver.semanticImportance(importance: Int) {
    this[SemanticImportance] = importance
}

/**
 * Accessibility helper functions
 */
object AccessibilityHelpers {
    
    /**
     * Get accessible label for UI element
     */
    fun getAccessibleLabel(
        label: String,
        role: String,
        state: String? = null
    ): String {
        return buildString {
            append(label)
            append(", ")
            append(role)
            state?.let {
                append(", ")
                append(it)
            }
        }
    }
    
    /**
     * Get accessible action description
     */
    fun getAccessibleAction(
        action: String,
        target: String? = null
    ): String {
        return buildString {
            append(action)
            target?.let {
                append(" ")
                append(it)
            }
        }
    }
    
    /**
     * Check if element should be focusable
     */
    fun shouldBeFocusable(
        isInteractive: Boolean,
        isVisible: Boolean,
        config: AccessibilityConfig
    ): Boolean {
        return isInteractive && isVisible && config.screenReaderEnabled
    }
}

/**
 * CompositionLocal for accessibility config
 */
val LocalAccessibilityConfig = staticCompositionLocalOf { AccessibilityConfig() }

/**
 * Platform-specific accessibility functions
 */
expect fun announceForAccessibility(message: String)
expect fun provideTactileFeedback()
expect fun provideAudioFeedback()
