package love.bside.server.routes.api.v1

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import love.bside.server.models.api.MatchActionRequest
import love.bside.server.plugins.getUserId
import love.bside.server.services.MatchingService
import love.bside.server.utils.toSuccessResponse
import org.koin.ktor.ext.inject

/**
 * Match-related API routes
 */
fun Route.matchRoutes() {
    val matchingService by inject<MatchingService>()
    
    authenticate("auth-jwt") {
        route("/matches") {
            // Get user's matches
            get {
                val principal = call.principal<JWTPrincipal>()!!
                val userId = principal.getUserId()
                
                val matches = matchingService.getMatches(userId)
                call.respond(HttpStatusCode.OK, matches.toSuccessResponse())
            }
            
            // Discover new matches
            get("/discover") {
                val principal = call.principal<JWTPrincipal>()!!
                val userId = principal.getUserId()
                
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 10
                val response = matchingService.discoverMatches(userId, limit)
                call.respond(HttpStatusCode.OK, response.toSuccessResponse())
            }
            
            // Take action on a match (like or pass)
            post("/{matchId}/action") {
                val principal = call.principal<JWTPrincipal>()!!
                val matchId = call.parameters["matchId"] 
                    ?: return@post call.respond(HttpStatusCode.BadRequest, 
                        mapOf("error" to "Match ID is required"))
                
                val request = call.receive<MatchActionRequest>()
                
                val match = when (request.action.uppercase()) {
                    "LIKE" -> matchingService.likeMatch(matchId)
                    "PASS" -> matchingService.passMatch(matchId)
                    else -> return@post call.respond(HttpStatusCode.BadRequest,
                        mapOf("error" to "Invalid action. Must be LIKE or PASS"))
                }
                
                call.respond(HttpStatusCode.OK, match.toSuccessResponse())
            }
            
            // Like a match (convenience endpoint)
            post("/{matchId}/like") {
                val principal = call.principal<JWTPrincipal>()!!
                val matchId = call.parameters["matchId"]
                    ?: return@post call.respond(HttpStatusCode.BadRequest,
                        mapOf("error" to "Match ID is required"))
                
                val match = matchingService.likeMatch(matchId)
                call.respond(HttpStatusCode.OK, match.toSuccessResponse())
            }
            
            // Pass on a match (convenience endpoint)
            post("/{matchId}/pass") {
                val principal = call.principal<JWTPrincipal>()!!
                val matchId = call.parameters["matchId"]
                    ?: return@post call.respond(HttpStatusCode.BadRequest,
                        mapOf("error" to "Match ID is required"))
                
                val match = matchingService.passMatch(matchId)
                call.respond(HttpStatusCode.OK, match.toSuccessResponse())
            }
        }
    }
}
