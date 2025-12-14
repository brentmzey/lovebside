package love.bside.app.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EmotionTerm(
    val id: String,
    @SerialName("collectionId")
    val collectionId: String,
    @SerialName("collectionName")
    val collectionName: String,
    val created: String,
    val updated: String,
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
    @SerialName("POSITIVE")
    POSITIVE,

    @SerialName("NEGATIVE")
    NEGATIVE,

    @SerialName("NEUTRAL")
    NEUTRAL
}

@Serializable
data class ExpressionModifier(
    val id: String,
    @SerialName("collectionId")
    val collectionId: String,
    @SerialName("collectionName")
    val collectionName: String,
    val created: String,
    val updated: String,
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
    @SerialName("SOFT")
    SOFT,

    @SerialName("BOLD")
    BOLD,

    @SerialName("NEUTRAL")
    NEUTRAL
}

@Serializable
data class GraphItem(
    val id: String,
    @SerialName("collectionId")
    val collectionId: String,
    @SerialName("collectionName")
    val collectionName: String,
    val created: String,
    val updated: String,
    val ownerId: String,
    val title: String,
    val category: GraphItemCategory,
    val profileId: String? = null,
    val summary: String? = null,
    val referenceUrl: String? = null
)

@Serializable
enum class GraphItemCategory {
    @SerialName("PERSON")
    PERSON,

    @SerialName("ITEM")
    ITEM,

    @SerialName("PLACE")
    PLACE,

    @SerialName("MEMORY")
    MEMORY,

    @SerialName("MEDIA")
    MEDIA,

    @SerialName("OTHER")
    OTHER
}

@Serializable
data class EmotionEdge(
    val id: String,
    @SerialName("collectionId")
    val collectionId: String,
    @SerialName("collectionName")
    val collectionName: String,
    val created: String,
    val updated: String,
    val ownerId: String,
    val subjectProfileId: String? = null,
    val targetKind: EmotionTargetKind,
    val targetProfileId: String? = null,
    val targetItemId: String? = null,
    val emotionTermId: String,
    val customVerb: String? = null,
    val customAdverb: String? = null,
    val intensity: Int,
    val moment: String? = null,
    val contextTags: List<String>? = null,
    val narrative: String? = null
)

@Serializable
enum class EmotionTargetKind {
    @SerialName("PERSON")
    PERSON,

    @SerialName("ITEM")
    ITEM,

    @SerialName("MEMORY")
    MEMORY,

    @SerialName("EXPERIENCE")
    EXPERIENCE
}

@Serializable
data class EdgeModifier(
    val id: String,
    @SerialName("collectionId")
    val collectionId: String,
    @SerialName("collectionName")
    val collectionName: String,
    val created: String,
    val updated: String,
    val ownerId: String,
    val edgeId: String,
    val modifierId: String,
    val emphasis: Int? = null,
    val sequence: Int? = null
)
