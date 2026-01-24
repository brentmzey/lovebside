package love.bside.app.di

import love.bside.app.domain.services.LocationService

/**
 * Platform-specific factory for creating LocationService instances.
 */
expect fun createLocationService(): LocationService
