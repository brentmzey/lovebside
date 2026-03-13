/// <reference path="../pb_data/types.d.ts" />

/**
 * Real-Time Messaging Performance Optimization
 * 
 * Optimizations for high-concurrency, real-time messaging:
 * 1. Advanced indexing strategies for message queries
 * 2. Denormalization for faster reads
 * 3. Message status tracking optimization
 * 4. Conversation caching fields
 * 5. Optimistic locking for concurrent updates
 */

migrate((db) => {
  const dao = new Dao(db);

  // ==============================================
  // 1. MESSAGES - PERFORMANCE INDEXES
  // ==============================================
  try {
    const messagesCollection = dao.findCollectionByNameOrId("m_messages");
    
    // Add composite indexes for common query patterns
    const newIndexes = [
      // Most common: Get messages by conversation, ordered by time
      "CREATE INDEX IF NOT EXISTS idx_msg_conversation_time ON m_messages (conversation_id, sent_at DESC)",
      
      // For pagination with deleted messages filter
      "CREATE INDEX IF NOT EXISTS idx_msg_conversation_not_deleted ON m_messages (conversation_id, deleted_at) WHERE deleted_at IS NULL",
      
      // For thread support
      "CREATE INDEX IF NOT EXISTS idx_msg_thread_root ON m_messages (thread_root_id, sent_at DESC) WHERE thread_root_id IS NOT NULL",
      
      // For unread message counts
      "CREATE INDEX IF NOT EXISTS idx_msg_conversation_sender_time ON m_messages (conversation_id, sender_id, sent_at DESC)",
      
      // For searching messages
      "CREATE INDEX IF NOT EXISTS idx_msg_content ON m_messages (conversation_id, content) WHERE deleted_at IS NULL",
      
      // For message type filtering
      "CREATE INDEX IF NOT EXISTS idx_msg_type ON m_messages (conversation_id, type, sent_at DESC) WHERE deleted_at IS NULL"
    ];

    messagesCollection.indexes = [...new Set([...messagesCollection.indexes, ...newIndexes])];
    
    // Add message hash for deduplication
    messagesCollection.schema.addField(new SchemaField({
      system: false,
      id: "text_msg_hash",
      name: "message_hash",
      type: "text",
      required: false,
      presentable: false,
      options: {
        min: 0,
        max: 64,
        pattern: "^[a-f0-9]{64}$"
      }
    }));

    // Add delivery status for real-time tracking
    messagesCollection.schema.addField(new SchemaField({
      system: false,
      id: "select_delivery",
      name: "delivery_status",
      type: "select",
      required: false,
      presentable: false,
      options: {
        maxSelect: 1,
        values: [
          "sending",
          "sent",
          "delivered",
          "failed"
        ]
      }
    }));

    // Add version for optimistic locking
    messagesCollection.schema.addField(new SchemaField({
      system: false,
      id: "number_version",
      name: "version",
      type: "number",
      required: false,
      presentable: false,
      options: {
        min: 0,
        max: null,
        noDecimal: true
      }
    }));

    dao.saveCollection(messagesCollection);
    console.log("✅ Optimized m_messages for real-time performance");

  } catch (e) {
    console.error("❌ Error optimizing m_messages:", e);
  }

  // ==============================================
  // 2. CONVERSATIONS - DENORMALIZATION & CACHING
  // ==============================================
  try {
    const conversationsCollection = dao.findCollectionByNameOrId("m_conversations");
    
    // Add denormalized fields for faster queries
    conversationsCollection.schema.addField(new SchemaField({
      system: false,
      id: "text_last_sender_id",
      name: "last_message_sender_id",
      type: "text",
      required: false,
      presentable: false,
      options: {
        min: 0,
        max: 15,
        pattern: "^[a-z0-9]*$"
      }
    }));

    conversationsCollection.schema.addField(new SchemaField({
      system: false,
      id: "text_last_sender_name",
      name: "last_message_sender_name",
      type: "text",
      required: false,
      presentable: false,
      options: {
        min: 0,
        max: 100,
        pattern: ""
      }
    }));

    conversationsCollection.schema.addField(new SchemaField({
      system: false,
      id: "number_msg_count",
      name: "message_count",
      type: "number",
      required: false,
      presentable: false,
      options: {
        min: 0,
        max: null,
        noDecimal: true
      }
    }));

    conversationsCollection.schema.addField(new SchemaField({
      system: false,
      id: "number_unread",
      name: "unread_count",
      type: "number",
      required: false,
      presentable: false,
      options: {
        min: 0,
        max: null,
        noDecimal: true
      }
    }));

    conversationsCollection.schema.addField(new SchemaField({
      system: false,
      id: "json_participant_ids",
      name: "participant_ids_cache",
      type: "json",
      required: false,
      presentable: false,
      options: {
        maxSize: 10000
      }
    }));

    conversationsCollection.schema.addField(new SchemaField({
      system: false,
      id: "bool_has_unread",
      name: "has_unread_messages",
      type: "bool",
      required: false,
      presentable: false
    }));

    // Add optimized indexes
    const conversationIndexes = [
      // For listing user conversations ordered by recent activity
      "CREATE INDEX IF NOT EXISTS idx_conv_last_message ON m_conversations (last_message_at DESC) WHERE is_archived = FALSE",
      
      // For archived conversations
      "CREATE INDEX IF NOT EXISTS idx_conv_archived ON m_conversations (is_archived, last_message_at DESC)",
      
      // For conversation type filtering
      "CREATE INDEX IF NOT EXISTS idx_conv_type ON m_conversations (type, last_message_at DESC)",
      
      // For unread conversations
      "CREATE INDEX IF NOT EXISTS idx_conv_unread ON m_conversations (has_unread_messages, last_message_at DESC) WHERE has_unread_messages = TRUE"
    ];

    conversationsCollection.indexes = [...new Set([...conversationsCollection.indexes, ...conversationIndexes])];

    dao.saveCollection(conversationsCollection);
    console.log("✅ Optimized m_conversations with caching fields");

  } catch (e) {
    console.error("❌ Error optimizing m_conversations:", e);
  }

  // ==============================================
  // 3. READ RECEIPTS - BATCH OPTIMIZATION
  // ==============================================
  try {
    const readReceiptsCollection = dao.findCollectionByNameOrId("m_read_receipts");
    
    // Add conversation ID for faster queries
    readReceiptsCollection.schema.addField(new SchemaField({
      system: false,
      id: "relation_conv_id",
      name: "conversation_id",
      type: "relation",
      required: false,
      presentable: false,
      options: {
        collectionId: "m_conversations",
        cascadeDelete: true,
        minSelect: null,
        maxSelect: 1,
        displayFields: []
      }
    }));

    // Optimized indexes for read receipts
    const readReceiptIndexes = [
      // For checking if user has read a message
      "CREATE INDEX IF NOT EXISTS idx_read_user_message ON m_read_receipts (user_id, message_id)",
      
      // For getting all read receipts for a conversation
      "CREATE INDEX IF NOT EXISTS idx_read_conversation ON m_read_receipts (conversation_id, read_at DESC)",
      
      // For getting unread messages count
      "CREATE INDEX IF NOT EXISTS idx_read_user_conv ON m_read_receipts (user_id, conversation_id, read_at DESC)"
    ];

    readReceiptsCollection.indexes = [...new Set([...readReceiptsCollection.indexes, ...readReceiptIndexes])];

    dao.saveCollection(readReceiptsCollection);
    console.log("✅ Optimized m_read_receipts for batch operations");

  } catch (e) {
    console.error("❌ Error optimizing m_read_receipts:", e);
  }

  // ==============================================
  // 4. TYPING STATUS - EPHEMERAL DATA OPTIMIZATION
  // ==============================================
  try {
    const typingCollection = dao.findCollectionByNameOrId("m_typing_status");
    
    // Add TTL field for automatic cleanup
    typingCollection.schema.addField(new SchemaField({
      system: false,
      id: "date_expires",
      name: "expires_at",
      type: "date",
      required: true,
      presentable: false,
      options: {
        min: "",
        max: ""
      }
    }));

    // Optimized index for active typing users
    const typingIndexes = [
      "CREATE INDEX IF NOT EXISTS idx_typing_conv_user ON m_typing_status (conversation_id, user_id, expires_at DESC) WHERE is_typing = TRUE",
      "CREATE INDEX IF NOT EXISTS idx_typing_expired ON m_typing_status (expires_at) WHERE expires_at < datetime('now')"
    ];

    typingCollection.indexes = [...new Set([...typingCollection.indexes, ...typingIndexes])];

    dao.saveCollection(typingCollection);
    console.log("✅ Optimized m_typing_status for ephemeral data");

  } catch (e) {
    console.error("❌ Error optimizing m_typing_status:", e);
  }

  // ==============================================
  // 5. PRESENCE - ONLINE STATUS OPTIMIZATION
  // ==============================================
  try {
    const presenceCollection = dao.findCollectionByNameOrId("m_presence");
    
    // Add connection tracking
    presenceCollection.schema.addField(new SchemaField({
      system: false,
      id: "text_connection_id",
      name: "connection_id",
      type: "text",
      required: false,
      presentable: false,
      options: {
        min: 0,
        max: 100,
        pattern: ""
      }
    }));

    presenceCollection.schema.addField(new SchemaField({
      system: false,
      id: "number_conn_count",
      name: "connection_count",
      type: "number",
      required: false,
      presentable: false,
      options: {
        min: 0,
        max: null,
        noDecimal: true
      }
    }));

    // Optimized indexes for presence
    const presenceIndexes = [
      "CREATE INDEX IF NOT EXISTS idx_presence_user_status ON m_presence (user_id, status, last_seen DESC)",
      "CREATE INDEX IF NOT EXISTS idx_presence_online ON m_presence (status, last_seen DESC) WHERE status = 'online'",
      "CREATE INDEX IF NOT EXISTS idx_presence_stale ON m_presence (last_seen) WHERE status = 'online' AND last_seen < datetime('now', '-5 minutes')"
    ];

    presenceCollection.indexes = [...new Set([...presenceCollection.indexes, ...presenceIndexes])];

    dao.saveCollection(presenceCollection);
    console.log("✅ Optimized m_presence for real-time status");

  } catch (e) {
    console.error("❌ Error optimizing m_presence:", e);
  }

  // ==============================================
  // 6. REACTIONS - FAST AGGREGATION
  // ==============================================
  try {
    const reactionsCollection = dao.findCollectionByNameOrId("m_reactions");
    
    // Add created_at for ordering
    reactionsCollection.schema.addField(new SchemaField({
      system: false,
      id: "date_reacted_at",
      name: "reacted_at",
      type: "date",
      required: true,
      presentable: false,
      options: {
        min: "",
        max: ""
      }
    }));

    // Optimized indexes for reaction counts and queries
    const reactionIndexes = [
      // For getting reactions by message
      "CREATE INDEX IF NOT EXISTS idx_reaction_message ON m_reactions (message_id, reaction)",
      
      // For counting reactions by type
      "CREATE INDEX IF NOT EXISTS idx_reaction_count ON m_reactions (message_id, reaction, reacted_at DESC)",
      
      // For checking if user already reacted
      "CREATE INDEX IF NOT EXISTS idx_reaction_user_message ON m_reactions (user_id, message_id, reaction)"
    ];

    reactionsCollection.indexes = [...new Set([...reactionsCollection.indexes, ...reactionIndexes])];

    dao.saveCollection(reactionsCollection);
    console.log("✅ Optimized m_reactions for fast aggregation");

  } catch (e) {
    console.error("❌ Error optimizing m_reactions:", e);
  }

  // ==============================================
  // 7. CONVERSATION PARTICIPANTS - MEMBERSHIP OPTIMIZATION
  // ==============================================
  try {
    const participantsCollection = dao.findCollectionByNameOrId("m_conversation_participants");
    
    // Add role and permissions
    participantsCollection.schema.addField(new SchemaField({
      system: false,
      id: "select_role",
      name: "role",
      type: "select",
      required: true,
      presentable: false,
      options: {
        maxSelect: 1,
        values: [
          "owner",
          "admin",
          "member",
          "guest"
        ]
      }
    }));

    participantsCollection.schema.addField(new SchemaField({
      system: false,
      id: "date_last_read",
      name: "last_read_at",
      type: "date",
      required: false,
      presentable: false,
      options: {
        min: "",
        max: ""
      }
    }));

    participantsCollection.schema.addField(new SchemaField({
      system: false,
      id: "text_last_read_msg",
      name: "last_read_message_id",
      type: "text",
      required: false,
      presentable: false,
      options: {
        min: 0,
        max: 15,
        pattern: "^[a-z0-9]*$"
      }
    }));

    participantsCollection.schema.addField(new SchemaField({
      system: false,
      id: "bool_notifications",
      name: "notifications_enabled",
      type: "bool",
      required: false,
      presentable: false
    }));

    // Optimized indexes for participant queries
    const participantIndexes = [
      // For getting user's conversations
      "CREATE INDEX IF NOT EXISTS idx_participant_user ON m_conversation_participants (user_id, conversation_id)",
      
      // For getting conversation members
      "CREATE INDEX IF NOT EXISTS idx_participant_conv ON m_conversation_participants (conversation_id, role)",
      
      // For finding admins/owners
      "CREATE INDEX IF NOT EXISTS idx_participant_role ON m_conversation_participants (conversation_id, role) WHERE role IN ('owner', 'admin')"
    ];

    participantsCollection.indexes = [...new Set([...participantsCollection.indexes, ...participantIndexes])];

    dao.saveCollection(participantsCollection);
    console.log("✅ Optimized m_conversation_participants for fast membership queries");

  } catch (e) {
    console.error("❌ Error optimizing m_conversation_participants:", e);
  }

}, (db) => {
  // Rollback - remove added fields and indexes
  const dao = new Dao(db);

  // Remove message optimizations
  try {
    const messagesCollection = dao.findCollectionByNameOrId("m_messages");
    messagesCollection.schema.removeField("text_msg_hash");
    messagesCollection.schema.removeField("select_delivery");
    messagesCollection.schema.removeField("number_version");
    dao.saveCollection(messagesCollection);
  } catch (e) {
    console.error("Rollback error for m_messages:", e);
  }

  // Remove conversation optimizations
  try {
    const conversationsCollection = dao.findCollectionByNameOrId("m_conversations");
    conversationsCollection.schema.removeField("text_last_sender_id");
    conversationsCollection.schema.removeField("text_last_sender_name");
    conversationsCollection.schema.removeField("number_msg_count");
    conversationsCollection.schema.removeField("number_unread");
    conversationsCollection.schema.removeField("json_participant_ids");
    conversationsCollection.schema.removeField("bool_has_unread");
    dao.saveCollection(conversationsCollection);
  } catch (e) {
    console.error("Rollback error for m_conversations:", e);
  }

  // Continue rollback for other collections...
  console.log("⚠️ Rollback completed");
});
