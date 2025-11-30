package love.bside.app.domain.usecase

import love.bside.app.core.AppException
import love.bside.app.core.Result
import love.bside.app.domain.models.AuthDetails
import love.bside.app.domain.models.Profile
import love.bside.app.domain.models.SeekingStatus
import love.bside.app.domain.models.SignUpData
import love.bside.app.domain.repository.AuthRepository
import love.bside.app.integration.runTest
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class SignUpUseCaseTest {

    private val fakeRepository = FakeAuthRepository()
    private val useCase = SignUpUseCase(fakeRepository)

    @Test
    fun `use case forwards sign up payload to repository`() = runTest {
        val data = sampleSignUp()

        useCase(data)

        assertEquals(data, fakeRepository.lastSignUpRequest)
    }

    @Test
    fun `use case returns underlying success result`() = runTest {
        val expected = Result.Success(sampleAuthDetails(tokenSuffix = "success"))
        fakeRepository.nextResult = expected

        val actual = useCase(sampleSignUp())

        assertSame(expected, actual)
    }

    @Test
    fun `use case propagates error result`() = runTest {
        val error = AppException.Auth.InvalidCredentials()
        val expected = Result.Error(error)
        fakeRepository.nextResult = expected

        val actual = useCase(sampleSignUp())

        assertSame(expected, actual)
    }

    private fun sampleSignUp() = SignUpData(
        email = "test@example.com",
        password = "Password123",
        passwordConfirm = "Password123",
        firstName = "Test",
        lastName = "User",
        birthDate = LocalDate(1990, 1, 1),
        seeking = SeekingStatus.BOTH
    )

    private fun sampleAuthDetails(tokenSuffix: String = "token") = AuthDetails(
        token = "auth_$tokenSuffix",
        profile = Profile(
            id = "profile",
            created = Instant.fromEpochMilliseconds(0),
            updated = Instant.fromEpochMilliseconds(0),
            userId = "user",
            firstName = "Test",
            lastName = "User",
            birthDate = LocalDate(1990, 1, 1),
            bio = null,
            location = null,
            seeking = SeekingStatus.BOTH
        )
    )

    private class FakeAuthRepository : AuthRepository {
        var lastSignUpRequest: SignUpData? = null
        var nextResult: Result<AuthDetails> = Result.Success(
            AuthDetails(
                token = "default",
                profile = Profile(
                    id = "profile",
                    created = Instant.fromEpochMilliseconds(0),
                    updated = Instant.fromEpochMilliseconds(0),
                    userId = "user",
                    firstName = "Test",
                    lastName = "User",
                    birthDate = LocalDate(1990, 1, 1),
                    bio = null,
                    location = null,
                    seeking = SeekingStatus.BOTH
                )
            )
        )

        override suspend fun login(email: String, password: String): Result<AuthDetails> {
            throw UnsupportedOperationException("Not needed for this test")
        }

        override suspend fun signUp(data: SignUpData): Result<AuthDetails> {
            lastSignUpRequest = data
            return nextResult
        }

        override suspend fun logout() {}
    }
}
