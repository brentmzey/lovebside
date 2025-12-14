package love.bside.server.routes.api.v1

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import love.bside.server.models.api.SubmitAnswerRequest
import love.bside.server.plugins.getUserId
import love.bside.server.services.PromptService
import love.bside.server.utils.toSuccessResponse
import org.koin.ktor.ext.inject

/**
 * Prompt-related API routes
 */
fun Route.promptRoutes() {
    val promptService by inject<PromptService>()
    
    // Get all prompts (public endpoint - users need to see prompts before answering)
    route("/prompts") {
        get {
            val prompts = promptService.getAllPrompts()
            call.respond(HttpStatusCode.OK, prompts.toSuccessResponse())
        }
    }
    
    authenticate("auth-jwt") {
        route("/users/me/answers") {
            // Get user's prompt answers
            get {
                val principal = call.principal<JWTPrincipal>()!!
                val userId = principal.getUserId()
                
                val answers = promptService.getUserAnswers(userId)
                call.respond(HttpStatusCode.OK, answers.toSuccessResponse())
            }
            
            // Submit an answer to a prompt
            post {
                val principal = call.principal<JWTPrincipal>()!!
                val userId = principal.getUserId()
                val request = call.receive<SubmitAnswerRequest>()
                
                val answer = promptService.submitAnswer(userId, request)
                call.respond(HttpStatusCode.Created, answer.toSuccessResponse())
            }
        }
    }
}
