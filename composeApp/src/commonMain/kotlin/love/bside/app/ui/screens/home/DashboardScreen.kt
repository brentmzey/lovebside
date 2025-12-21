package love.bside.app.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import love.bside.app.domain.models.AuthDetails
import love.bside.app.domain.models.Profile

@OptIn(ExperimentalLayoutApi::class)
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import love.bside.app.domain.models.Match

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    details: AuthDetails,
    matches: List<Match> = emptyList(), // Default empty for preview/compatibility
    onLogout: () -> Unit,
    onOpenMessaging: () -> Unit,
    onOpenProust: () -> Unit,
    onOpenMatch: (Match) -> Unit = {}
) {
    val colorScheme = MaterialTheme.colorScheme
    val gradient = remember(colorScheme) {
        Brush.verticalGradient(
            0f to colorScheme.secondaryContainer,
            1f to colorScheme.primaryContainer
        )
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            HeroCard(details.profile)
            
            // Matches Carousel
            if (matches.isNotEmpty()) {
                MatchesCarousel(matches, onOpenMatch)
            }
            
            // Messaging Entry Point
            Button(
                onClick = onOpenMessaging,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary
                )
            ) {
                Text("Open Messages", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            // Proust Questionnaire Entry Point
            Button(
                onClick = onOpenProust,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary
                )
            ) {
                Text("Complete Questionnaire", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            InsightRow(details.profile)
            ValuesSection()
            Button(
                onClick = onLogout,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Sign out of B-Side", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
fun MatchesCarousel(matches: List<Match>, onMatchClick: (Match) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "Your Matches",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 4.dp)
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(matches) { match ->
                MatchCard(match, onClick = { onMatchClick(match) })
            }
        }
    }
}

@Composable
fun MatchCard(match: Match, onClick: () -> Unit) {
    val profile = match.expand?.matchedUserProfile
    val name = profile?.firstName ?: "Unknown"
    val score = match.matchScore
    
    // Construct avatar URL if available (Assuming full URL or relative)
    // profilePicture is likely just filename. needs base URL.
    // For now, assume relative filename and let AsyncImage handle or placeholder based on logic elsewhere.
    // Ideally use a helper `profile.avatarUrl(baseUrl)`. 
    // Just using placeholder logic for now if empty.
    
    Card(
        modifier = Modifier
            .width(140.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
             Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
             ) {
                 if (!profile?.profilePicture.isNullOrBlank()) {
                     // TODO: Base URL injection
                     // AsyncImage(model = getFileUrl(..., profile.profilePicture), ...)
                     // For now, simplified or text avatar
                      Text(name.take(1), style = MaterialTheme.typography.headlineMedium)
                 } else {
                     Text(name.take(1), style = MaterialTheme.typography.headlineMedium)
                 }
                 
                 // Score Badge
                 Surface(
                     modifier = Modifier.align(Alignment.BottomEnd),
                     shape = CircleShape,
                     color = MaterialTheme.colorScheme.primary,
                     contentColor = MaterialTheme.colorScheme.onPrimary
                 ) {
                     Text(
                         text = "$score%", 
                         style = MaterialTheme.typography.labelSmall, 
                         fontWeight = FontWeight.Bold,
                         modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                     )
                 }
             }
             
             Text(
                text = name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                color = MaterialTheme.colorScheme.onSurface
             )
             
             profile?.location?.let {
                 Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                 )
             }
        }
    }
}

@Composable
private fun HeroCard(profile: Profile) {
    val age = remember(profile) { profile.birthDate.calculateAge() }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)),
        shape = RoundedCornerShape(32.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = buildString {
                    append("Hey ${profile.firstName.ifBlank { "there" }}")
                    if (age != null) append(" · $age")
                    profile.location?.takeIf { it.isNotBlank() }?.let { append(" · $it") }
                },
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "We’re syncing your conversations, prompts, and shared values across every build target.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun InsightRow(profile: Profile) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        DashCard(
            title = "Alignment",
            value = "82%",
            caption = "Compared to thoughtful daters",
            modifier = Modifier.weight(1f)
        )
        DashCard(
            title = "Focus",
            value = profile.seeking.name.lowercase().replaceFirstChar { it.titlecase() },
            caption = "Updated across devices",
            modifier = Modifier.weight(1f)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ValuesSection() {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.15f)),
        shape = RoundedCornerShape(28.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Conversation sparkers",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Pragmatic romantics", "Slow coffee chats", "Code & feelings", "Analog weekends").forEach {
                    Chip(text = it)
                }
            }
            Text(
                text = "Tap run-android, run-ios, run-desktop, or run-web to see these UI tiles rendered natively.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DashCard(
    title: String,
    value: String,
    caption: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)),
        shape = RoundedCornerShape(28.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(caption, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun Chip(text: String) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

private fun LocalDate.calculateAge(): Int? = runCatching {
    val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    var age = today.year - year
    if (today.monthNumber < monthNumber || (today.monthNumber == monthNumber && today.dayOfMonth < dayOfMonth)) {
        age -= 1
    }
    age
}.getOrNull()
