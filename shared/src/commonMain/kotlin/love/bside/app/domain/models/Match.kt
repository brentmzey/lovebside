package love.bside.app.domain.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Match(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("matched_user_id") val matchedUserId: String,
    @SerialName("match_score") val matchScore: Int, // 0-100
    val status: MatchStatus,
    @SerialName("expand") val expand: MatchExpand? = null
)

@Serializable
enum class MatchStatus {
    @SerialName("pending") PENDING,
    @SerialName("accepted") ACCEPTED,
    @SerialName("rejected") REJECTED
}

@Serializable
data class MatchExpand(
    @SerialName("matched_user_id") val matchedUserProfile: Profile? = null
)
