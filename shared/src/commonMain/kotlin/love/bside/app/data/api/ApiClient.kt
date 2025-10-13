package love.bside.app.data.api

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.URLProtocol
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import love.bside.app.core.appConfig
import love.bside.app.core.logDebug
import love.bside.app.data.storage.TokenStorage

/**
 * Creates a configured HTTP client with authentication, logging, and timeout support
 */
fun createHttpClient(tokenStorage: TokenStorage) = HttpClient {
    val config = appConfig()

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

    if (config.enableLogging) {
        install(Logging) {
            logger = object : Logger {
                override fun log(message: String) {
                    this.logDebug(message)
                }
            }
            level = when (config.environment) {
                love.bside.app.core.Environment.DEVELOPMENT -> LogLevel.ALL
                love.bside.app.core.Environment.STAGING -> LogLevel.HEADERS
                love.bside.app.core.Environment.PRODUCTION -> LogLevel.NONE
            }
        }
    }

    install(Auth) {
        bearer {
            loadTokens {
                val token = tokenStorage.getToken()
                if (token != null) {
                    BearerTokens(token, "")
                } else {
                    null
                }
            }
        }
    }

    defaultRequest {
        url("https://bside.pockethost.io/api/")
    }
}
