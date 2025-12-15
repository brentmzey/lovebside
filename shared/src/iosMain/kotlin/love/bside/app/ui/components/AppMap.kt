package love.bside.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import platform.MapKit.MKMapView
import platform.MapKit.MKCoordinateRegionMakeWithDistance
import platform.CoreLocation.CLLocationCoordinate2DMake
import platform.MapKit.MKMapTypeStandard
import platform.MapKit.MKMapTypeSatellite
import platform.MapKit.MKMapTypeHybrid
import love.bside.app.domain.models.MapType

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun AppMap(
    lat: Double,
    lng: Double,
    mapType: MapType,
    modifier: Modifier
) {
    UIKitView(
        factory = {
            MKMapView().apply {
                setMapType(
                    when(mapType) {
                        MapType.NORMAL -> MKMapTypeStandard
                        MapType.SATELLITE -> MKMapTypeSatellite
                        MapType.HYBRID -> MKMapTypeHybrid
                        MapType.TERRAIN -> MKMapTypeHybrid // MapKit doesn't have explicit terrain, hybrid is closest
                    }
                )
            }
        },
        update = { mapView ->
            val coordinate = CLLocationCoordinate2DMake(lat, lng)
            val region = MKCoordinateRegionMakeWithDistance(coordinate, 1000.0, 1000.0)
            mapView.setRegion(region, animated = true)
        },
        modifier = modifier
    )
}
