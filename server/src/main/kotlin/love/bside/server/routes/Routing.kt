package love.bside.server.routes

import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.response.*
import love.bside.server.models.api.HealthResponse
import love.bside.server.routes.api.v1.authRoutes
import love.bside.server.routes.api.v1.userRoutes
import love.bside.server.routes.api.v1.valuesRoutes
import love.bside.server.routes.api.v1.matchRoutes
import love.bside.server.routes.api.v1.promptRoutes
import kotlinx.datetime.Clock

/**
 * Configure all application routes
 */
fun Application.configureRouting() {
    routing {
        // Health check endpoint
        get("/health") {
            call.respond(HealthResponse(
                status = "healthy",
                version = "1.0.0",
                timestamp = Clock.System.now().toString()
            ))
        }
        
        // API v1 routes
        route("/api/v1") {
            authRoutes()
            userRoutes()
            valuesRoutes()
            matchRoutes()
            promptRoutes()
        }
    }
}
