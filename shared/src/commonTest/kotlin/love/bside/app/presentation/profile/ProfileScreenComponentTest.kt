package love.bside.app.presentation.profile

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import love.bside.app.data.storage.SessionManager
import love.bside.app.domain.models.Profile
import love.bside.app.domain.usecase.GetUserProfileUseCase
import love.bside.app.domain.usecase.LogoutUseCase
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@ExperimentalCoroutinesApi
class ProfileScreenComponentTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var getUserProfileUseCase: GetUserProfileUseCase
    private lateinit var logoutUseCase: LogoutUseCase
    private lateinit var sessionManager: SessionManager
    private lateinit var component: DefaultProfileScreenComponent

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        getUserProfileUseCase = mockk()
        logoutUseCase = mockk()
        sessionManager = mockk()
        component = DefaultProfileScreenComponent(
            componentContext = DefaultComponentContext(LifecycleRegistry()),
            onLogout = {},
            onNavigateToQuestionnaire = {},
            onNavigateToValues = {},
            onNavigateToMatches = {}
        )
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test profile is loaded on init`() = runTest {
        val profile = Profile(
            id = "1",
            userId = "user1",
            firstName = "John",
            lastName = "Doe",
            created = kotlinx.datetime.Clock.System.now(),
            updated = kotlinx.datetime.Clock.System.now(),
            birthDate = kotlinx.datetime.LocalDate(1990, 1, 1),
            seeking = love.bside.app.domain.models.SeekingStatus.BOTH
        )
        coEvery { sessionManager.getProfile() } returns profile

        component = DefaultProfileScreenComponent(
            componentContext = DefaultComponentContext(LifecycleRegistry()),
            onLogout = {},
            onNavigateToQuestionnaire = {},
            onNavigateToValues = {},
            onNavigateToMatches = {}
        )

        assertEquals(profile, component.uiState.value.data)
    }
}
