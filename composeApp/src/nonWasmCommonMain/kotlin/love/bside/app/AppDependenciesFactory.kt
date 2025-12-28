package love.bside.app

import love.bside.app.domain.usecase.GetDiscoveryUsersUseCase
import love.bside.app.domain.usecase.LoginUseCase
import love.bside.app.domain.usecase.LogoutUseCase
import love.bside.app.domain.usecase.SignUpUseCase
import love.bside.app.security.usecase.BiometricLoginUseCase
import love.bside.app.security.usecase.EnableBiometricLoginUseCase
import love.bside.app.security.usecase.ObserveSecureEnrollmentsUseCase
import org.koin.core.Koin

fun buildAppDependencies(koin: Koin): AppDependencies = AppDependencies(
    loginUseCase = koin.get<LoginUseCase>(),
    signUpUseCase = koin.get<SignUpUseCase>(),
    logoutUseCase = koin.get<LogoutUseCase>(),
    biometricLoginUseCase = koin.get<BiometricLoginUseCase>(),
    enableBiometricLoginUseCase = koin.get<EnableBiometricLoginUseCase>(),
    observeSecureEnrollmentsUseCase = koin.get<ObserveSecureEnrollmentsUseCase>(),
    getDiscoveryUsersUseCase = koin.get<GetDiscoveryUsersUseCase>()
)
