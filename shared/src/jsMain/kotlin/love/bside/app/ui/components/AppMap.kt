package love.bside.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import love.bside.app.domain.models.MapType

@Composable
actual fun AppMap(
    lat: Double,
    lng: Double,
    mapType: MapType,
    modifier: Modifier
) {
    // No-op for JS currently
}
