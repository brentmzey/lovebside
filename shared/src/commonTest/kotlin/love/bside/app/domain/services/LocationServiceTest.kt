package love.bside.app.domain.services

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

class LocationServiceTest {

    @Test
    fun `StubLocationService returns valid location`() = runTest {
        val service = StubLocationService()
        val result = service.getCurrentLocation()
        
        assertTrue(result.isRight(), "Expected Right(LocationModel)")
        result.onRight { loc ->
            assertTrue(loc.id.isNotEmpty())
            assertTrue(loc.coordinate.lat != 0.0)
        }
    }

    @Test
    fun `StubLocationService flow emits valid location`() = runTest {
        val service = StubLocationService()
        val result = service.observeLocation().first()
        
        assertTrue(result.isRight())
        result.onRight { loc ->
             assertTrue(loc.id.isNotEmpty())
        }
    }
}
