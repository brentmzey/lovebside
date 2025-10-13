package love.bside.app.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class KeyValue(
    val id: String,
    @SerialName("collectionId")
    val collectionId: String,
    @SerialName("collectionName")
    val collectionName: String,
    val created: String,
    val updated: String,
    val key: String,
    val category: String,
    val description: String? = null,
    @SerialName("displayOrder")
    val displayOrder: Int = 0
)
