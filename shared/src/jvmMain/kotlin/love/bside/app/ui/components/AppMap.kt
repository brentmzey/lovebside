package love.bside.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import love.bside.app.domain.models.MapType

@Composable
actual fun AppMap(
    lat: Double,
    lng: Double,
    mapType: MapType,
    modifier: Modifier
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Text("Maps are not supported on Desktop/JVM target.")
    }
}
