package io.pocketbase.migrations

/**
 * Represents a PocketBase collection schema that can be translated to other databases.
 */
data class CollectionSchema(
    val name: String,
    val fields: List<FieldSchema>,
    val indexes: List<String> = emptyList(),
    val description: String? = null
)

/**
 * Definition of a single field/column.
 */
data class FieldSchema(
    val name: String,
    val type: FieldType,
    val required: Boolean = false,
    val constraints: FieldConstraints = FieldConstraints()
)

/**
 * Optional constraints that help adapt the schema to different databases.
 */
data class FieldConstraints(
    val maxLength: Int? = null,
    val enumValues: List<String> = emptyList(),
    val relationCollection: String? = null,
    val defaultValue: String? = null
)

/**
 * Supported PocketBase field types.
 */
enum class FieldType {
    TEXT,
    NUMBER,
    BOOL,
    DATE,
    JSON,
    RELATION,
    SELECT
}

/**
 * Database targets we can emit migration statements for.
 */
sealed class DatabaseTarget(val label: String) {
    data object Postgres : DatabaseTarget("postgres")
    data object Mongo : DatabaseTarget("mongo")
}

/**
 * Represents the output of a migration plan.
 */
data class MigrationPlan(
    val target: DatabaseTarget,
    val statements: List<String>,
    val warnings: List<String> = emptyList()
)
