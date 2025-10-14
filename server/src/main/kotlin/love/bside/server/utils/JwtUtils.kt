package love.bside.server.utils

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.DecodedJWT
import love.bside.server.config.ServerConfig
import love.bside.server.models.domain.AuthToken
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.milliseconds

/**
 * JWT token generation and validation utilities
 */
object JwtUtils {
    
    /**
     * Generate access token for user
     */
    fun generateAccessToken(userId: String, email: String, config: ServerConfig.JwtConfig): String {
        val expiresAt = Clock.System.now().plus(config.expirationMs.milliseconds)
        
        return JWT.create()
            .withAudience(config.audience)
            .withIssuer(config.issuer)
            .withClaim("userId", userId)
            .withClaim("email", email)
            .withExpiresAt(java.util.Date(expiresAt.toEpochMilliseconds()))
            .sign(Algorithm.HMAC256(config.secret))
    }
    
    /**
     * Generate refresh token for user
     */
    fun generateRefreshToken(userId: String, config: ServerConfig.JwtConfig): String {
        val expiresAt = Clock.System.now().plus(config.refreshExpirationMs.milliseconds)
        
        return JWT.create()
            .withAudience(config.audience)
            .withIssuer(config.issuer)
            .withClaim("userId", userId)
            .withClaim("type", "refresh")
            .withExpiresAt(java.util.Date(expiresAt.toEpochMilliseconds()))
            .sign(Algorithm.HMAC256(config.secret))
    }
    
    /**
     * Generate both access and refresh tokens
     */
    fun generateTokenPair(userId: String, email: String, config: ServerConfig.JwtConfig): AuthToken {
        val accessToken = generateAccessToken(userId, email, config)
        val refreshToken = generateRefreshToken(userId, config)
        val expiresAt = Clock.System.now().plus(config.expirationMs.milliseconds)
        
        return AuthToken(
            token = accessToken,
            refreshToken = refreshToken,
            expiresAt = expiresAt
        )
    }
    
    /**
     * Decode and verify JWT token
     */
    fun verifyToken(token: String, config: ServerConfig.JwtConfig): DecodedJWT? {
        return try {
            val verifier = JWT.require(Algorithm.HMAC256(config.secret))
                .withAudience(config.audience)
                .withIssuer(config.issuer)
                .build()
            
            verifier.verify(token)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Extract user ID from token
     */
    fun getUserIdFromToken(token: String, config: ServerConfig.JwtConfig): String? {
        return verifyToken(token, config)?.getClaim("userId")?.asString()
    }
    
    /**
     * Check if token is a refresh token
     */
    fun isRefreshToken(token: String, config: ServerConfig.JwtConfig): Boolean {
        return verifyToken(token, config)?.getClaim("type")?.asString() == "refresh"
    }
    
    /**
     * Check if token has expired
     */
    fun isTokenExpired(token: String, config: ServerConfig.JwtConfig): Boolean {
        val decoded = verifyToken(token, config) ?: return true
        val expiresAt = decoded.expiresAt?.time ?: return true
        return expiresAt < System.currentTimeMillis()
    }
}
