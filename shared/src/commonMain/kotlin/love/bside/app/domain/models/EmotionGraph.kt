package love.bside.app.domain.models

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class EmotionTerm(
    val id: String,
    val created: Instant,
    val updated: Instant,
    val slug: String,
    val label: String,
    val polarity: EmotionPolarity,
    val defaultVerb: String,
    val defaultAdverb: String? = null,
    val intensityMin: Int,
    val intensityMax: Int,
    val color: String? = null
)

@Serializable
enum class EmotionPolarity {
    POSITIVE,
    NEGATIVE,
    NEUTRAL
}

@Serializable
data class ExpressionModifier(
    val id: String,
    val created: Instant,
    val updated: Instant,
    val ownerId: String,
    val slug: String,
    val label: String,
    val verbOverride: String? = null,
    val adverb: String? = null,
    val intensityDelta: Int? = null,
    val tone: ModifierTone? = null
)

@Serializable
enum class ModifierTone {
    SOFT,
    BOLD,
    NEUTRAL
}

@Serializable
data class GraphItem(
    val id: String,
    val created: Instant,
    val updated: Instant,
    val ownerId: String,
    val title: String,
    val category: GraphItemCategory,
    val profileId: String? = null,
    val summary: String? = null,
    val referenceUrl: String? = null
)

@Serializable
enum class GraphItemCategory {
    PERSON,
    ITEM,
    PLACE,
    MEMORY,
    MEDIA,
    OTHER
}

@Serializable
data class EmotionEdge(
    val id: String,
    val created: Instant,
    val updated: Instant,
    val ownerId: String,
    val subjectProfileId: String? = null,
    val targetKind: EmotionTargetKind,
    val targetProfileId: String? = null,
    val targetItemId: String? = null,
    val emotionTermId: String,
    val customVerb: String? = null,
    val customAdverb: String? = null,
    val intensity: Int,
    val moment: Instant? = null,
    val contextTags: List<String> = emptyList(),
    val narrative: String? = null
)

@Serializable
enum class EmotionTargetKind {
    PERSON,
    ITEM,
    MEMORY,
    EXPERIENCE
}

@Serializable
data class EdgeModifier(
    val id: String,
    val created: Instant,
    val updated: Instant,
    val ownerId: String,
    val edgeId: String,
    val modifierId: String,
    val emphasis: Int? = null,
    val sequence: Int? = null
)
