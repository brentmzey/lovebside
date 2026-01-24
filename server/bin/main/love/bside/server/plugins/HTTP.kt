package love.bside.server.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.cors.routing.*
import love.bside.server.config.ServerConfig

/**
 * Configure HTTP settings including CORS
 */
fun Application.configureHTTP(config: ServerConfig) {
    install(CORS) {
        // Allow configured origins
        config.server.allowedOrigins.forEach { origin ->
            allowHost(origin.removePrefix("http://").removePrefix("https://"), schemes = listOf("http", "https"))
        }
        
        // Allow common HTTP methods
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Patch)
        
        // Allow common headers
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Accept)
        allowHeader("X-Requested-With")
        
        // Allow credentials (cookies, auth headers)
        allowCredentials = true
        
        // Cache preflight response for 1 day
        maxAgeInSeconds = 86400
    }
}
