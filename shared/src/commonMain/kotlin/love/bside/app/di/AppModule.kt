package love.bside.app.di

import com.russhwolf.settings.Settings
import io.ktor.client.HttpClient
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import love.bside.app.core.AppConfig
import love.bside.app.core.AppLogger
import love.bside.app.data.api.PocketBaseClient
import love.bside.app.data.repository.PocketBaseAuthRepository
import love.bside.app.data.repository.PocketBaseMatchRepository
import love.bside.app.data.repository.PocketBaseProfileRepository
import love.bside.app.data.repository.PocketBaseQuestionnaireRepository
import love.bside.app.data.repository.PocketBaseValuesRepository
import love.bside.app.data.storage.SessionManager
import love.bside.app.data.storage.SessionManagerImpl
import love.bside.app.domain.repository.AuthRepository
import love.bside.app.domain.repository.MatchRepository
import love.bside.app.domain.repository.ProfileRepository
import love.bside.app.domain.repository.QuestionnaireRepository
import love.bside.app.domain.repository.ValuesRepository
import love.bside.app.domain.usecase.GetUserProfileUseCase
import love.bside.app.domain.usecase.LoginUseCase
import love.bside.app.domain.usecase.LogoutUseCase
import love.bside.app.domain.usecase.SignUpUseCase
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

/**
 * Main DI module configuration for the application  
 * Pass Settings instance during Koin initialization
 */
fun appModule(settings: Settings) = module {
    // Core
    single { AppConfig() }
    
    // Settings
    single { settings }
    
    // Session Manager
    single<SessionManager> { SessionManagerImpl(get()) }
    
    // HTTP Client with Bearer auth
    single {
        HttpClient {
            install(ContentNegotiation) {
                json(Json {
                    prettyPrint = true
                    isLenient = true
                    ignoreUnknownKeys = true
                })
            }
            
            install(Logging) {
                logger = object : Logger {
                    override fun log(message: String) {
                        AppLogger.debug("HttpClient", message)
                    }
                }
                level = LogLevel.INFO
            }
            
            install(Auth) {
                bearer {
                    loadTokens {
                        val sessionManager = get<SessionManager>()
                        val token = sessionManager.getToken()
                        token?.let {
                            BearerTokens(accessToken = it, refreshToken = "")
                        }
                    }
                }
            }
            
            defaultRequest {
                url(get<AppConfig>().pocketBaseUrl)
            }
        }
    }
    
    // PocketBase Client
    single { PocketBaseClient(get(), get<AppConfig>().pocketBaseUrl) }
    
    // Repositories
    singleOf(::PocketBaseAuthRepository) bind AuthRepository::class
    singleOf(::PocketBaseProfileRepository) bind ProfileRepository::class
    singleOf(::PocketBaseMatchRepository) bind MatchRepository::class
    singleOf(::PocketBaseQuestionnaireRepository) bind QuestionnaireRepository::class
    singleOf(::PocketBaseValuesRepository) bind ValuesRepository::class
    
    // Use Cases
    factoryOf(::LoginUseCase)
    factoryOf(::SignUpUseCase)
    factoryOf(::LogoutUseCase)
    factoryOf(::GetUserProfileUseCase)
}
