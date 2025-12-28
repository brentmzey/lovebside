package love.bside.app.ui.screens.auth

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Message
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import love.bside.app.ui.theme.BsideBrand
import kotlin.math.cos
import kotlin.math.sin

// Helper function to generate pastel colors from string
private fun generatePastelColor(name: String): Color {
    val hash = name.hashCode()
    val hue = (hash and 0xFF) / 255f * 360f
    return Color.hsl(hue, 0.5f, 0.8f)
}

@Composable
fun LandingScreen(
    onLoginOptions: () -> Unit,
    onSignUp: () -> Unit,
    profiles: List<love.bside.app.domain.models.Profile> = emptyList(),
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .systemBarsPadding()
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(60.dp))
            
            // Orbit Animation Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                OrbitAnimation(profiles)
            }
            
            // Content Area with CTA buttons
            Column(
                modifier = Modifier
                    .padding(horizontal = 32.dp, vertical = 24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Let's meet new\\npeople around you",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = BsideBrand.Charcoal
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Login Buttons
                LandingButton(
                    text = "Login with Phone",
                    icon = Icons.Default.Call,
                    backgroundColor = Color(0xFF4A1D4D),
                    contentColor = Color.White,
                    iconContainerColor = Color.White,
                    iconTintColor = Color(0xFF4A1D4D),
                    onClick = onLoginOptions
                )
                
                LandingButton(
                    text = "Login with Google",
                    icon = null,
                    isGoogle = true,
                    backgroundColor = Color(0xFFFFF0F5),
                    contentColor = Color(0xFF4A1D4D),
                    iconContainerColor = Color.White,
                    iconTintColor = Color.Unspecified,
                    onClick = onLoginOptions
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Sign Up Text
                val annotatedString = buildAnnotatedString {
                    append("Don't have an account? ")
                    withStyle(style = SpanStyle(color = BsideBrand.TealTileDark, fontWeight = FontWeight.Bold)) {
                        append("Sign Up")
                    }
                }
                
                Text(
                    text = annotatedString,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    modifier = Modifier.clickable(onClick = onSignUp)
                )
                
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun OrbitAnimation(profiles: List<love.bside.app.domain.models.Profile>) {
    val infiniteTransition = rememberInfiniteTransition()
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "pulse"
    )
    
    val outerRadius = 140.dp
    val innerRadius = 90.dp
    
    Box(contentAlignment = Alignment.Center) {
        // Draw circles (can be expanded later)
        
        // Floating Elements on Outer Ring
        if (profiles.isNotEmpty()) {
            val profileSpots = listOf(0f, 90f, 135f, 180f, 270f, 315f)
            
            profileSpots.forEachIndexed { index, angle ->
                val profile = profiles.getOrNull(index % profiles.size)
                if (profile != null) {
                    OrbitItem(angle = angle, radius = outerRadius) {
                        val color = generatePastelColor(profile.firstName)
                        AvatarPlaceholder(color = color, initial = profile.firstName.firstOrNull()?.toString() ?: "?")
                    }
                }
            }
        } else {
            // Fallback avatars
            OrbitItem(angle = 0f, radius = outerRadius) { AvatarPlaceholder(Color(0xFF81C784)) }
            OrbitItem(angle = 90f, radius = outerRadius) { AvatarPlaceholder(Color(0xFFE57373)) }
            OrbitItem(angle = 135f, radius = outerRadius) { AvatarPlaceholder(Color(0xFFFFD54F)) }
            OrbitItem(angle = 180f, radius = outerRadius) { AvatarPlaceholder(Color(0xFF7986CB)) }
            OrbitItem(angle = 270f, radius = outerRadius) { AvatarPlaceholder(Color(0xFFBA68C8)) }
            OrbitItem(angle = 315f, radius = outerRadius) { AvatarPlaceholder(Color(0xFFFF8A65), size = 64.dp) }
        }
        
        // Icon decorations
        OrbitItem(angle = 45f, radius = outerRadius) {
            Icon(Icons.Default.LocationOn, null, tint = Color(0xFFF48FB1), modifier = Modifier.size(24.dp))
        }
        
        OrbitItem(angle = 225f, radius = outerRadius) {
            Icon(Icons.Default.Message, null, tint = Color(0xFFF06292), modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
fun AvatarPlaceholder(color: Color, size: Dp = 48.dp, initial: String? = null) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(Color.White)
            .padding(2.dp)
            .clip(CircleShape)
            .background(color),
        contentAlignment = Alignment.Center
    ) {
        if (initial != null) {
            Text(initial, color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun OrbitItem(angle: Float, radius: Dp, content: @Composable () -> Unit) {
    val angleRadians = (angle - 90) * kotlin.math.PI / 180.0
    val x = (radius.value * cos(angleRadians)).dp
    val y = (radius.value * sin(angleRadians)).dp
    
    Box(modifier = Modifier.offset(x = x, y = y)) {
        content()
    }
}

@Composable
private fun LandingButton(
    text: String,
    icon: ImageVector?,
    onClick: () -> Unit,
    backgroundColor: Color,
    contentColor: Color,
    iconContainerColor: Color,
    iconTintColor: Color,
    isGoogle: Boolean = false
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            contentColor = contentColor
        ),
        shape = RoundedCornerShape(28.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(iconContainerColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = iconTintColor, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
            }
            
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.size(32.dp)) // Balance layout
        }
    }
}
