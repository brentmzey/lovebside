package love.bside.server.config

import io.ktor.server.application.*

/**
 * Server configuration loaded from environment variables and application.conf
 */
data class ServerConfig(
    val environment: Environment,
    val jwt: JwtConfig,
    val pocketbase: PocketBaseConfig,
    val server: HttpServerConfig
) {
    enum class Environment {
        DEVELOPMENT, STAGING, PRODUCTION;
        
        companion object {
            fun fromString(value: String): Environment = when (value.lowercase()) {
                "dev", "development" -> DEVELOPMENT
                "staging", "stage" -> STAGING
                "prod", "production" -> PRODUCTION
                else -> DEVELOPMENT
            }
        }
    }
    
    data class JwtConfig(
        val secret: String,
        val issuer: String,
        val audience: String,
        val realm: String,
        val expirationMs: Long = 3600000, // 1 hour
        val refreshExpirationMs: Long = 2592000000 // 30 days
    )
    
    data class PocketBaseConfig(
        val baseUrl: String,
        val timeout: Long = 30000
    )
    
    data class HttpServerConfig(
        val host: String,
        val port: Int,
        val allowedOrigins: List<String>
    )
    
    companion object {
        /**
         * Load configuration from Application environment
         */
        fun load(application: Application): ServerConfig {
            val config = application.environment.config
            
            return ServerConfig(
                environment = Environment.fromString(
                    config.propertyOrNull("app.environment")?.getString() ?: "development"
                ),
                jwt = JwtConfig(
                    secret = config.propertyOrNull("jwt.secret")?.getString() 
                        ?: System.getenv("JWT_SECRET") 
                        ?: "default-secret-change-in-production",
                    issuer = config.propertyOrNull("jwt.issuer")?.getString() 
                        ?: "https://www.bside.love",
                    audience = config.propertyOrNull("jwt.audience")?.getString() 
                        ?: "bside-api",
                    realm = config.propertyOrNull("jwt.realm")?.getString() 
                        ?: "B-Side API",
                    expirationMs = config.propertyOrNull("jwt.expiration")?.getString()?.toLong() 
                        ?: 3600000,
                    refreshExpirationMs = config.propertyOrNull("jwt.refreshExpiration")?.getString()?.toLong() 
                        ?: 2592000000
                ),
                pocketbase = PocketBaseConfig(
                    baseUrl = config.propertyOrNull("pocketbase.url")?.getString() 
                        ?: System.getenv("POCKETBASE_URL") 
                        ?: "https://bside.pockethost.io",
                    timeout = config.propertyOrNull("pocketbase.timeout")?.getString()?.toLong() 
                        ?: 30000
                ),
                server = HttpServerConfig(
                    host = config.propertyOrNull("server.host")?.getString() 
                        ?: "0.0.0.0",
                    port = config.propertyOrNull("server.port")?.getString()?.toInt() 
                        ?: 8080,
                    allowedOrigins = config.propertyOrNull("server.allowedOrigins")?.getString()
                        ?.split(",")?.map { it.trim() }
                        ?: listOf("http://localhost:8080", "http://localhost:3000", "https://www.bside.love")
                )
            )
        }
    }
}
