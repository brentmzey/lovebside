package love.bside.app.domain.models

import arrow.core.Some
import love.bside.app.domain.models.GeoCoordinate
import love.bside.app.domain.models.LocationModel
import love.bside.app.domain.models.LocationSource
import kotlinx.datetime.Clock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LocationModelsTest {

    @Test
    fun `addressOption returns Some when address is present`() {
        val model = LocationModel(
            id = "123",
            coordinate = GeoCoordinate(0.0, 0.0),
            _address = "123 Main St",
            source = LocationSource.UserSelected,
            recordedAt = Clock.System.now()
        )

        assertTrue(model.address is Some)
        assertEquals("123 Main St", model.address.getOrNull())
    }

    @Test
    fun `addressOption returns None when address is null`() {
        val model = LocationModel(
            id = "123",
            coordinate = GeoCoordinate(0.0, 0.0),
            _address = null,
            source = LocationSource.UserSelected,
            recordedAt = Clock.System.now()
        )

        assertTrue(model.address.isNone())
        assertNull(model.address.getOrNull())
    }
}
