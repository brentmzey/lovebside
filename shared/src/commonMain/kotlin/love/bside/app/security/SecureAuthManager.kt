package love.bside.app.security

import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Clock
import love.bside.app.data.storage.SessionManager
import love.bside.app.data.storage.TokenStorage
import love.bside.app.domain.models.AuthDetails

class SecureAuthManager(
    private val credentialStore: SecureCredentialStore,
    private val tokenStorage: TokenStorage,
    private val sessionManager: SessionManager,
    private val biometricPromptBridge: BiometricPromptBridge,
    private val passkeyBridge: PasskeyBridge,
    private val clock: Clock = Clock.System
) {
    val enrollments: Flow<List<SecureEnrollmentRecord>> = credentialStore.updates()

    suspend fun canUseBiometric(): BiometricAvailability = biometricPromptBridge.canAuthenticate()

    suspend fun passkeyAvailability(): PasskeyAvailability = passkeyBridge.availability()

    suspend fun enrollBiometric(
        authDetails: AuthDetails,
        email: String,
        label: String? = null,
        factor: SecureAuthFactor.Biometric = SecureAuthFactor.Biometric()
    ): SecureEnrollmentResult {
        val existing = credentialStore.get(authDetails.profile.userId)
        val now = clock.now()
        val record = SecureEnrollmentRecord(
            userId = authDetails.profile.userId,
            email = email,
            factor = factor,
            authDetails = authDetails,
            createdAt = existing?.createdAt ?: now,
            updatedAt = now,
            label = label ?: existing?.label
        )
        credentialStore.upsert(record)
        return if (existing == null) SecureEnrollmentResult.Enrolled(record) else SecureEnrollmentResult.Updated(record)
    }

    suspend fun authenticateWithBiometric(
        prompt: SecurePromptText = SecurePromptText.default()
    ): SecureAuthResult {
        val record = credentialStore.getDefault() ?: return SecureAuthResult.NoEnrollment
        val factor = record.factor
        if (factor !is SecureAuthFactor.Biometric) {
            return SecureAuthResult.NoEnrollment
        }

        return when (val availability = biometricPromptBridge.canAuthenticate(factor)) {
            BiometricAvailability.Available -> when (val result = biometricPromptBridge.authenticate(prompt, factor)) {
                BiometricAuthResult.Success -> restoreSession(record.authDetails)
                BiometricAuthResult.Canceled -> SecureAuthResult.Canceled
                is BiometricAuthResult.Error -> SecureAuthResult.Error(result.message)
            }
            BiometricAvailability.NoHardware,
            BiometricAvailability.NotEnrolled,
            is BiometricAvailability.Unavailable -> SecureAuthResult.Unavailable(availability)
            BiometricAvailability.Unknown -> SecureAuthResult.Unavailable(availability)
        }
    }

    private fun restoreSession(details: AuthDetails): SecureAuthResult {
        tokenStorage.saveToken(details.token)
        sessionManager.saveSession(details.profile, details.token)
        return SecureAuthResult.Success(details)
    }
}
