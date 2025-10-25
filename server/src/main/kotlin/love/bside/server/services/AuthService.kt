package love.bside.server.services

import love.bside.app.core.Result
import love.bside.server.config.ServerConfig
import love.bside.server.models.api.LoginRequest
import love.bside.server.models.api.RegisterRequest
import love.bside.server.models.api.AuthResponse
import love.bside.server.models.domain.User
import love.bside.server.models.domain.Profile
import love.bside.server.plugins.ValidationException
import love.bside.server.plugins.AuthenticationException
import love.bside.server.plugins.ConflictException
import love.bside.server.repositories.UserRepository
import love.bside.server.repositories.ProfileRepository
import love.bside.server.utils.*

/**
 * Service for authentication operations
 */
class AuthService(
    private val userRepository: UserRepository,
    private val profileRepository: ProfileRepository,
    private val config: ServerConfig
) {
    
    /**
     * Login user with email and password
     */
    suspend fun login(request: LoginRequest): AuthResponse {
        // Validate input
        validateEmail(request.email)
        validatePassword(request.password)
        
        // Authenticate with PocketBase
        val authResult = userRepository.authenticate(request.email, request.password)
        
        return when (authResult) {
            is Result.Error -> {
                throw AuthenticationException("Invalid email or password")
            }
            is Result.Success -> {
                val (_, user) = authResult.data
                
                // Get user profile
                val profile = when (val profileResult = profileRepository.getProfileByUserId(user.id)) {
                    is Result.Success -> profileResult.data
                    is Result.Error -> null
                    is Result.Loading -> null
                }
                
                // Generate JWT tokens
                val tokens = JwtUtils.generateTokenPair(user.id, user.email, config.jwt)
                
                tokens.toAuthResponse(user, profile)
            }
            is Result.Loading -> {
                throw AuthenticationException("Authentication is still loading")
            }
        }
    }
    
    /**
     * Register new user
     */
    suspend fun register(request: RegisterRequest): AuthResponse {
        // Validate input
        validateEmail(request.email)
        validatePassword(request.password)
        validatePasswordConfirmation(request.password, request.passwordConfirm)
        validateName(request.firstName, "First name")
        validateName(request.lastName, "Last name")
        validateBirthDate(request.birthDate)
        validateSeeking(request.seeking)
        
        // Check if user already exists
        when (val existingUser = userRepository.getUserByEmail(request.email)) {
            is Result.Success -> {
                if (existingUser.data != null) {
                    throw ConflictException("User with this email already exists")
                }
            }
            is Result.Error -> {
                // Error checking, continue
            }
            is Result.Loading -> {
                // Skip check if loading
            }
        }
        
        // Create user
        val userResult = userRepository.createUser(request.email, request.password)
        val user = when (userResult) {
            is Result.Success -> userResult.data
            is Result.Error -> throw Exception("Failed to create user: ${userResult.exception.message}")
            is Result.Loading -> throw Exception("User creation is still loading")
        }
        
        // Create profile
        val profileResult = profileRepository.createProfile(
            userId = user.id,
            firstName = request.firstName,
            lastName = request.lastName,
            birthDate = request.birthDate,
            seeking = request.seeking
        )
        
        val profile = when (profileResult) {
            is Result.Success -> profileResult.data
            is Result.Error -> throw Exception("Failed to create profile: ${profileResult.exception.message}")
            is Result.Loading -> throw Exception("Profile creation is still loading")
        }
        
        // Generate JWT tokens
        val tokens = JwtUtils.generateTokenPair(user.id, user.email, config.jwt)
        
        return tokens.toAuthResponse(user, profile)
    }
    
    /**
     * Refresh access token
     */
        suspend fun refreshToken(refreshToken: String): AuthResponse {
            // Verify refresh token
            val userId = JwtUtils.getUserIdFromToken(refreshToken, config.jwt)
                ?: throw AuthenticationException("Invalid refresh token")
            
            if (!JwtUtils.isRefreshToken(refreshToken, config.jwt)) {
                throw AuthenticationException("Token is not a refresh token")
            }
            
            // Get user
            val user = when (val userResult = userRepository.getUserById(userId)) {
                is Result.Success -> userResult.data
                is Result.Error -> throw AuthenticationException("User not found")
                is Result.Loading -> throw AuthenticationException("User lookup is still loading")
            }
            
            // Get profile
            val profile = when (val profileResult = profileRepository.getProfileByUserId(user.id)) {
                is Result.Success -> profileResult.data
                is Result.Error -> null
                is Result.Loading -> null
            }
            
            // Generate new tokens
            val tokens = JwtUtils.generateTokenPair(user.id, user.email, config.jwt)
            
            return tokens.toAuthResponse(user, profile)
        }
        
        // ===== Validation Helpers =====
        
        private fun validateEmail(email: String) {
            if (email.isBlank()) {
                throw ValidationException("Email is required")
            }
            if (!email.matches(Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"))) {
                throw ValidationException("Invalid email format")
            }
        }
        
        private fun validatePassword(password: String) {
            if (password.length < 8) {
                throw ValidationException("Password must be at least 8 characters long")
            }
        }
        
        private fun validatePasswordConfirmation(password: String, passwordConfirm: String) {
            if (password != passwordConfirm) {
                throw ValidationException("Passwords do not match")
            }
        }
        
        private fun validateName(name: String, fieldName: String) {
            if (name.isBlank()) {
                throw ValidationException("$fieldName is required")
            }
            if (name.length > 50) {
                throw ValidationException("$fieldName must be less than 50 characters")
            }
        }
        
        private fun validateBirthDate(birthDate: String) {
            try {
                kotlinx.datetime.LocalDate.parse(birthDate)
            } catch (e: Exception) {
                throw ValidationException("Invalid birth date format. Use YYYY-MM-DD")
            }
        }
        
        private fun validateSeeking(seeking: String) {
            if (seeking !in listOf("FRIENDSHIP", "RELATIONSHIP", "BOTH")) {
                throw ValidationException("Invalid seeking value. Must be FRIENDSHIP, RELATIONSHIP, or BOTH")
            }
        }
    }
