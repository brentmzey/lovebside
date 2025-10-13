package love.bside.app.domain.models

import kotlinx.datetime.Instant

data class KeyValue(
    val id: String,
    val created: Instant,
    val updated: Instant,
    val key: String,
    val valueText: String,
    val category: String,
    val description: String? = null,
    val displayOrder: Int = 0
)
