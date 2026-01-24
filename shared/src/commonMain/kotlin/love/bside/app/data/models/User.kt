package love.bside.app.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import love.bside.app.data.serializers.PocketBaseInstantSerializer
import kotlinx.datetime.Instant

@Serializable
data class User(
    val id: String,
    @SerialName("collectionId")
    val collectionId: String,
    @SerialName("collectionName")
    val collectionName: String,
    @Serializable(with = PocketBaseInstantSerializer::class)
    val created: Instant,
    @Serializable(with = PocketBaseInstantSerializer::class)
    val updated: Instant,
    val username: String = "",
    val email: String,
    val name: String = "",
    val avatar: String = "",
    @SerialName("connection_type")
    val connectionType: String = "",
    @SerialName("completed_proust_questionnaire")
    val completedProustQuestionnaire: Boolean = false,
    val emailVisibility: Boolean = false,
    val verified: Boolean = false
)
