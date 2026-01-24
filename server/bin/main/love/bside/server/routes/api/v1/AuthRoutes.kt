package love.bside.server.routes.api.v1

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import love.bside.server.models.api.*
import love.bside.server.services.AuthService
import love.bside.server.utils.toSuccessResponse
import org.koin.ktor.ext.inject

/**
 * Authentication routes
 */
fun Route.authRoutes() {
    val authService by inject<AuthService>()
    
    route("/auth") {
        // Login
        post("/login") {
            val request = call.receive<LoginRequest>()
            val response = authService.login(request)
            call.respond(HttpStatusCode.OK, response.toSuccessResponse())
        }
        
        // Register
        post("/register") {
            val request = call.receive<RegisterRequest>()
            val response = authService.register(request)
            call.respond(HttpStatusCode.Created, response.toSuccessResponse())
        }
        
        // Refresh token
        post("/refresh") {
            val request = call.receive<RefreshTokenRequest>()
            val response = authService.refreshToken(request.refreshToken)
            call.respond(HttpStatusCode.OK, response.toSuccessResponse())
        }
        
        // Logout (client-side only, invalidate JWT)
        post("/logout") {
            // JWT is stateless, so logout is handled client-side
            call.respond(HttpStatusCode.OK, mapOf("message" to "Logged out successfully").toSuccessResponse())
        }
    }
}
