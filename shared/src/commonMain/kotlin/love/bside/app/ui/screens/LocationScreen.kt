package love.bside.app.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import love.bside.app.domain.models.LocationModel
import love.bside.app.domain.services.LocationService
import love.bside.app.ui.components.AppMap
import org.koin.compose.koinInject

@Composable
fun LocationScreen(
    onBack: () -> Unit
) {
    val locationService: LocationService = koinInject()
    var location by remember { mutableStateOf<LocationModel?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        locationService.getCurrentLocation().fold(
            ifLeft = { error = "Failed to get location: $it" },
            ifRight = { location = it }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            location != null -> {
                val loc = location!!
                AppMap(
                    lat = loc.coordinate.lat,
                    lng = loc.coordinate.lng,
                    modifier = Modifier.fillMaxSize()
                )
            }
            error != null -> {
                Text(text = error!!, modifier = Modifier.align(Alignment.Center))
            }
            else -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}
