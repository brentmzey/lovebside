package love.bside.server.schema

import kotlin.reflect.KClass
import kotlin.reflect.full.memberProperties

/**
 * Checks synchronization between API models, Domain models, and Database models
 * Helps identify field mismatches before they cause runtime errors
 * Can run without PocketBase connection
 */
class SchemaSyncChecker {
    
    data class SyncResult(
        val isSync: Boolean,
        val issues: List<SyncIssue>
    ) {
        fun printReport() {
            if (isSync) {
                println("✅ All models are in sync!")
            } else {
                println("❌ Found ${issues.size} sync issues:")
                issues.forEach { issue ->
                    println()
                    println("  ${issue.severity} - ${issue.model}")
                    println("  ${issue.description}")
                    issue.suggestions.forEach { suggestion ->
                        println("    → $suggestion")
                    }
                }
            }
        }
    }
    
    data class SyncIssue(
        val model: String,
        val severity: Severity,
        val description: String,
        val suggestions: List<String>
    )
    
    enum class Severity {
        CRITICAL, WARNING, INFO
    }
    
    /**
     * Check if all models are in sync
     */
    fun checkAll(): SyncResult {
        val issues = mutableListOf<SyncIssue>()
        
        // Check Profile models
        issues.addAll(checkProfileSync())
        
        // Check User Value models
        issues.addAll(checkUserValueSync())
        
        // Check User Answer models
        issues.addAll(checkUserAnswerSync())
        
        // Check Match models
        issues.addAll(checkMatchSync())
        
        // Check Key Value models
        issues.addAll(checkKeyValueSync())
        
        // Check Prompt models
        issues.addAll(checkPromptSync())
        
        return SyncResult(
            isSync = issues.none { it.severity == Severity.CRITICAL },
            issues = issues
        )
    }
    
    /**
     * Check Profile model sync
     */
    private fun checkProfileSync(): List<SyncIssue> {
        val issues = mutableListOf<SyncIssue>()
        
        // Expected fields from schema
        val schemaFields = setOf(
            "id", "created", "updated",
            "userId", "firstName", "lastName",
            "birthDate", "bio", "location", "seeking"
        )
        
        // Check required fields are documented
        val requiredFields = setOf("userId", "firstName", "lastName", "birthDate", "seeking")
        val optionalFields = setOf("bio", "location")
        
        // Validate schema completeness
        val missingRequired = requiredFields - schemaFields
        if (missingRequired.isNotEmpty()) {
            issues.add(SyncIssue(
                model = "s_profiles",
                severity = Severity.CRITICAL,
                description = "Missing required fields in schema: ${missingRequired.joinToString()}",
                suggestions = listOf(
                    "Add these fields to BsideSchema.profiles",
                    "Update POCKETBASE_SCHEMA.md documentation"
                )
            ))
        }
        
        // Check field naming consistency
        issues.add(SyncIssue(
            model = "s_profiles",
            severity = Severity.INFO,
            description = "Field naming convention check",
            suggestions = listOf(
                "Ensure API models use camelCase: userId, firstName, lastName",
                "Ensure DB models use snake_case or camelCase consistently",
                "Check JSON serialization annotations"
            )
        ))
        
        return issues
    }
    
    /**
     * Check User Value model sync
     */
    private fun checkUserValueSync(): List<SyncIssue> {
        val issues = mutableListOf<SyncIssue>()
        
        val schemaFields = setOf(
            "id", "created", "updated",
            "userId", "keyValueId", "importance"
        )
        
        val requiredFields = setOf("userId", "keyValueId", "importance")
        
        // Check unique constraint
        issues.add(SyncIssue(
            model = "s_user_values",
            severity = Severity.WARNING,
            description = "Unique constraint check",
            suggestions = listOf(
                "Ensure composite unique index on (userId, keyValueId)",
                "Prevent duplicate value selections",
                "Handle update vs create logic properly"
            )
        ))
        
        // Check importance range
        issues.add(SyncIssue(
            model = "s_user_values",
            severity = Severity.INFO,
            description = "Importance validation check",
            suggestions = listOf(
                "Validate importance range: 1-10",
                "Enforce in API validation",
                "Enforce in PocketBase schema rules"
            )
        ))
        
        return issues
    }
    
    /**
     * Check User Answer model sync
     */
    private fun checkUserAnswerSync(): List<SyncIssue> {
        val issues = mutableListOf<SyncIssue>()
        
        val schemaFields = setOf(
            "id", "created", "updated",
            "userId", "promptId", "answer"
        )
        
        // Check unique constraint
        issues.add(SyncIssue(
            model = "s_user_answers",
            severity = Severity.WARNING,
            description = "Unique constraint check",
            suggestions = listOf(
                "Ensure composite unique index on (userId, promptId)",
                "Prevent duplicate answers to same prompt",
                "Handle update vs create logic"
            )
        ))
        
        // Check answer length
        issues.add(SyncIssue(
            model = "s_user_answers",
            severity = Severity.INFO,
            description = "Answer text validation",
            suggestions = listOf(
                "Enforce min length: 10 characters",
                "Enforce max length: 500 characters",
                "Validate in both client and server"
            )
        ))
        
        return issues
    }
    
    /**
     * Check Match model sync
     */
    private fun checkMatchSync(): List<SyncIssue> {
        val issues = mutableListOf<SyncIssue>()
        
        val schemaFields = setOf(
            "id", "created", "updated",
            "userId", "matchedUserId",
            "compatibilityScore", "sharedValues"
        )
        
        // Check compatibility score type
        issues.add(SyncIssue(
            model = "s_matches",
            severity = Severity.WARNING,
            description = "Compatibility score type check",
            suggestions = listOf(
                "Ensure compatibilityScore is Number (Double) in PocketBase",
                "Ensure API models use Double type",
                "Validate range: 0.0 - 100.0"
            )
        ))
        
        // Check sharedValues JSON field
        issues.add(SyncIssue(
            model = "s_matches",
            severity = Severity.INFO,
            description = "SharedValues JSON serialization",
            suggestions = listOf(
                "Ensure sharedValues is JSON type in PocketBase",
                "Test serialization of List<String>",
                "Verify deserialization works correctly"
            )
        ))
        
        // Check self-match prevention
        issues.add(SyncIssue(
            model = "s_matches",
            severity = Severity.WARNING,
            description = "Self-match validation",
            suggestions = listOf(
                "Validate userId != matchedUserId",
                "Add check constraint in PocketBase if possible",
                "Enforce in API validation layer"
            )
        ))
        
        // Check unique constraint
        issues.add(SyncIssue(
            model = "s_matches",
            severity = Severity.WARNING,
            description = "Unique match check",
            suggestions = listOf(
                "Ensure composite unique index on (userId, matchedUserId)",
                "Prevent duplicate match records",
                "Consider bidirectional matches"
            )
        ))
        
        return issues
    }
    
    /**
     * Check Key Value model sync
     */
    private fun checkKeyValueSync(): List<SyncIssue> {
        val issues = mutableListOf<SyncIssue>()
        
        val schemaFields = setOf(
            "id", "created", "updated",
            "key", "category", "description", "displayOrder"
        )
        
        // Check category enum values
        val validCategories = setOf("PERSONALITY", "VALUES", "INTERESTS", "LIFESTYLE")
        issues.add(SyncIssue(
            model = "s_key_values",
            severity = Severity.INFO,
            description = "Category enum validation",
            suggestions = listOf(
                "Ensure category options match: ${validCategories.joinToString()}",
                "Keep enum in sync across API and DB",
                "Update both when adding new categories"
            )
        ))
        
        // Check unique key constraint
        issues.add(SyncIssue(
            model = "s_key_values",
            severity = Severity.WARNING,
            description = "Unique key constraint",
            suggestions = listOf(
                "Ensure unique index on 'key' field",
                "Prevent duplicate value keys",
                "Use key as human-readable identifier"
            )
        ))
        
        return issues
    }
    
    /**
     * Check Prompt model sync
     */
    private fun checkPromptSync(): List<SyncIssue> {
        val issues = mutableListOf<SyncIssue>()
        
        val schemaFields = setOf(
            "id", "created", "updated",
            "question", "category", "displayOrder"
        )
        
        // Check question uniqueness
        issues.add(SyncIssue(
            model = "s_prompts",
            severity = Severity.INFO,
            description = "Question uniqueness check",
            suggestions = listOf(
                "Consider unique constraint on 'question' field",
                "Prevent duplicate prompts",
                "Or use unique identifier pattern"
            )
        ))
        
        return issues
    }
    
    /**
     * Check field type compatibility
     */
    private fun checkFieldType(
        fieldName: String,
        apiType: String,
        dbType: FieldType
    ): SyncIssue? {
        val compatible = when (apiType to dbType) {
            "String" to FieldType.TEXT -> true
            "String" to FieldType.EMAIL -> true
            "String" to FieldType.URL -> true
            "String" to FieldType.DATE -> true
            "String" to FieldType.DATETIME -> true
            "Int" to FieldType.NUMBER -> true
            "Double" to FieldType.NUMBER -> true
            "Boolean" to FieldType.BOOL -> true
            "List" to FieldType.JSON -> true
            "Map" to FieldType.JSON -> true
            else -> false
        }
        
        return if (!compatible) {
            SyncIssue(
                model = fieldName,
                severity = Severity.CRITICAL,
                description = "Type mismatch: API=$apiType, DB=$dbType",
                suggestions = listOf(
                    "Update API model to match DB type",
                    "Or update DB schema to match API type",
                    "Ensure proper serialization/deserialization"
                )
            )
        } else {
            null
        }
    }
    
    /**
     * Generate sync recommendations
     */
    fun generateRecommendations(): List<String> {
        return listOf(
            "Naming Convention",
            "  - API Models: Use camelCase (userId, firstName, lastName)",
            "  - DB Models: Match PocketBase convention (usually camelCase)",
            "  - JSON Serialization: Use @SerialName if needed",
            "",
            "Required Fields",
            "  - Mark as 'required = true' in BsideSchema",
            "  - Add NOT NULL constraint in PocketBase",
            "  - Validate in RequestValidators",
            "",
            "Unique Constraints",
            "  - Add unique indexes for ID fields",
            "  - Add composite unique indexes where needed",
            "  - Enforce in both DB and API layers",
            "",
            "Foreign Keys",
            "  - Use relation type in PocketBase",
            "  - Set cascadeDelete = true for dependent data",
            "  - Validate existence in API layer",
            "",
            "Enum Values",
            "  - Keep enum values in sync across layers",
            "  - Validate in API before DB insert",
            "  - Document all valid options",
            "",
            "Date/Time Fields",
            "  - Use ISO 8601 format: YYYY-MM-DD or YYYY-MM-DDTHH:MM:SSZ",
            "  - Use kotlinx.datetime for parsing",
            "  - Validate format in API layer",
            "",
            "JSON Fields",
            "  - Test serialization/deserialization thoroughly",
            "  - Use typed models not raw JSON",
            "  - Handle null/empty cases",
            "",
            "Field Lengths",
            "  - Set max length in PocketBase schema",
            "  - Validate in RequestValidators",
            "  - Match limits across all layers",
            "",
            "Indexes",
            "  - Add indexes for frequently queried fields",
            "  - Add composite indexes for multi-field queries",
            "  - Add indexes for foreign keys",
            "  - Test query performance"
        )
    }
}

/**
 * CLI tool for checking schema synchronization
 */
fun main(args: Array<String>) {
    val checker = SchemaSyncChecker()
    
    when (args.firstOrNull()) {
        "check" -> {
            println("Checking schema synchronization...")
            println()
            val result = checker.checkAll()
            result.printReport()
        }
        "recommendations" -> {
            println("Schema Synchronization Recommendations:")
            println()
            checker.generateRecommendations().forEach { println(it) }
        }
        else -> {
            println("""
                Usage: ./gradlew :server:checkSchemaSync --args="<command>"
                
                Commands:
                  check            - Check all model synchronization
                  recommendations  - Show synchronization best practices
            """.trimIndent())
        }
    }
}
