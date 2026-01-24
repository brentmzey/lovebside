package love.bside.server.routes.api.v1

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import love.bside.server.models.api.UpdateProfileRequest
import love.bside.server.plugins.getUserId
import love.bside.server.services.UserService
import love.bside.server.utils.toSuccessResponse
import org.koin.ktor.ext.inject

/**
 * User routes (protected)
 */
fun Route.userRoutes() {
    val userService by inject<UserService>()
    
    authenticate("auth-jwt") {
        route("/users") {
            // Get current user
            get("/me") {
                val principal = call.principal<JWTPrincipal>()!!
                val userId = principal.getUserId()
                
                val user = userService.getCurrentUser(userId)
                call.respond(HttpStatusCode.OK, user.toSuccessResponse())
            }
            
            // Update current user's profile
            put("/me") {
                val principal = call.principal<JWTPrincipal>()!!
                val userId = principal.getUserId()
                val request = call.receive<UpdateProfileRequest>()
                
                val profile = userService.updateProfile(userId, request)
                call.respond(HttpStatusCode.OK, profile.toSuccessResponse())
            }
            
            // Delete current user
            delete("/me") {
                val principal = call.principal<JWTPrincipal>()!!
                val userId = principal.getUserId()
                
                userService.deleteUser(userId)
                call.respond(HttpStatusCode.OK, mapOf("message" to "User deleted successfully").toSuccessResponse())
            }
        }
    }
}
