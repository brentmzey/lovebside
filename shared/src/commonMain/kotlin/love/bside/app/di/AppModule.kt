package love.bside.app.di

import com.russhwolf.settings.Settings
import love.bside.app.core.AppConfig
import love.bside.app.data.api.InternalApiClient
// import love.bside.app.data.repository.ApiAuthRepository
// import love.bside.app.data.repository.ApiMatchRepository
// import love.bside.app.data.repository.ApiProfileRepository
// import love.bside.app.data.repository.ApiQuestionnaireRepository
// import love.bside.app.data.repository.ApiValuesRepository
import love.bside.app.data.storage.SessionManager
import love.bside.app.data.storage.SessionManagerImpl
import love.bside.app.data.storage.TokenStorage
import love.bside.app.data.storage.TokenStorageImpl
import io.ktor.client.HttpClient
import love.bside.app.data.api.createHttpClient
import love.bside.app.data.api.PocketBaseClient
import love.bside.app.data.repository.PocketBaseAuthRepository
import love.bside.app.data.repository.PocketBaseProfileRepository
import love.bside.app.AppConstants
import love.bside.app.domain.repository.AuthRepository
import love.bside.app.domain.repository.MatchRepository
import love.bside.app.domain.repository.ProfileRepository
import love.bside.app.domain.repository.QuestionnaireRepository
import love.bside.app.domain.repository.ValuesRepository
import love.bside.app.domain.usecase.GetUserProfileUseCase
import love.bside.app.domain.usecase.LoginUseCase
import love.bside.app.domain.usecase.LogoutUseCase
import love.bside.app.domain.usecase.SignUpUseCase
import love.bside.app.domain.usecase.GetDiscoveryUsersUseCase
import love.bside.app.domain.repository.MessagingRepository
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

import love.bside.app.domain.services.LocationService
import love.bside.app.domain.services.StubLocationService

/**
 * Main DI module configuration for the application  
 * Pass Settings instance during Koin initialization
 * 
 * IMPORTANT: This module now uses API-based repositories that communicate
 * with our internal API instead of PocketBase directly!
 * 
 * ORCHESTRATION: Now includes professional orchestration layer with
 * event bus, job scheduling, sync, and health monitoring.
 */
fun appModule(settings: Settings) = module {
    // Core
    single { AppConfig() }
    
    // Settings
    single { settings }
    
    // Token Storage
    single<TokenStorage> { TokenStorageImpl(get()) }
    
    // Session Manager (for backward compatibility)
    single<SessionManager> { SessionManagerImpl(get()) }
    
    // HTTP Client (Shared)
    single { createHttpClient(get()) }
    single { PocketBaseAuthRepository(get(), get(), get()) }
    single { love.bside.app.data.repository.MessagingRepository(get()) }
    // PocketBase Client (SDK)
    single { PocketBaseClient(get(), "${love.bside.app.AppConstants.POCKETBASE_URL}/api/") }

    // Internal API Client - The ONLY way clients communicate with backend
    single { InternalApiClient(get()) }

    // Official PocketBase SDK (Required by some legacy repositories)
    single { io.pocketbase.PocketBase("${love.bside.app.AppConstants.POCKETBASE_URL}/", httpClient = get<io.ktor.client.HttpClient>()) }
    
    // API-based Repositories - All use InternalApiClient
    // Updated to use PocketBaseClient for Auth
    singleOf(::PocketBaseAuthRepository) bind AuthRepository::class
    
    // Repositories using Official SDK
    single<ProfileRepository> { PocketBaseProfileRepository(get()) }
    // MessagingRepository is already defined above, removing legacy or duplicate lines if any
    // single<MessagingRepository> { love.bside.app.data.repository.PocketBaseMessagingRepository(get()) }
    
    // singleOf(::ApiProfileRepository) bind ProfileRepository::class
    // singleOf(::ApiMatchRepository) bind MatchRepository::class
    // singleOf(::ApiQuestionnaireRepository) bind QuestionnaireRepository::class
    // singleOf(::ApiValuesRepository) bind ValuesRepository::class
    
    // Location - Platform-specific implementations
    single<LocationService> { createLocationService() }
    
    // Use Cases
    factoryOf(::LoginUseCase)
    factoryOf(::SignUpUseCase)
    factoryOf(::LogoutUseCase)
    factoryOf(::GetUserProfileUseCase)
    factoryOf(::GetDiscoveryUsersUseCase)
}

