package love.bside.app.data

/**
 * Static enumeration of all database tables/collections used by the BSide app.
 * 
 * This centralized collection naming makes it easier to:
 * - Maintain consistency across the codebase
 * - Migrate to different database backends (Postgres, DynamoDB, MongoDB, etc.)
 * - Track all data entities in one place
 * - Avoid typos in collection/table names
 * 
 * Naming Convention:
 * - m_ = Messaging domain
 * - s_ = Social/Profile domain
 * - t_ = Technical/System tables
 * - p_ = Proust questionnaire domain
 */
object DatabaseCollections {
    

    
    // ===== Core System Collections =====
    
    /** PocketBase built-in users collection - auth and user management */
    const val USERS = "t_user"

    /** User-specific property key-value storage for distributed caching */
    const val T_USER_PROPERTY = "t_user_property"
    
    /** Tenant-specific property key-value storage for multi-tenant features */
    const val T_TENANT_PROPERTY = "t_tenant_property"
    
    
    // ===== Messaging Domain (m_) =====
    
    /** Conversations between users */
    const val M_CONVERSATIONS = "m_conversations"
    
    /** Messages within conversations */
    const val M_MESSAGES = "m_messages"
    
    /** Conversation participants (many-to-many relationship) */
    const val M_CONVERSATION_PARTICIPANTS = "m_conversation_participants"
    
    /** Real-time typing status indicators */
    const val M_TYPING_STATUS = "m_typing_status"

    /** Message read receipts */
    const val M_READ_RECEIPTS = "m_read_receipts"
    
    
    // ===== Social/Profile Domain (s_) =====
    
    /** User profiles with personal information, photos, and preferences */
    const val S_PROFILES = "s_profiles"
    
    
    // ===== Matching Domain (m_) =====
    
    /** Matches between users with match scores and status */
    const val M_MATCHES = "m_matches"
    
    
    // ===== Proust Questionnaire Domain (p_ and t_) =====
    
    /** Proust questionnaire definitions */
    const val T_PROUST_QUESTIONNAIRE = "t_proust_questionnaire"
    
    /** Proust questionnaire questions */
    const val T_PROUST_QUESTION = "t_proust_question"
    
    /** User responses to questionnaire questions */
    const val T_USER_QUESTIONNAIRE_RESPONSES = "t_user_questionnaire_responses"
    
    
    // ===== Helper Functions =====
    
    /**
     * Get all collection names as a list.
     * Useful for migrations, testing, or administrative operations.
     */
    fun all(): List<String> = listOf(
        USERS,
        T_USER_PROPERTY,
        T_TENANT_PROPERTY,
        M_CONVERSATIONS,
        M_MESSAGES,
        M_CONVERSATION_PARTICIPANTS,
        M_TYPING_STATUS,
        M_READ_RECEIPTS,
        S_PROFILES,
        M_MATCHES,
        T_PROUST_QUESTIONNAIRE,
        T_PROUST_QUESTION,
        T_USER_QUESTIONNAIRE_RESPONSES
    )
    
    /**
     * Get all messaging-related collections.
     */
    fun messagingCollections(): List<String> = listOf(
        M_CONVERSATIONS,
        M_MESSAGES,
        M_CONVERSATION_PARTICIPANTS,
        M_TYPING_STATUS,
        M_READ_RECEIPTS
    )
    
    /**
     * Get all profile and matching-related collections.
     */
    fun socialCollections(): List<String> = listOf(
        S_PROFILES,
        M_MATCHES
    )
    
    /**
     * Get all questionnaire-related collections.
     */
    fun questionnaireCollections(): List<String> = listOf(
        T_PROUST_QUESTIONNAIRE,
        T_PROUST_QUESTION,
        T_USER_QUESTIONNAIRE_RESPONSES
    )
    
    /**
     * Get all system/technical collections.
     */
    fun systemCollections(): List<String> = listOf(
        USERS,
        T_USER_PROPERTY,
        T_TENANT_PROPERTY
    )
    
    /**
     * Validate if a collection name is known to the system.
     */
    fun isValid(collectionName: String): Boolean = 
        collectionName in all() || collectionName == "users" // Allow "users" internal alias
    
    /**
     * Get collection name by domain and entity.
     * This can be useful for programmatic access.
     */
    fun get(domain: Domain, entity: Entity): String = when (domain) {
        Domain.MESSAGING -> when (entity) {
            Entity.CONVERSATIONS -> M_CONVERSATIONS
            Entity.MESSAGES -> M_MESSAGES
            Entity.PARTICIPANTS -> M_CONVERSATION_PARTICIPANTS
            Entity.TYPING_STATUS -> M_TYPING_STATUS
            Entity.READ_RECEIPTS -> M_READ_RECEIPTS
            else -> throw IllegalArgumentException("Invalid messaging entity: $entity")
        }
        Domain.SOCIAL -> when (entity) {
            Entity.PROFILES -> S_PROFILES
            Entity.MATCHES -> M_MATCHES
            else -> throw IllegalArgumentException("Invalid social entity: $entity")
        }
        Domain.QUESTIONNAIRE -> when (entity) {
            Entity.QUESTIONNAIRES -> T_PROUST_QUESTIONNAIRE
            Entity.QUESTIONS -> T_PROUST_QUESTION
            Entity.RESPONSES -> T_USER_QUESTIONNAIRE_RESPONSES
            else -> throw IllegalArgumentException("Invalid questionnaire entity: $entity")
        }
        Domain.SYSTEM -> when (entity) {
            Entity.USERS -> USERS
            Entity.USER_PROPERTIES -> T_USER_PROPERTY
            Entity.TENANT_PROPERTIES -> T_TENANT_PROPERTY
            else -> throw IllegalArgumentException("Invalid system entity: $entity")
        }
    }
    
    /**
     * Domain categories for logical grouping.
     */
    enum class Domain {
        MESSAGING,
        SOCIAL,
        QUESTIONNAIRE,
        SYSTEM
    }
    
    /**
     * Entity types within domains.
     */
    enum class Entity {
        // Messaging
        CONVERSATIONS,
        MESSAGES,
        PARTICIPANTS,
        TYPING_STATUS,
        READ_RECEIPTS,
        
        // Social
        PROFILES,
        MATCHES,
        
        // Questionnaire
        QUESTIONNAIRES,
        QUESTIONS,
        RESPONSES,
        
        // System
        USERS,
        USER_PROPERTIES,
        TENANT_PROPERTIES
    }
}

/**
 * Extension function for convenient collection access in repositories.
 * 
 * Example usage:
 * ```
 * pocketBase.collection(DatabaseCollections.M_MESSAGES).getList(...)
 * ```
 */
fun String.Companion.collection(name: String): String {
    require(DatabaseCollections.isValid(name)) { 
        "Unknown collection name: $name. Use DatabaseCollections constants." 
    }
    return name
}
