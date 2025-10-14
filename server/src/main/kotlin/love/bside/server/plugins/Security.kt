package love.bside.server.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import love.bside.server.config.ServerConfig

/**
 * Configure JWT authentication
 */
fun Application.configureSecurity(config: ServerConfig) {
    val jwtConfig = config.jwt
    
    authentication {
        jwt("auth-jwt") {
            realm = jwtConfig.realm
            
            verifier(
                JWT.require(Algorithm.HMAC256(jwtConfig.secret))
                    .withAudience(jwtConfig.audience)
                    .withIssuer(jwtConfig.issuer)
                    .build()
            )
            
            validate { credential ->
                // Validate that the token has a valid userId claim
                val userId = credential.payload.getClaim("userId").asString()
                if (userId.isNullOrEmpty()) {
                    null
                } else {
                    JWTPrincipal(credential.payload)
                }
            }
            
            challenge { _, _ ->
                // Return 401 Unauthorized for invalid/missing tokens
                // The response is handled by StatusPages plugin
            }
        }
    }
}

/**
 * Extension to get user ID from JWT principal
 */
fun JWTPrincipal.getUserId(): String {
    return payload.getClaim("userId").asString()
}

/**
 * Extension to get email from JWT principal
 */
fun JWTPrincipal.getEmail(): String? {
    return payload.getClaim("email").asString()
}
