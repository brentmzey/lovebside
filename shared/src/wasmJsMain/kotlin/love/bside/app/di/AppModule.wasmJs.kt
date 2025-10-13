package love.bside.app.di

import com.russhwolf.settings.Settings
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.URLProtocol
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import love.bside.app.core.appConfig
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
import kotlin.reflect.KClass

/**
 * Manual dependency injection for WasmJS (Koin doesn't support WasmJS yet)
 */
class ManualDIModule : DIModule {
    private val instances = mutableMapOf<KClass<*>, Any>()
    
    private val settings by lazy { Settings() }
    private val tokenStorage by lazy { TokenStorageImpl(settings) }
    private val sessionManager by lazy { SessionManagerImpl(settings) }
    
    private val httpClient by lazy {
        val config = appConfig()
        HttpClient {
            install(ContentNegotiation) {
                json(Json {
                    prettyPrint = true
                    isLenient = true
                    ignoreUnknownKeys = true
                    encodeDefaults = true
                })
            }

            install(HttpTimeout) {
                requestTimeoutMillis = config.apiTimeout
                connectTimeoutMillis = config.apiTimeout / 2
                socketTimeoutMillis = config.apiTimeout
            }

            defaultRequest {
                url {
                    protocol = URLProtocol.HTTPS
                    host = "bside.pockethost.io"
                    pathSegments = listOf("api", "")
                }
            }
        }
    }
    
    private val pocketBaseClient by lazy { PocketBaseClient(httpClient) }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> get(clazz: KClass<T>): T {
        return instances.getOrPut(clazz) {
            when (clazz) {
                Settings::class -> settings
                TokenStorage::class -> tokenStorage
                SessionManager::class -> sessionManager
                HttpClient::class -> httpClient
                PocketBaseClient::class -> pocketBaseClient
                AuthRepository::class -> PocketBaseAuthRepository(httpClient, tokenStorage, sessionManager)
                ProfileRepository::class -> PocketBaseProfileRepository(httpClient)
                QuestionnaireRepository::class -> PocketBaseQuestionnaireRepository(httpClient)
                ValuesRepository::class -> PocketBaseValuesRepository(pocketBaseClient)
                MatchRepository::class -> PocketBaseMatchRepository(httpClient)
                GetUserProfileUseCase::class -> GetUserProfileUseCase(get(ProfileRepository::class))
                LoginUseCase::class -> LoginUseCase(get(AuthRepository::class))
                SignUpUseCase::class -> SignUpUseCase(get(AuthRepository::class))
                LogoutUseCase::class -> LogoutUseCase(get(AuthRepository::class))
                ProfileViewModel::class -> ProfileViewModel(get(GetUserProfileUseCase::class), get(LogoutUseCase::class))
                AuthViewModel::class -> AuthViewModel(get(LoginUseCase::class), get(SignUpUseCase::class))
                QuestionnaireViewModel::class -> QuestionnaireViewModel(get(QuestionnaireRepository::class))
                ValuesViewModel::class -> ValuesViewModel(get(ValuesRepository::class))
                MatchViewModel::class -> MatchViewModel(get(MatchRepository::class))
                else -> error("No binding found for ${clazz.simpleName}")
            }
        } as T
    }

    override fun <T : Any> inject(clazz: KClass<T>): Lazy<T> {
        return lazy { get(clazz) }
    }
}

private val diInstance = ManualDIModule()

actual fun initializeDI() {
    // Nothing to initialize for manual DI
}

actual fun getDI(): DIModule = diInstance
