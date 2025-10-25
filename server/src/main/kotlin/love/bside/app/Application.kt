package love.bside.app

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import love.bside.server.config.*
import love.bside.server.plugins.*
import love.bside.server.plugins.configureRouting

// Server port configuration
const val SERVER_PORT = 8080

fun main() {
    embeddedServer(
        Netty,
        port = SERVER_PORT,
        host = "0.0.0.0",
        module = Application::module
    ).start(wait = true)
}

fun Application.module() {
    // Load configuration
    val config = ServerConfig.load(this)
    
    // Configure plugins
    configureSerialization()
    configureHTTP(config)
    configureMonitoring()
    configureSecurity(config)
    configureStatusPages()
    configureDependencyInjection(config)
    
    // Configure routing
    configureRouting()
}
