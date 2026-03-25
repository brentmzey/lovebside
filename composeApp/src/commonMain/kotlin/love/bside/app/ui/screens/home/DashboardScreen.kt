package love.bside.app.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import love.bside.app.domain.models.AuthDetails
import love.bside.app.domain.models.Match
import love.bside.app.domain.models.Profile
import love.bside.app.ui.design.tokens.BsideColors
import love.bside.app.ui.design.tokens.BsideShapes
import love.bside.app.ui.theme.BsideBackground

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    details: AuthDetails,
    matches: List<Match> = emptyList(),
    onLogout: () -> Unit,
    onOpenMessaging: () -> Unit,
    onOpenProust: () -> Unit,
    onOpenMatch: (Match) -> Unit = {}
) {
    val onLogoutRemembered = remember(onLogout) { onLogout }
    val onOpenMessagingRemembered = remember(onOpenMessaging) { onOpenMessaging }
    val onOpenProustRemembered = remember(onOpenProust) { onOpenProust }
    val onOpenMatchRemembered = remember(onOpenMatch) { onOpenMatch }

    BsideBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 32.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                 HeroCard(details.profile)
                 InsightRow(details.profile)
            }

            if (matches.isNotEmpty()) {
                MatchesSection(matches, onOpenMatchRemembered)
            } else {
                EmptyMatchesCard(onOpenProustRemembered)
            }

            Column(
                modifier = Modifier.padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Quick Actions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                )
                
                DashboardActionButton(
                    text = "Messages",
                    icon = Icons.Default.ChatBubbleOutline,
                    onClick = onOpenMessagingRemembered,
                    containerColor = BsideColors.Primary,
                    contentColor = BsideColors.OnPrimary
                )

                DashboardActionButton(
                    text = "Update Questionnaire",
                    icon = Icons.Default.Edit,
                    onClick = onOpenProustRemembered,
                    containerColor = BsideColors.Secondary,
                    contentColor = BsideColors.OnSecondary
                )
                
                ValuesSection()
                
                Spacer(modifier = Modifier.height(16.dp))
                
                TextButton(
                    onClick = onLogoutRemembered,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Sign out", color = MaterialTheme.colorScheme.error)
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun MatchesSection(matches: List<Match>, onMatchClick: (Match) -> Unit) {
    val onMatchClickRemembered = remember(onMatchClick) { onMatchClick }
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            modifier = Modifier.padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically, 
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "Your Matches",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(BsideColors.Primary.copy(alpha=0.1f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    "${matches.size}",
                    style = MaterialTheme.typography.labelMedium,
                    color = BsideColors.Primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        LazyRow(
            contentPadding = PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(matches) { match ->
                MatchCard(match, onClick = { onMatchClickRemembered(match) })
            }
        }
    }
}

@Composable
fun MatchCard(match: Match, onClick: () -> Unit) {
    val profile = match.expand?.matchedUserProfile
    val name = profile?.firstName ?: "Unknown"
    val score = match.matchScore
    val onClickRemembered = remember(onClick) { onClick }
    
    Card(
        modifier = Modifier
            .width(160.dp)
            .height(220.dp)
            .clickable(onClick = onClickRemembered),
        shape = BsideShapes.Large,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                BsideColors.Primary.copy(alpha=0.3f),
                                BsideColors.Secondary.copy(alpha=0.3f)
                            ),
                            start = androidx.compose.ui.geometry.Offset(0f, Float.POSITIVE_INFINITY),
                            end = androidx.compose.ui.geometry.Offset(Float.POSITIVE_INFINITY, 0f)
                        )
                    )
            )
            
            Box(
                 modifier = Modifier.fillMaxSize(),
                 contentAlignment = Alignment.Center
            ) {
                 Text(
                     text = name.take(1),
                     style = MaterialTheme.typography.displayMedium,
                     color = Color.White.copy(alpha=0.5f),
                     fontWeight = FontWeight.Black
                 )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f)),
                            startY = 100f
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                profile?.location?.getOrNull()?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f),
                        maxLines = 1
                    )
                }
            }

            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp),
                shape = CircleShape,
                color = BsideColors.Secondary,
                contentColor = Color.White
            ) {
                Text(
                    text = "$score%",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun HeroCard(profile: Profile) {
    val age = remember(profile) { profile.birthDate.calculateAge() }
    
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
             text = "Good ${getTimeOfDay()}",
             style = MaterialTheme.typography.bodyLarge,
             color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
             text = profile.firstName.ifBlank { "Friend" },
             style = MaterialTheme.typography.displaySmall,
             fontWeight = FontWeight.Bold,
             color = MaterialTheme.colorScheme.onBackground
        )
    }
}

private fun getTimeOfDay(): String {
    val hour = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).hour
    return when (hour) {
        in 5..11 -> "Morning"
        in 12..17 -> "Afternoon"
        in 18..22 -> "Evening"
        else -> "Night"
    }
}

@Composable
private fun EmptyMatchesCard(onAction: () -> Unit) {
    val onActionRemembered = remember(onAction) { onAction }
     Card(
         modifier = Modifier
             .padding(horizontal = 24.dp)
             .fillMaxWidth()
             .clickable(onClick = onActionRemembered),
        shape = BsideShapes.Card,
        colors = CardDefaults.cardColors(containerColor = BsideColors.GlassySurface)
     ) {
          Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Text(
                  "No matches yet",
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold
              )
              Text(
                  "Answer more questions to help us find your B-Side.",
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Text(
                  "Go to Questionnaire →",
                  style = MaterialTheme.typography.labelLarge,
                  color = BsideColors.Primary,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier.padding(top = 8.dp)
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
            title = "Consistency",
            value = "85%", // Placeholder for analytics
            icon = null, // Could add icon
            modifier = Modifier.weight(1f)
        )
        DashCard(
            title = "Seeking",
            value = profile.seeking.name.lowercase().replaceFirstChar { it.titlecase() },
            icon = null,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun DashboardActionButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    containerColor: Color,
    contentColor: Color
) {
    val onClickRemembered = remember(onClick) { onClick }
    Button(
        onClick = onClickRemembered,
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp),
        shape = BsideShapes.Large,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null)
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
        }
    }
}


@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ValuesSection() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = BsideShapes.Card,
        colors = CardDefaults.cardColors(containerColor = BsideColors.GlassySurface)
    ) {
        Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                text = "Discover",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Deep Talks", "Slow Mornings", "Tech & Tea", "Vinyl Records").forEach {
                    Chip(text = it)
                }
            }
        }
    }
}

@Composable
private fun DashCard(
    title: String,
    value: String,
    icon: ImageVector?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = BsideShapes.Card,
        colors = CardDefaults.cardColors(containerColor = BsideColors.GlassySurface)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun Chip(text: String) {
    Surface(
        shape = BsideShapes.Chip,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.5f),
        modifier = Modifier.height(32.dp),
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 12.dp)) {
             Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium
             )
        }
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
