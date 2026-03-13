package love.bside.server.schema

import kotlinx.serialization.Serializable

/**
 * Represents the expected schema for a PocketBase collection
 */
@Serializable
data class CollectionSchema(
    val name: String,
    val type: CollectionType,
    val fields: List<FieldSchema>,
    val indexes: List<IndexSchema> = emptyList(),
    val apiRules: ApiRules? = null
)

@Serializable
enum class CollectionType {
    BASE, AUTH, VIEW
}

@Serializable
data class FieldSchema(
    val name: String,
    val type: FieldType,
    val required: Boolean = false,
    val unique: Boolean = false,
    val options: FieldOptions? = null
)

@Serializable
enum class FieldType {
    TEXT, EMAIL, NUMBER, BOOL, DATE, DATETIME, JSON,
    SELECT, RELATION, FILE, URL
}

@Serializable
data class FieldOptions(
    val min: Int? = null,
    val max: Int? = null,
    val pattern: String? = null,
    val values: List<String>? = null,
    val maxSelect: Int? = null,
    val collectionId: String? = null,
    val cascadeDelete: Boolean? = null,
    val maxSize: Long? = null
)

@Serializable
data class IndexSchema(
    val name: String,
    val fields: List<String>,
    val unique: Boolean = false
)

@Serializable
data class ApiRules(
    val listRule: String? = null,
    val viewRule: String? = null,
    val createRule: String? = null,
    val updateRule: String? = null,
    val deleteRule: String? = null
)

/**
 * Schema definitions for all B-Side collections
 */
object BsideSchema {
    
    val profiles = CollectionSchema(
        name = "s_profiles",
        type = CollectionType.BASE,
        fields = listOf(
            FieldSchema("userId", FieldType.RELATION, required = true, unique = true,
                options = FieldOptions(collectionId = "users", cascadeDelete = true)),
            FieldSchema("firstName", FieldType.TEXT, required = true,
                options = FieldOptions(min = 1, max = 50)),
            FieldSchema("lastName", FieldType.TEXT, required = true,
                options = FieldOptions(min = 1, max = 50)),
            FieldSchema("birthDate", FieldType.DATE, required = true),
            FieldSchema("bio", FieldType.TEXT, options = FieldOptions(max = 500)),
            FieldSchema("location", FieldType.TEXT, options = FieldOptions(max = 100)),
            FieldSchema("seeking", FieldType.SELECT, required = true,
                options = FieldOptions(values = listOf("FRIENDSHIP", "RELATIONSHIP", "BOTH")))
        ),
        indexes = listOf(
            IndexSchema("idx_userId", listOf("userId"), unique = true),
            IndexSchema("idx_seeking", listOf("seeking"))
        ),
        apiRules = ApiRules(
            listRule = "@request.auth.id != \"\"",
            viewRule = "@request.auth.id != \"\" && (userId = @request.auth.id || @request.auth.id != \"\")",
            createRule = "@request.auth.id != \"\" && userId = @request.auth.id",
            updateRule = "@request.auth.id != \"\" && userId = @request.auth.id",
            deleteRule = "@request.auth.id != \"\" && userId = @request.auth.id"
        )
    )
    
    val keyValues = CollectionSchema(
        name = "s_key_values",
        type = CollectionType.BASE,
        fields = listOf(
            FieldSchema("key", FieldType.TEXT, required = true, unique = true,
                options = FieldOptions(max = 100)),
            FieldSchema("category", FieldType.SELECT, required = true,
                options = FieldOptions(values = listOf("PERSONALITY", "VALUES", "INTERESTS", "LIFESTYLE"))),
            FieldSchema("description", FieldType.TEXT, options = FieldOptions(max = 500)),
            FieldSchema("displayOrder", FieldType.NUMBER, options = FieldOptions(min = 0))
        ),
        indexes = listOf(
            IndexSchema("idx_key", listOf("key"), unique = true),
            IndexSchema("idx_category", listOf("category"))
        ),
        apiRules = ApiRules(
            listRule = "",  // Public read
            viewRule = "",  // Public read
            createRule = null,  // Admin only
            updateRule = null,  // Admin only
            deleteRule = null   // Admin only
        )
    )
    
    val userValues = CollectionSchema(
        name = "s_user_values",
        type = CollectionType.BASE,
        fields = listOf(
            FieldSchema("userId", FieldType.RELATION, required = true,
                options = FieldOptions(collectionId = "users", cascadeDelete = true)),
            FieldSchema("keyValueId", FieldType.RELATION, required = true,
                options = FieldOptions(collectionId = "s_key_values")),
            FieldSchema("importance", FieldType.NUMBER, required = true,
                options = FieldOptions(min = 1, max = 10))
        ),
        indexes = listOf(
            IndexSchema("idx_userId", listOf("userId")),
            IndexSchema("idx_keyValueId", listOf("keyValueId")),
            IndexSchema("idx_user_keyValue", listOf("userId", "keyValueId"), unique = true)
        ),
        apiRules = ApiRules(
            listRule = "@request.auth.id != \"\" && userId = @request.auth.id",
            viewRule = "@request.auth.id != \"\" && userId = @request.auth.id",
            createRule = "@request.auth.id != \"\" && userId = @request.auth.id",
            updateRule = "@request.auth.id != \"\" && userId = @request.auth.id",
            deleteRule = "@request.auth.id != \"\" && userId = @request.auth.id"
        )
    )
    
    val prompts = CollectionSchema(
        name = "s_prompts",
        type = CollectionType.BASE,
        fields = listOf(
            FieldSchema("text", FieldType.TEXT, required = true,
                options = FieldOptions(max = 200)),
            FieldSchema("category", FieldType.SELECT,
                options = FieldOptions(values = listOf("ICEBREAKER", "DEEP", "FUN", "VALUES"))),
            FieldSchema("isActive", FieldType.BOOL)
        ),
        indexes = listOf(
            IndexSchema("idx_category", listOf("category")),
            IndexSchema("idx_isActive", listOf("isActive"))
        ),
        apiRules = ApiRules(
            listRule = "",  // Public read
            viewRule = "",  // Public read
            createRule = null,  // Admin only
            updateRule = null,  // Admin only
            deleteRule = null   // Admin only
        )
    )
    
    val userAnswers = CollectionSchema(
        name = "s_user_answers",
        type = CollectionType.BASE,
        fields = listOf(
            FieldSchema("userId", FieldType.RELATION, required = true,
                options = FieldOptions(collectionId = "users", cascadeDelete = true)),
            FieldSchema("promptId", FieldType.RELATION, required = true,
                options = FieldOptions(collectionId = "s_prompts")),
            FieldSchema("answer", FieldType.TEXT, required = true,
                options = FieldOptions(max = 1000))
        ),
        indexes = listOf(
            IndexSchema("idx_userId", listOf("userId")),
            IndexSchema("idx_promptId", listOf("promptId")),
            IndexSchema("idx_user_prompt", listOf("userId", "promptId"), unique = true)
        ),
        apiRules = ApiRules(
            listRule = "@request.auth.id != \"\"",
            viewRule = "@request.auth.id != \"\"",
            createRule = "@request.auth.id != \"\" && userId = @request.auth.id",
            updateRule = "@request.auth.id != \"\" && userId = @request.auth.id",
            deleteRule = "@request.auth.id != \"\" && userId = @request.auth.id"
        )
    )
    
    val matches = CollectionSchema(
        name = "s_matches",
        type = CollectionType.BASE,
        fields = listOf(
            FieldSchema("userId", FieldType.RELATION, required = true,
                options = FieldOptions(collectionId = "users")),
            FieldSchema("matchedUserId", FieldType.RELATION, required = true,
                options = FieldOptions(collectionId = "users")),
            FieldSchema("compatibilityScore", FieldType.NUMBER, required = true,
                options = FieldOptions(min = 0, max = 100)),
            FieldSchema("sharedValues", FieldType.JSON),
            FieldSchema("status", FieldType.SELECT, required = true,
                options = FieldOptions(values = listOf("PENDING", "ACCEPTED", "REJECTED", "BLOCKED"))),
            FieldSchema("lastCalculated", FieldType.DATETIME)
        ),
        indexes = listOf(
            IndexSchema("idx_userId", listOf("userId")),
            IndexSchema("idx_matchedUserId", listOf("matchedUserId")),
            IndexSchema("idx_compatibilityScore", listOf("compatibilityScore")),
            IndexSchema("idx_status", listOf("status")),
            IndexSchema("idx_user_matched", listOf("userId", "matchedUserId"), unique = true)
        ),
        apiRules = ApiRules(
            listRule = "@request.auth.id != \"\" && (userId = @request.auth.id || matchedUserId = @request.auth.id)",
            viewRule = "@request.auth.id != \"\" && (userId = @request.auth.id || matchedUserId = @request.auth.id)",
            createRule = null,  // System only
            updateRule = "@request.auth.id != \"\" && (userId = @request.auth.id || matchedUserId = @request.auth.id)",
            deleteRule = "@request.auth.id != \"\" && userId = @request.auth.id"
        )
    )
    
    val migrations = CollectionSchema(
        name = "s_migrations",
        type = CollectionType.BASE,
        fields = listOf(
            FieldSchema("version", FieldType.NUMBER, required = true, unique = true),
            FieldSchema("name", FieldType.TEXT, required = true),
            FieldSchema("description", FieldType.TEXT),
            FieldSchema("appliedAt", FieldType.DATETIME),
            FieldSchema("executionTimeMs", FieldType.NUMBER, required = true)
        ),
        indexes = listOf(
            IndexSchema("idx_version", listOf("version"), unique = true),
            IndexSchema("idx_appliedAt", listOf("appliedAt"))
        ),
        apiRules = ApiRules(
            listRule = null,  // Admin only
            viewRule = null,  // Admin only
            createRule = null,  // System only
            updateRule = null,  // Never
            deleteRule = null   // Admin only
        )
    )
    
    /**
     * All collection schemas
     */
    val allSchemas = listOf(
        profiles,
        keyValues,
        userValues,
        prompts,
        userAnswers,
        matches,
        migrations
    )
}
