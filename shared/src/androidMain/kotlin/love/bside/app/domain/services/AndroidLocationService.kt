package love.bside.app.domain.services

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import kotlinx.datetime.Clock
import love.bside.app.domain.models.GeoCoordinate
import love.bside.app.domain.models.LocationModel
import love.bside.app.domain.models.LocationSource

/**
 * Android implementation of LocationService using Fused Location Provider.
 */
class AndroidLocationService(private val context: Context) : LocationService {
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    override suspend fun getCurrentLocation(): Either<LocationError, LocationModel> {
        // Check permissions
        if (!hasLocationPermission()) {
            return LocationError.PermissionDenied.left()
        }

        // Check if GPS is enabled
        if (!isGpsEnabled()) {
            return LocationError.GpsDisabled.left()
        }

        return try {
            val cancellationTokenSource = CancellationTokenSource()
            val location: Location? = fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                cancellationTokenSource.token
            ).await()

            if (location != null) {
                LocationModel(
                    id = "android_${System.currentTimeMillis()}",
                    coordinate = GeoCoordinate(location.latitude, location.longitude),
                    _address = null, // Reverse geocoding can be added later
                    source = LocationSource.Gps,
                    recordedAt = Clock.System.now()
                ).right()
            } else {
                LocationError.Unknown.left()
            }
        } catch (e: Exception) {
            LocationError.NetworkError(e.message ?: "Unknown error").left()
        }
    }

    override fun observeLocation(): Flow<Either<LocationError, LocationModel>> = flow {
        // For now, just emit current location
        // Can be enhanced with LocationRequest for continuous updates
        emit(getCurrentLocation())
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }

    private fun isGpsEnabled(): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }
}
