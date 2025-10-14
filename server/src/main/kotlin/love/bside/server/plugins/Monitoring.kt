package love.bside.server.plugins

import io.ktor.server.application.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.request.*
import org.slf4j.event.Level

/**
 * Configure request/response logging and monitoring
 */
fun Application.configureMonitoring() {
    install(CallLogging) {
        level = Level.INFO
        
        // Log request details
        format { call ->
            val status = call.response.status()
            val httpMethod = call.request.httpMethod.value
            val uri = call.request.uri
            val userAgent = call.request.headers["User-Agent"]
            
            "Status: $status, HTTP method: $httpMethod, URI: $uri, User-Agent: $userAgent"
        }
        
        // Filter out health checks from logs to reduce noise
        filter { call ->
            !call.request.path().startsWith("/health")
        }
    }
}
