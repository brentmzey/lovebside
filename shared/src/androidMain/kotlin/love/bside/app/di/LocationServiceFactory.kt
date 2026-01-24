package love.bside.app.di

import android.content.Context
import love.bside.app.domain.services.AndroidLocationService
import love.bside.app.domain.services.LocationService
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

actual fun createLocationService(): LocationService {
    return object : KoinComponent {
        val context: Context by inject()
    }.let { AndroidLocationService(it.context) }
}
