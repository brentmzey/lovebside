package love.bside.app

import love.bside.app.domain.usecase.LoginUseCase
import love.bside.app.domain.usecase.LogoutUseCase
import love.bside.app.domain.usecase.SignUpUseCase
import org.koin.core.Koin

fun buildAppDependencies(koin: Koin): AppDependencies = AppDependencies(
    loginUseCase = koin.get<LoginUseCase>(),
    signUpUseCase = koin.get<SignUpUseCase>(),
    logoutUseCase = koin.get<LogoutUseCase>()
)
