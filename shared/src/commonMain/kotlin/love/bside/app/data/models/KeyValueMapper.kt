package love.bside.app.data.models

import kotlinx.datetime.Instant
import love.bside.app.domain.models.KeyValue as DomainKeyValue

fun KeyValue.toDomain(): DomainKeyValue {
    return DomainKeyValue(
        id = this.id,
        created = Instant.parse(this.created),
        updated = Instant.parse(this.updated),
        key = this.key,
        valueText = this.key.replace("_", " ").split(" ").joinToString(" ") { it.replaceFirstChar { char -> char.uppercaseChar() } },
        category = this.category,
        description = this.description,
        displayOrder = this.displayOrder
    )
}
