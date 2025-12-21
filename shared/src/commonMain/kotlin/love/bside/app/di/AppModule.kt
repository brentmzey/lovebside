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
import love.bside.app.data.api.createHttpClient
import love.bside.app.data.api.PocketBaseClient
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

    // PocketBase Client (SDK)
    single { PocketBaseClient(get(), "${love.bside.app.AppConstants.POCKETBASE_URL}/api/") }

    // Internal API Client - The ONLY way clients communicate with backend
    single { InternalApiClient(get()) }
    
    // API-based Repositories - All use InternalApiClient
    // Updated to use PocketBaseClient for Auth
    // singleOf(::ApiAuthRepository) bind AuthRepository::class
    // singleOf(::ApiProfileRepository) bind ProfileRepository::class
    // singleOf(::ApiMatchRepository) bind MatchRepository::class
    // singleOf(::ApiQuestionnaireRepository) bind QuestionnaireRepository::class
    // singleOf(::ApiValuesRepository) bind ValuesRepository::class
    
    // Location
    singleOf(::StubLocationService) bind LocationService::class
    
    // Use Cases
    factoryOf(::LoginUseCase)
    factoryOf(::SignUpUseCase)
    factoryOf(::LogoutUseCase)
    factoryOf(::GetUserProfileUseCase)
}
