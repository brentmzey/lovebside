package love.bside.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import love.bside.app.ui.design.tokens.BsideElevation
import love.bside.app.ui.design.tokens.BsideShapes
import love.bside.app.ui.design.tokens.BsideSpacing

/**
 * BSide Card Component following Apple Human Interface Guidelines
 * 
 * Features:
 * - Elevated with subtle shadow
 * - Rounded corners (16dp for cards)
 * - Consistent padding
 * - Clean, minimal design
 * 
 * **Responsive Design:**
 * Cards automatically adapt to container width. For content areas, wrap in:
 * ```
 * Column(modifier = Modifier.responsiveContentWidth()) {
 *     BSideCard { /* content */ }
 * }
 * ```
 */

@Composable
fun BSideCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    elevation: Int = 2, // 1-5 scale
    content: @Composable () -> Unit
) {
    val cardElevation = when (elevation) {
        1 -> BsideElevation.Level1
        2 -> BsideElevation.Level2
        3 -> BsideElevation.Level3
        4 -> BsideElevation.Level4
        5 -> BsideElevation.Level5
        else -> BsideElevation.Level2
    }
    
    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = modifier,
            shape = BsideShapes.Card,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = cardElevation
            )
        ) {
            Column(
                modifier = Modifier.padding(BsideSpacing.CardPadding)
            ) {
                content()
            }
        }
    } else {
        Card(
            modifier = modifier,
            shape = BsideShapes.Card,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = cardElevation
            )
        ) {
            Column(
                modifier = Modifier.padding(BsideSpacing.CardPadding)
            ) {
                content()
            }
        }
    }
}
