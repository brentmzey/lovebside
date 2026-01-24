package love.bside.app.di

import love.bside.app.domain.services.IosLocationService
import love.bside.app.domain.services.LocationService

actual fun createLocationService(): LocationService {
    return IosLocationService()
}
