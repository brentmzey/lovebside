package io.pocketbase.migrations

/**
 * Produces migration statements for databases other than PocketBase's internal SQLite engine.
 *
 * The goal is to keep the logic fully multiplatform/pure Kotlin so that the same planning
 * utilities can run on JVM, iOS, or JS alongside the rest of the PocketBase SDK.
 */
class MigrationController {
    /**
     * Generate a migration plan for the requested [target].
     */
    fun planFor(target: DatabaseTarget, schemas: List<CollectionSchema>): MigrationPlan {
        val warnings = mutableListOf<String>()
        val statements = schemas
            .sortedBy { it.name }
            .flatMap { schema ->
                when (target) {
                    DatabaseTarget.Postgres -> emitPostgres(schema, warnings)
                    DatabaseTarget.Mongo -> emitMongo(schema, warnings)
                }
            }
        return MigrationPlan(target, statements, warnings)
    }

    private fun emitPostgres(
        schema: CollectionSchema,
        warnings: MutableList<String>
    ): List<String> {
        val columns = mutableListOf<String>()
        val hasIdColumn = schema.fields.any { it.name.equals("id", ignoreCase = true) }
        if (!hasIdColumn) {
            columns += "\"id\" TEXT PRIMARY KEY"
        }
        columns += schema.fields.map { it.toPostgresColumn(schema.name, warnings) }

        val createTable = buildString {
            append("CREATE TABLE IF NOT EXISTS \"")
            append(schema.name)
            append("\" (\n  ")
            append(columns.joinToString(",\n  "))
            append("\n);")
        }

        val indexStatements = schema.indexes.map { index ->
            val trimmed = index.trim()
            if (trimmed.startsWith("create", ignoreCase = true)) {
                trimmed.ensureSemicolon()
            } else {
                val safeName = "${schema.name}_${trimmed.replace('"', '_').replace('`', '_').replace(' ', '_')}_idx"
                "CREATE INDEX IF NOT EXISTS \"$safeName\" ON \"${schema.name}\" (\"${trimmed}\");"
            }
        }

        return listOf(createTable) + indexStatements
    }

    private fun emitMongo(
        schema: CollectionSchema,
        warnings: MutableList<String>
    ): List<String> {
        val required = schema.fields.filter { it.required }.map { it.name }
        val properties = schema.fields.joinToString(",\n") { it.toMongoProperty() }

        val createCollection = buildString {
            append("db.createCollection(\"")
            append(schema.name)
            append("\", {\n  validator: {\n    $JSON_SCHEMA_KEY: {\n")
            if (required.isNotEmpty()) {
                append("      required: [")
                append(required.joinToString(", ") { "\"$it\"" })
                append("],\n")
            }
            append("      properties: {\n")
            append(properties)
            append("\n      }\n    }\n  }\n});")
        }

        val indexStatements = schema.indexes.mapNotNull { toMongoIndex(schema.name, it) }
            .ifEmpty {
                emptyList()
            }

        if (schema.indexes.isNotEmpty() && indexStatements.isEmpty()) {
            warnings += "Failed to translate SQL indexes for collection ${schema.name} to Mongo syntax"
        }

        return listOf(createCollection) + indexStatements
    }

    private fun FieldSchema.toPostgresColumn(
        collectionName: String,
        warnings: MutableList<String>
    ): String {
        val columnType = when (type) {
            FieldType.TEXT -> constraints.maxLength?.let { "VARCHAR($it)" } ?: "TEXT"
            FieldType.NUMBER -> "DOUBLE PRECISION"
            FieldType.BOOL -> "BOOLEAN"
            FieldType.DATE -> "TIMESTAMPTZ"
            FieldType.JSON -> "JSONB"
            FieldType.RELATION -> "TEXT"
            FieldType.SELECT -> constraints.maxLength?.let { "VARCHAR($it)" } ?: "TEXT"
        }

        val stringBuilder = StringBuilder()
        stringBuilder.append("\"")
        stringBuilder.append(name)
        stringBuilder.append("\" ")
        stringBuilder.append(columnType)

        if (required) {
            stringBuilder.append(" NOT NULL")
        }

        constraints.defaultValue?.let {
            stringBuilder.append(" DEFAULT ")
            stringBuilder.append(it)
        }

        if (type == FieldType.RELATION) {
            val relation = constraints.relationCollection
            if (relation.isNullOrBlank()) {
                warnings += "Relation field $name in collection $collectionName is missing relationCollection metadata"
            } else {
                stringBuilder.append(" REFERENCES \"")
                stringBuilder.append(relation)
                stringBuilder.append("\"(\"id\") ON DELETE CASCADE")
            }
        }

        if (type == FieldType.SELECT && constraints.enumValues.isNotEmpty()) {
            val allowed = constraints.enumValues.joinToString(", ") { "'${it}'" }
            stringBuilder.append(" CHECK (\"")
            stringBuilder.append(name)
            stringBuilder.append("\" = ANY(ARRAY[")
            stringBuilder.append(allowed)
            stringBuilder.append("]::TEXT[]))")
        }

        return stringBuilder.toString()
    }

    private fun FieldSchema.toMongoProperty(): String {
        val bsonType = when (type) {
            FieldType.TEXT, FieldType.SELECT, FieldType.RELATION -> "string"
            FieldType.NUMBER -> "double"
            FieldType.BOOL -> "bool"
            FieldType.DATE -> "date"
            FieldType.JSON -> "object"
        }

        val builder = StringBuilder()
        builder.append("        \"")
        builder.append(name)
        builder.append("\": { bsonType: \"")
        builder.append(bsonType)
        builder.append("\"")

        if (constraints.enumValues.isNotEmpty()) {
            val values = constraints.enumValues.joinToString(", ") { "\"$it\"" }
            builder.append(", enum: [")
            builder.append(values)
            builder.append("]")
        }

        builder.append(" }")
        return builder.toString()
    }

    private fun toMongoIndex(collectionName: String, rawIndex: String): String? {
        val trimmed = rawIndex.trim()
        val matcher = INDEX_FIELDS_REGEX.find(trimmed) ?: return null
        val fields = matcher.groupValues[1]
            .split(',')
            .map { it.trim().trim('"', '`', '\'') }
            .filter { it.isNotEmpty() }
        if (fields.isEmpty()) return null
        val spec = fields.joinToString(", ") { "\"$it\": 1" }
        val uniqueClause = if (trimmed.contains("unique", ignoreCase = true)) ", { unique: true }" else ""
        return "db.getCollection(\"$collectionName\").createIndex({ $spec }$uniqueClause);"
    }

    private fun String.ensureSemicolon(): String = if (trimEnd().endsWith(';')) this else "$this;"

    private companion object {
        private val INDEX_FIELDS_REGEX = Regex("\\(([^)]+)\\)")
        private const val JSON_SCHEMA_KEY = "\$jsonSchema"
    }
}
