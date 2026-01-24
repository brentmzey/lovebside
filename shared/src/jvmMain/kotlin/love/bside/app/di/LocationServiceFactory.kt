package love.bside.app.di

import io.ktor.client.*
import love.bside.app.domain.services.JvmLocationService
import love.bside.app.domain.services.LocationService
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

actual fun createLocationService(): LocationService {
    return object : KoinComponent {
        val httpClient: HttpClient by inject()
    }.let { JvmLocationService(it.httpClient) }
}
