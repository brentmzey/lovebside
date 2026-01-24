package love.bside.app.di

import love.bside.app.domain.services.JsLocationService
import love.bside.app.domain.services.LocationService

actual fun createLocationService(): LocationService {
    return JsLocationService()
}
