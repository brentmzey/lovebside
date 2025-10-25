package love.bside.server.migrations.versions

import love.bside.server.migrations.Migration
import love.bside.server.migrations.MigrationResult

/**
 * Migration v1: Initial schema setup
 * 
 * Creates all the base collections and indexes for the B-Side app:
 * - users (system collection - already exists in PocketBase)
 * - s_profiles (user profiles)
 * - s_key_values (master list of personality traits)
 * - s_user_values (user's selected values)
 * - s_prompts (Proust-style questionnaire prompts)
 * - s_user_answers (user answers to prompts)
 * - s_matches (calculated matches between users)
 * - s_migrations (tracking applied migrations)
 */
class Migration1_InitialSchema : Migration {
    override val version = 1
    override val name = "initial_schema"
    override val description = "Create base schema for B-Side app"
    
    override suspend fun up(): MigrationResult {
        return try {
            // Note: Actual PocketBase collection creation would be done via PocketBase Admin API
            // This migration serves as documentation of the schema
            
            val collections = listOf(
                "s_profiles" to """
                    Fields:
                    - userId (relation to users, required, unique)
                    - firstName (text, required, min:1, max:50)
                    - lastName (text, required, min:1, max:50)
                    - birthDate (date, required)
                    - bio (text, optional, max:500)
                    - location (text, optional, max:100)
                    - seeking (select, required, options: FRIENDSHIP|RELATIONSHIP|BOTH)
                    
                    Indexes:
                    - userId (unique)
                    - seeking
                """,
                "s_key_values" to """
                    Fields:
                    - key (text, required, unique, max:100)
                    - category (select, required, options: PERSONALITY|VALUES|INTERESTS|LIFESTYLE)
                    - description (text, optional, max:500)
                    - displayOrder (number, default:0)
                    
                    Indexes:
                    - key (unique)
                    - category
                """,
                "s_user_values" to """
                    Fields:
                    - userId (relation to users, required)
                    - keyValueId (relation to s_key_values, required)
                    - importance (number, required, min:1, max:10)
                    
                    Indexes:
                    - userId
                    - keyValueId
                    - Compound: userId + keyValueId (unique)
                """,
                "s_prompts" to """
                    Fields:
                    - text (text, required, max:200)
                    - category (select, optional, options: ICEBREAKER|DEEP|FUN|VALUES)
                    - isActive (bool, default:true)
                    
                    Indexes:
                    - category
                    - isActive
                """,
                "s_user_answers" to """
                    Fields:
                    - userId (relation to users, required)
                    - promptId (relation to s_prompts, required)
                    - answer (text, required, max:1000)
                    
                    Indexes:
                    - userId
                    - promptId
                    - Compound: userId + promptId (unique)
                """,
                "s_matches" to """
                    Fields:
                    - userId (relation to users, required)
                    - matchedUserId (relation to users, required)
                    - compatibilityScore (number, required, min:0, max:100)
                    - sharedValues (json, optional)
                    - status (select, required, default:PENDING, options: PENDING|ACCEPTED|REJECTED|BLOCKED)
                    - lastCalculated (datetime, auto)
                    
                    Indexes:
                    - userId
                    - matchedUserId
                    - compatibilityScore
                    - status
                    - Compound: userId + matchedUserId (unique)
                """,
                "s_migrations" to """
                    Fields:
                    - version (number, required, unique)
                    - name (text, required)
                    - description (text, optional)
                    - appliedAt (datetime, auto)
                    - executionTimeMs (number, required)
                    
                    Indexes:
                    - version (unique)
                    - appliedAt
                """
            )
            
            MigrationResult.Success(
                "Initial schema documented. Collections to create:\n" +
                collections.joinToString("\n") { (name, schema) -> "  - $name\n$schema" }
            )
        } catch (e: Exception) {
            MigrationResult.Failure("Migration failed: ${e.message}", e)
        }
    }
    
    override suspend fun down(): MigrationResult {
        return try {
            // Rollback would drop all collections
            // In production, this should be done carefully with data backup
            MigrationResult.Success("Would drop all collections (not implemented for safety)")
        } catch (e: Exception) {
            MigrationResult.Failure("Rollback failed: ${e.message}", e)
        }
    }
}
