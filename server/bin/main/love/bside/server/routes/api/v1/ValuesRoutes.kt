package love.bside.server.routes.api.v1

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import love.bside.server.models.api.SaveUserValuesRequest
import love.bside.server.plugins.getUserId
import love.bside.server.services.ValuesService
import love.bside.server.utils.toSuccessResponse
import org.koin.ktor.ext.inject

fun Route.valuesRoutes() {
    val valuesService by inject<ValuesService>()
    
    route("/values") {
        // Get all key values (public)
        get {
            val category = call.request.queryParameters["category"]
            val values = valuesService.getAllKeyValues(category)
            call.respond(HttpStatusCode.OK, values.toSuccessResponse())
        }
    }
    
    authenticate("auth-jwt") {
        route("/users/me/values") {
            // Get current user's values
            get {
                val principal = call.principal<JWTPrincipal>()!!
                val userId = principal.getUserId()
                
                val values = valuesService.getUserValues(userId)
                call.respond(HttpStatusCode.OK, values.toSuccessResponse())
            }
            
            // Save user's values
            post {
                val principal = call.principal<JWTPrincipal>()!!
                val userId = principal.getUserId()
                val request = call.receive<SaveUserValuesRequest>()
                
                val values = valuesService.saveUserValues(userId, request)
                call.respond(HttpStatusCode.Created, values.toSuccessResponse())
            }
        }
    }
}
