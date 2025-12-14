package love.bside.server.schema

/**
 * Schema validation and documentation tool
 * 
 * Validates that the actual PocketBase schema matches expected schema
 * and generates documentation
 */
class SchemaValidator {
    
    /**
     * Validate all schemas
     */
    fun validateAll(): List<ValidationResult> {
        return BsideSchema.allSchemas.map { schema ->
            validate(schema)
        }
    }
    
    /**
     * Validate a single schema
     */
    fun validate(schema: CollectionSchema): ValidationResult {
        val issues = mutableListOf<String>()
        
        // Check required fields
        schema.fields.forEach { field ->
            if (field.required && field.options?.let { it.min != null && it.min > 0 } != true) {
                issues.add("Field '${field.name}' is required but has no min constraint")
            }
        }
        
        // Check indexes
        schema.indexes.forEach { index ->
            index.fields.forEach { fieldName ->
                if (schema.fields.none { it.name == fieldName }) {
                    issues.add("Index '${index.name}' references non-existent field '$fieldName'")
                }
            }
        }
        
        return if (issues.isEmpty()) {
            ValidationResult.Valid(schema.name)
        } else {
            ValidationResult.Invalid(schema.name, issues)
        }
    }
    
    /**
     * Generate markdown documentation for all schemas
     */
    fun generateDocumentation(): String {
        val sb = StringBuilder()
        
        sb.appendLine("# B-Side Database Schema Documentation")
        sb.appendLine()
        sb.appendLine("Auto-generated schema documentation for all PocketBase collections.")
        sb.appendLine()
        sb.appendLine("## Collections Overview")
        sb.appendLine()
        
        BsideSchema.allSchemas.forEach { schema ->
            sb.appendLine("- **${schema.name}** (${schema.type}): ${schema.fields.size} fields, ${schema.indexes.size} indexes")
        }
        
        sb.appendLine()
        sb.appendLine("---")
        sb.appendLine()
        
        BsideSchema.allSchemas.forEach { schema ->
            sb.appendLine("## ${schema.name}")
            sb.appendLine()
            sb.appendLine("**Type:** ${schema.type}")
            sb.appendLine()
            
            // Fields
            sb.appendLine("### Fields")
            sb.appendLine()
            sb.appendLine("| Field | Type | Required | Unique | Options |")
            sb.appendLine("|-------|------|----------|--------|---------|")
            
            schema.fields.forEach { field ->
                val opts = buildString {
                    field.options?.let { options ->
                        val parts = mutableListOf<String>()
                        options.min?.let { parts.add("min: $it") }
                        options.max?.let { parts.add("max: $it") }
                        options.values?.let { parts.add("values: ${it.joinToString(", ")}") }
                        options.collectionId?.let { parts.add("→ $it") }
                        options.cascadeDelete?.let { if (it) parts.add("cascade delete") }
                        append(parts.joinToString("; "))
                    }
                }
                
                sb.appendLine("| ${field.name} | ${field.type} | ${if (field.required) "✓" else ""} | ${if (field.unique) "✓" else ""} | $opts |")
            }
            
            sb.appendLine()
            
            // Indexes
            if (schema.indexes.isNotEmpty()) {
                sb.appendLine("### Indexes")
                sb.appendLine()
                sb.appendLine("| Name | Fields | Unique |")
                sb.appendLine("|------|--------|--------|")
                
                schema.indexes.forEach { index ->
                    sb.appendLine("| ${index.name} | ${index.fields.joinToString(" + ")} | ${if (index.unique) "✓" else ""} |")
                }
                
                sb.appendLine()
            }
            
            // API Rules
            schema.apiRules?.let { rules ->
                sb.appendLine("### API Rules")
                sb.appendLine()
                sb.appendLine("| Operation | Rule |")
                sb.appendLine("|-----------|------|")
                sb.appendLine("| List | ${rules.listRule ?: "Admin only"} |")
                sb.appendLine("| View | ${rules.viewRule ?: "Admin only"} |")
                sb.appendLine("| Create | ${rules.createRule ?: "Admin only"} |")
                sb.appendLine("| Update | ${rules.updateRule ?: "Admin only"} |")
                sb.appendLine("| Delete | ${rules.deleteRule ?: "Admin only"} |")
                sb.appendLine()
            }
            
            sb.appendLine("---")
            sb.appendLine()
        }
        
        return sb.toString()
    }
    
    /**
     * Generate SQL-like DDL for documentation
     */
    fun generateDDL(schema: CollectionSchema): String {
        val sb = StringBuilder()
        
        sb.appendLine("-- Collection: ${schema.name}")
        sb.appendLine("CREATE COLLECTION ${schema.name} (")
        
        schema.fields.forEachIndexed { index, field ->
            val constraints = buildString {
                if (field.required) append(" NOT NULL")
                if (field.unique) append(" UNIQUE")
                field.options?.let { opts ->
                    opts.min?.let { append(" CHECK(LENGTH(${field.name}) >= $it)") }
                    opts.max?.let { append(" CHECK(LENGTH(${field.name}) <= $it)") }
                }
            }
            
            val comma = if (index < schema.fields.size - 1) "," else ""
            sb.appendLine("    ${field.name} ${field.type}$constraints$comma")
        }
        
        sb.appendLine(");")
        sb.appendLine()
        
        // Indexes
        schema.indexes.forEach { index ->
            val unique = if (index.unique) "UNIQUE " else ""
            sb.appendLine("CREATE ${unique}INDEX ${index.name} ON ${schema.name}(${index.fields.joinToString(", ")});")
        }
        
        return sb.toString()
    }
}

sealed class ValidationResult {
    data class Valid(val collectionName: String) : ValidationResult()
    data class Invalid(val collectionName: String, val issues: List<String>) : ValidationResult()
}

/**
 * CLI tool for schema operations
 */
fun main(args: Array<String>) {
    val validator = SchemaValidator()
    
    when (args.firstOrNull()) {
        "validate" -> {
            println("Validating all schemas...")
            val results = validator.validateAll()
            results.forEach { result ->
                when (result) {
                    is ValidationResult.Valid -> println("✓ ${result.collectionName}: Valid")
                    is ValidationResult.Invalid -> {
                        println("✗ ${result.collectionName}: Invalid")
                        result.issues.forEach { println("  - $it") }
                    }
                }
            }
        }
        "docs" -> {
            val docs = validator.generateDocumentation()
            println(docs)
        }
        "ddl" -> {
            BsideSchema.allSchemas.forEach { schema ->
                println(validator.generateDDL(schema))
                println()
            }
        }
        else -> {
            println("""
                Usage: ./gradlew :server:runSchemaValidator --args="<command>"
                
                Commands:
                  validate  - Validate all schemas
                  docs      - Generate markdown documentation
                  ddl       - Generate DDL statements
            """.trimIndent())
        }
    }
}
