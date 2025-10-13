package love.bside.app.di

import com.russhwolf.settings.Settings
import love.bside.app.data.api.createHttpClient
import love.bside.app.data.api.PocketBaseClient
import love.bside.app.data.repository.PocketBaseAuthRepository
import love.bside.app.data.repository.PocketBaseMatchRepository
import love.bside.app.data.repository.PocketBaseProfileRepository
import love.bside.app.data.repository.PocketBaseQuestionnaireRepository
import love.bside.app.data.repository.PocketBaseValuesRepository
import love.bside.app.data.storage.SessionManager
import love.bside.app.data.storage.SessionManagerImpl
import love.bside.app.data.storage.TokenStorage
import love.bside.app.data.storage.TokenStorageImpl
import love.bside.app.domain.repository.AuthRepository
import love.bside.app.domain.repository.MatchRepository
import love.bside.app.domain.repository.ProfileRepository
import love.bside.app.domain.repository.QuestionnaireRepository
import love.bside.app.domain.repository.ValuesRepository
import love.bside.app.domain.usecase.GetUserProfileUseCase
import love.bside.app.domain.usecase.LoginUseCase
import love.bside.app.domain.usecase.LogoutUseCase
import love.bside.app.domain.usecase.SignUpUseCase
import love.bside.app.presentation.AuthViewModel
import love.bside.app.presentation.MatchViewModel
import love.bside.app.presentation.ProfileViewModel
import love.bside.app.presentation.QuestionnaireViewModel
import love.bside.app.presentation.ValuesViewModel
import org.koin.core.Koin
import org.koin.core.context.startKoin
import org.koin.dsl.module
import kotlin.reflect.KClass

val appModule = module {
    single { Settings() }
    single<TokenStorage> { TokenStorageImpl(get<Settings>()) }
    single<SessionManager> { SessionManagerImpl(get<Settings>()) }
    single { createHttpClient(get<TokenStorage>()) }
    single { PocketBaseClient(get()) }

    single<AuthRepository> { PocketBaseAuthRepository(get(), get(), get()) }
    single<ProfileRepository> { PocketBaseProfileRepository(get()) }
    single<QuestionnaireRepository> { PocketBaseQuestionnaireRepository(get()) }
    single<ValuesRepository> { PocketBaseValuesRepository(get()) }
    single<MatchRepository> { PocketBaseMatchRepository(get()) }

    factory { GetUserProfileUseCase(get()) }
    factory { LoginUseCase(get()) }
    factory { SignUpUseCase(get()) }
    factory { LogoutUseCase(get()) }
    factory { ProfileViewModel(get(), get()) }
    factory { AuthViewModel(get(), get()) }
    factory { QuestionnaireViewModel(get()) }
    factory { ValuesViewModel(get()) }
    factory { MatchViewModel(get()) }
}

private lateinit var koinInstance: Koin

actual fun initializeDI() {
    koinInstance = startKoin {
        modules(appModule)
    }.koin
}

class KoinDIModule : DIModule {
    override fun <T : Any> get(clazz: KClass<T>): T {
        return koinInstance.get(clazz, null, null)
    }

    override fun <T : Any> inject(clazz: KClass<T>): Lazy<T> {
        return lazy { get(clazz) }
    }
}

actual fun getDI(): DIModule = KoinDIModule()
