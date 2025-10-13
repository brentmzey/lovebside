package love.bside.app.data.models

import kotlinx.datetime.Instant
import love.bside.app.domain.models.Prompt as DomainPrompt

fun Prompt.toDomain(): DomainPrompt {
    return DomainPrompt(
        id = this.id,
        created = Instant.parse(this.created),
        updated = Instant.parse(this.updated),
        matchId = this.matchId,
        promptText = this.promptText,
        promptType = this.promptType
    )
}
