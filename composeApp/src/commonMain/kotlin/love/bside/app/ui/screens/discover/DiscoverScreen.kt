package love.bside.app.ui.screens.discover

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import love.bside.app.ui.theme.BSideTokens
import love.bside.app.ui.theme.BsideBrand
import kotlin.math.abs
import kotlin.math.roundToInt

data class ProfileCard(
    val id: String,
    val name: String,
    val age: Int,
    val bio: String,
    val matchScore: Int,
    val distance: String,
    val interests: List<String>,
    val photos: List<String>
)

@Composable
fun DiscoverScreen(
    modifier: Modifier = Modifier,
    profiles: List<ProfileCard> = emptyList(),
    onLike: (ProfileCard) -> Unit = {},
    onPass: (ProfileCard) -> Unit = {},
    onSuperLike: (ProfileCard) -> Unit = {}
) {
    var currentIndex by remember { mutableStateOf(0) }
    val currentProfile = profiles.getOrNull(currentIndex)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        BsideBrand.TealTileLight,
                        BsideBrand.MistyTeal
                    )
                )
            )
            .padding(BSideTokens.Spacing.M)
    ) {
        if (currentProfile != null) {
            // Profile card here...
            Text(
                "Discover Screen - ${currentProfile.name}",
                modifier = Modifier.align(Alignment.Center)
            )
        } else {
            Text(
                "No more profiles!",
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}
