package love.bside.api.contracts

/**
 * API endpoint definitions with versioning support.
 * These are the contracts between client and server.
 */
object ApiEndpoints {
    const val BASE_URL = "/api"
    
    object V1 {
        const val VERSION = "v1"
        const val BASE = "$BASE_URL/$VERSION"
        
        object Auth {
            const val LOGIN = "$BASE/auth/login"
            const val LOGOUT = "$BASE/auth/logout"
            const val REFRESH = "$BASE/auth/refresh"
            const val REGISTER = "$BASE/auth/register"
        }
        
        object Users {
            const val BASE_PATH = "$BASE/users"
            const val ME = "$BASE_PATH/me"
            fun byId(id: String) = "$BASE_PATH/$id"
        }
        
        object Conversations {
            const val BASE_PATH = "$BASE/conversations"
            fun byId(id: String) = "$BASE_PATH/$id"
            fun messages(conversationId: String) = "$BASE_PATH/$conversationId/messages"
            fun typing(conversationId: String) = "$BASE_PATH/$conversationId/typing"
        }
        
        object Messages {
            const val BASE_PATH = "$BASE/messages"
            fun byId(id: String) = "$BASE_PATH/$id"
            fun markRead(id: String) = "$BASE_PATH/$id/read"
        }
        
        object Matches {
            const val BASE_PATH = "$BASE/matches"
            fun byId(id: String) = "$BASE_PATH/$id"
            fun respond(id: String) = "$BASE_PATH/$id/respond"
        }
        
        object Questionnaire {
            const val BASE_PATH = "$BASE/questionnaire"
            const val QUESTIONS = "$BASE_PATH/questions"
            const val ANSWERS = "$BASE_PATH/answers"
            fun submitAnswer(questionId: String) = "$BASE_PATH/questions/$questionId/answer"
        }
        
        object Health {
            const val STATUS = "$BASE/health"
            const val READY = "$BASE/health/ready"
            const val LIVE = "$BASE/health/live"
        }
    }
    
    // Future versions can be added here
    object V2 {
        const val VERSION = "v2"
        const val BASE = "$BASE_URL/$VERSION"
        // V2 endpoints...
    }
}

/**
 * HTTP methods
 */
enum class HttpMethod {
    GET, POST, PUT, PATCH, DELETE, HEAD, OPTIONS
}

/**
 * API route definition
 */
data class ApiRoute(
    val path: String,
    val method: HttpMethod,
    val requiresAuth: Boolean = true,
    val description: String = ""
)

/**
 * Catalog of all API routes for documentation and testing
 */
object ApiCatalog {
    val routes = listOf(
        // Auth routes
        ApiRoute(ApiEndpoints.V1.Auth.LOGIN, HttpMethod.POST, requiresAuth = false, "User login"),
        ApiRoute(ApiEndpoints.V1.Auth.REGISTER, HttpMethod.POST, requiresAuth = false, "User registration"),
        ApiRoute(ApiEndpoints.V1.Auth.LOGOUT, HttpMethod.POST, description = "User logout"),
        
        // User routes
        ApiRoute(ApiEndpoints.V1.Users.ME, HttpMethod.GET, description = "Get current user"),
        ApiRoute(ApiEndpoints.V1.Users.ME, HttpMethod.PATCH, description = "Update current user"),
        
        // Conversation routes
        ApiRoute(ApiEndpoints.V1.Conversations.BASE_PATH, HttpMethod.GET, description = "List conversations"),
        ApiRoute(ApiEndpoints.V1.Conversations.BASE_PATH, HttpMethod.POST, description = "Create conversation"),
        
        // Message routes
        ApiRoute(ApiEndpoints.V1.Messages.BASE_PATH, HttpMethod.POST, description = "Send message"),
        
        // Match routes
        ApiRoute(ApiEndpoints.V1.Matches.BASE_PATH, HttpMethod.GET, description = "List matches"),
        
        // Health routes
        ApiRoute(ApiEndpoints.V1.Health.STATUS, HttpMethod.GET, requiresAuth = false, "Health check")
    )
}
