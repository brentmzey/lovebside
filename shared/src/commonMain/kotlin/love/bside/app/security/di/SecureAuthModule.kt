package love.bside.app.security.di

import love.bside.app.security.BiometricPromptBridge
import love.bside.app.security.PasskeyBridge
import love.bside.app.security.SecureAuthManager
import love.bside.app.security.SecureCredentialStore
import love.bside.app.security.usecase.BiometricLoginUseCase
import love.bside.app.security.usecase.EnableBiometricLoginUseCase
import love.bside.app.security.usecase.ObserveSecureEnrollmentsUseCase
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

fun secureAuthModule(
    credentialStore: SecureCredentialStore,
    biometricPromptBridge: BiometricPromptBridge,
    passkeyBridge: PasskeyBridge
) = module {
    single<SecureCredentialStore> { credentialStore }
    single<BiometricPromptBridge> { biometricPromptBridge }
    single<PasskeyBridge> { passkeyBridge }
    single { SecureAuthManager(get(), get(), get(), get(), get()) }
    factoryOf(::EnableBiometricLoginUseCase)
    factoryOf(::BiometricLoginUseCase)
    factoryOf(::ObserveSecureEnrollmentsUseCase)
}
