package love.bside.server.config

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import kotlinx.serialization.json.Json
import love.bside.app.data.api.PocketBaseClient
import love.bside.server.repositories.*
import love.bside.server.services.*
import love.bside.server.matching.*
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin

/**
 * Configure dependency injection with Koin
 */
fun Application.configureDependencyInjection(config: ServerConfig) {
    install(Koin) {
        // Removed slf4jLogger() - not available in Koin 3.5.6
        modules(serverModule(config))
    }
}

/**
 * Server dependency injection module
 */
fun serverModule(config: ServerConfig) = module {
    // Configuration
    single { config }
    
    // HTTP Client for PocketBase
    single {
        HttpClient(CIO) {
            install(ContentNegotiation) {
                json(Json {
                    prettyPrint = true
                    isLenient = true
                    ignoreUnknownKeys = true
                })
            }
        }
    }
    
    // PocketBase Client (reused from shared module)
    single {
        PocketBaseClient(
            client = get(),
            baseUrl = config.pocketbase.baseUrl
        )
    }
    
    // Repositories
    single<UserRepository> { UserRepositoryImpl(get()) }
    single<ProfileRepository> { ProfileRepositoryImpl(get()) }
    single<ValuesRepository> { ValuesRepositoryImpl(get()) }
    single<MatchRepository> { MatchRepositoryImpl(get()) }
    single<PromptRepository> { PromptRepositoryImpl(get()) }
    single<MessagingRepository> { MessagingRepository(get()) }
    
    // Matching Engine — Scorers
    single<List<ScoreDimension>> {
        listOf(
            ProustAffinityScorer(get()),
            GeolocationScorer(get()),
            InterestOverlapScorer(get()),
            SeekingCompatibilityScorer(get())
        )
    }
    single { MatchDiscoveryService(get(), get()) }
    
    // Services
    single { AuthService(get(), get(), get()) }
    single { UserService(get(), get()) }
    single { ValuesService(get()) }
    single { MatchingService(get<MatchRepository>(), get<UserRepository>(), get<ProfileRepository>(), get<MatchDiscoveryService>()) }
    single { PromptService(get()) }
    single { MessagingService(get()) }
}
