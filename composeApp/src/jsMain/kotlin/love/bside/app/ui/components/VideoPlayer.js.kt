package love.bside.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
actual fun VideoPlayer(
    url: String,
    modifier: Modifier
) {
    Box(modifier = modifier.background(Color.Black), contentAlignment = Alignment.Center) {
        Text("Video Player (Web Placeholder)", color = Color.White)
        // TODO: Implement Web Video Player using HTML <video> tag
    }
}
