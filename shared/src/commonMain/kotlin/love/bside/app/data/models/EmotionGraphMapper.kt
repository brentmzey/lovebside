package love.bside.app.data.models

import kotlinx.datetime.Instant
import love.bside.app.domain.models.EmotionEdge as DomainEmotionEdge
import love.bside.app.domain.models.EmotionPolarity as DomainEmotionPolarity
import love.bside.app.domain.models.EmotionTargetKind as DomainEmotionTargetKind
import love.bside.app.domain.models.EmotionTerm as DomainEmotionTerm
import love.bside.app.domain.models.EdgeModifier as DomainEdgeModifier
import love.bside.app.domain.models.ExpressionModifier as DomainExpressionModifier
import love.bside.app.domain.models.GraphItem as DomainGraphItem
import love.bside.app.domain.models.GraphItemCategory as DomainGraphItemCategory
import love.bside.app.domain.models.ModifierTone as DomainModifierTone

fun EmotionTerm.toDomain(): DomainEmotionTerm = DomainEmotionTerm(
    id = id,
    created = created.parsePocketBaseInstant(),
    updated = updated.parsePocketBaseInstant(),
    slug = slug,
    label = label,
    polarity = when (polarity) {
        EmotionPolarity.POSITIVE -> DomainEmotionPolarity.POSITIVE
        EmotionPolarity.NEGATIVE -> DomainEmotionPolarity.NEGATIVE
        EmotionPolarity.NEUTRAL -> DomainEmotionPolarity.NEUTRAL
    },
    defaultVerb = defaultVerb,
    defaultAdverb = defaultAdverb,
    intensityMin = intensityMin,
    intensityMax = intensityMax,
    color = color
)

fun ExpressionModifier.toDomain(): DomainExpressionModifier = DomainExpressionModifier(
    id = id,
    created = created.parsePocketBaseInstant(),
    updated = updated.parsePocketBaseInstant(),
    ownerId = ownerId,
    slug = slug,
    label = label,
    verbOverride = verbOverride,
    adverb = adverb,
    intensityDelta = intensityDelta,
    tone = tone?.let {
        when (it) {
            ModifierTone.SOFT -> DomainModifierTone.SOFT
            ModifierTone.BOLD -> DomainModifierTone.BOLD
            ModifierTone.NEUTRAL -> DomainModifierTone.NEUTRAL
        }
    }
)

fun GraphItem.toDomain(): DomainGraphItem = DomainGraphItem(
    id = id,
    created = created.parsePocketBaseInstant(),
    updated = updated.parsePocketBaseInstant(),
    ownerId = ownerId,
    title = title,
    category = when (category) {
        GraphItemCategory.PERSON -> DomainGraphItemCategory.PERSON
        GraphItemCategory.ITEM -> DomainGraphItemCategory.ITEM
        GraphItemCategory.PLACE -> DomainGraphItemCategory.PLACE
        GraphItemCategory.MEMORY -> DomainGraphItemCategory.MEMORY
        GraphItemCategory.MEDIA -> DomainGraphItemCategory.MEDIA
        GraphItemCategory.OTHER -> DomainGraphItemCategory.OTHER
    },
    profileId = profileId,
    summary = summary,
    referenceUrl = referenceUrl
)

fun EmotionEdge.toDomain(): DomainEmotionEdge = DomainEmotionEdge(
    id = id,
    created = created.parsePocketBaseInstant(),
    updated = updated.parsePocketBaseInstant(),
    ownerId = ownerId,
    subjectProfileId = subjectProfileId,
    targetKind = when (targetKind) {
        EmotionTargetKind.PERSON -> DomainEmotionTargetKind.PERSON
        EmotionTargetKind.ITEM -> DomainEmotionTargetKind.ITEM
        EmotionTargetKind.MEMORY -> DomainEmotionTargetKind.MEMORY
        EmotionTargetKind.EXPERIENCE -> DomainEmotionTargetKind.EXPERIENCE
    },
    targetProfileId = targetProfileId,
    targetItemId = targetItemId,
    emotionTermId = emotionTermId,
    customVerb = customVerb,
    customAdverb = customAdverb,
    intensity = intensity,
    moment = moment.parsePocketBaseInstantOrNull(),
    contextTags = contextTags ?: emptyList(),
    narrative = narrative
)

fun EdgeModifier.toDomain(): DomainEdgeModifier = DomainEdgeModifier(
    id = id,
    created = created.parsePocketBaseInstant(),
    updated = updated.parsePocketBaseInstant(),
    ownerId = ownerId,
    edgeId = edgeId,
    modifierId = modifierId,
    emphasis = emphasis,
    sequence = sequence
)

private fun String.parsePocketBaseInstant(): Instant =
    Instant.parse(if (this.contains("T")) this else this.replace(" ", "T"))

private fun String?.parsePocketBaseInstantOrNull(): Instant? =
    this?.let { runCatching { it.parsePocketBaseInstant() }.getOrNull() }
