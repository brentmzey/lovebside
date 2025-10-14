package love.bside.server.config

import io.ktor.server.application.*
import love.bside.app.data.network.PocketBaseClient
import love.bside.server.repositories.*
import love.bside.server.services.*
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

/**
 * Configure dependency injection with Koin
 */
fun Application.configureDependencyInjection(config: ServerConfig) {
    install(Koin) {
        slf4jLogger()
        modules(serverModule(config))
    }
}

/**
 * Server dependency injection module
 */
fun serverModule(config: ServerConfig) = module {
    // Configuration
    single { config }
    
    // PocketBase Client (reused from shared module)
    single {
        PocketBaseClient(
            baseUrl = config.pocketbase.baseUrl,
            timeout = config.pocketbase.timeout
        )
    }
    
    // Repositories
    single<UserRepository> { UserRepositoryImpl(get()) }
    single<ProfileRepository> { ProfileRepositoryImpl(get()) }
    single<ValuesRepository> { ValuesRepositoryImpl(get()) }
    single<MatchRepository> { MatchRepositoryImpl(get()) }
    single<PromptRepository> { PromptRepositoryImpl(get()) }
    
    // Services
    single { AuthService(get(), get(), get()) }
    single { UserService(get(), get()) }
    single { ValuesService(get()) }
    single { MatchingService(get(), get(), get()) }
    single { PromptService(get()) }
}
