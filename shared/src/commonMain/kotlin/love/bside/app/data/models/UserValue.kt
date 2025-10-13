package love.bside.app.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserValue(
    val id: String,
    @SerialName("collectionId")
    val collectionId: String,
    @SerialName("collectionName")
    val collectionName: String,
    val created: String,
    val updated: String,
    @SerialName("userId")
    val userId: String,
    @SerialName("keyValueId")
    val keyValueId: String
)
