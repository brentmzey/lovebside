package love.bside.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType as GMapType
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import love.bside.app.domain.models.MapType

@Composable
actual fun AppMap(
    lat: Double,
    lng: Double,
    mapType: MapType,
    modifier: Modifier
) {
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(lat, lng), 15f)
    }
    
    val properties = MapProperties(
        mapType = when(mapType) {
            MapType.NORMAL -> GMapType.NORMAL
            MapType.SATELLITE -> GMapType.SATELLITE
            MapType.HYBRID -> GMapType.HYBRID
            MapType.TERRAIN -> GMapType.TERRAIN
        },
        isMyLocationEnabled = false // Requires permission check handled separately
    )

    GoogleMap(
        modifier = modifier,
        cameraPositionState = cameraPositionState,
        properties = properties
    )
}
