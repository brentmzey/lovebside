package love.bside.app.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Match(
    val id: String,
    @SerialName("collectionId")
    val collectionId: String,
    @SerialName("collectionName")
    val collectionName: String,
    val created: String,
    val updated: String,
    @SerialName("userOneId")
    val userOneId: String,
    @SerialName("userTwoId")
    val userTwoId: String,
    val matchScore: Double,
    val matchStatus: String,
    val generatedAt: String
)
