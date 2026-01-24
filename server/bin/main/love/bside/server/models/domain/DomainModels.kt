package love.bside.server.models.domain

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

/**
 * Domain models for server business logic
 * These represent the core business entities
 */

data class User(
    val id: String,
    val email: String,
    val emailVisibility: Boolean = false,
    val verified: Boolean = false,
    val createdAt: Instant,
    val updatedAt: Instant
)

data class Profile(
    val id: String,
    val userId: String,
    val firstName: String,
    val lastName: String,
    val birthDate: LocalDate,
    val bio: String? = null,
    val location: String? = null,
    val seeking: SeekingType,
    val createdAt: Instant,
    val updatedAt: Instant
) {
    val age: Int
        get() {
            val now = kotlinx.datetime.Clock.System.now()
            val today = now.toString().substring(0, 10) // Simple date extraction
            val years = today.substring(0, 4).toInt() - birthDate.year
            val hasHadBirthdayThisYear = today.substring(5) >= birthDate.toString().substring(5)
            return if (hasHadBirthdayThisYear) years else years - 1
        }
}

enum class SeekingType {
    FRIENDSHIP,
    RELATIONSHIP,
    BOTH;
    
    companion object {
        fun fromString(value: String): SeekingType = when (value.uppercase()) {
            "FRIENDSHIP" -> FRIENDSHIP
            "RELATIONSHIP" -> RELATIONSHIP
            "BOTH" -> BOTH
            else -> BOTH
        }
    }
}

data class KeyValue(
    val id: String,
    val key: String,
    val category: CategoryType,
    val description: String? = null,
    val displayOrder: Int = 0,
    val createdAt: Instant,
    val updatedAt: Instant
)

enum class CategoryType {
    PERSONALITY,
    VALUES,
    INTERESTS,
    LIFESTYLE;
    
    companion object {
        fun fromString(value: String): CategoryType = when (value.uppercase()) {
            "PERSONALITY" -> PERSONALITY
            "VALUES" -> VALUES
            "INTERESTS" -> INTERESTS
            "LIFESTYLE" -> LIFESTYLE
            else -> VALUES
        }
    }
}

data class UserValue(
    val id: String,
    val userId: String,
    val keyValue: KeyValue,
    val importance: Int, // 1-10
    val createdAt: Instant,
    val updatedAt: Instant
)

data class Match(
    val id: String,
    val userId: String,
    val matchedUserId: String,
    val compatibilityScore: Double,
    val status: MatchStatus,
    val createdAt: Instant,
    val updatedAt: Instant
)

enum class MatchStatus {
    PENDING,
    LIKED,
    PASSED,
    MUTUAL;
    
    companion object {
        fun fromString(value: String): MatchStatus = when (value.uppercase()) {
            "PENDING" -> PENDING
            "LIKED" -> LIKED
            "PASSED" -> PASSED
            "MUTUAL" -> MUTUAL
            else -> PENDING
        }
    }
}

data class Prompt(
    val id: String,
    val text: String,
    val category: String,
    val displayOrder: Int = 0,
    val createdAt: Instant,
    val updatedAt: Instant
)

data class UserAnswer(
    val id: String,
    val userId: String,
    val promptId: String,
    val answer: String,
    val createdAt: Instant,
    val updatedAt: Instant
)

data class AuthToken(
    val token: String,
    val refreshToken: String,
    val expiresAt: Instant
)
