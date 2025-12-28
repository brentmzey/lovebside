package love.bside.app.domain.models

import kotlinx.serialization.Serializable

@Serializable
data class MessagingSettings(
    val readReceiptsEnabled: Boolean = true,
    val typingStatusEnabled: Boolean = true
)
