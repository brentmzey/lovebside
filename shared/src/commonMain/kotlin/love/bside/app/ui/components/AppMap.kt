package love.bside.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import love.bside.app.domain.models.MapType

@Composable
expect fun AppMap(
    lat: Double,
    lng: Double,
    mapType: MapType = MapType.NORMAL,
    modifier: Modifier = Modifier
)
