package love.bside.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ProustQuestion(
    val id: String,
    val category: String,
    val questionText: String,
    val order: Int,
    val created: String
)

@Serializable
data class ProustResponse(
    val id: String = "",
    val user: String,
    val question: String,
    val response: String,
    val created: String = "",
    val updated: String = ""
)

@Serializable
data class ProustResponseCreate(
    val user: String,
    val question: String,
    val response: String
)

@Serializable
data class Match(
    val id: String,
    val user1: String,
    val user2: String,
    val matchScore: Double,
    val matchType: String, // "algorithm", "mutual_like", "manual"
    val status: String = "pending", // "pending", "accepted", "declined", "expired"
    val expiresAt: String? = null,
    val created: String,
    val updated: String
)

@Serializable
data class Swipe(
    val id: String = "",
    val swiper: String,
    val swiped: String,
    val direction: String, // "like", "pass", "superlike"
    val created: String = ""
)
