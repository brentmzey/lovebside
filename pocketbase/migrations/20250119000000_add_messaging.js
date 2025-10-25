// Migration: Add real-time messaging schema
// Date: 2025-01-19
// Prefix: m_ (messaging)

migrate((db) => {
  const dao = new Dao(db);

  // ============================================================================
  // Collection: m_conversations
  // Purpose: Tracks 1-to-1 conversations between users
  // Real-time: Yes (subscribed by both participants)
  // ============================================================================
  
  const conversationsCollection = new Collection({
    "name": "m_conversations",
    "type": "base",
    "system": false,
    "schema": [
      {
        "name": "participant1Id",
        "type": "relation",
        "required": true,
        "options": {
          "collectionId": "_pb_users_auth_",
          "cascadeDelete": true,
          "min": 1,
          "max": 1,
          "displayFields": ["username"]
        }
      },
      {
        "name": "participant2Id",
        "type": "relation",
        "required": true,
        "options": {
          "collectionId": "_pb_users_auth_",
          "cascadeDelete": true,
          "min": 1,
          "max": 1,
          "displayFields": ["username"]
        }
      },
      {
        "name": "lastMessageText",
        "type": "text",
        "required": false
      },
      {
        "name": "lastMessageAt",
        "type": "date",
        "required": false
      },
      {
        "name": "participant1UnreadCount",
        "type": "number",
        "required": true,
        "options": {
          "min": 0,
          "max": null
        }
      },
      {
        "name": "participant2UnreadCount",
        "type": "number",
        "required": true,
        "options": {
          "min": 0,
          "max": null
        }
      },
      {
        "name": "participant1LastReadAt",
        "type": "date",
        "required": false
      },
      {
        "name": "participant2LastReadAt",
        "type": "date",
        "required": false
      }
    ],
    "indexes": [
      "CREATE UNIQUE INDEX idx_conversation_participants ON m_conversations (participant1Id, participant2Id)",
      "CREATE INDEX idx_conversation_lastMessage ON m_conversations (lastMessageAt DESC)",
      "CREATE INDEX idx_conversation_participant1 ON m_conversations (participant1Id)",
      "CREATE INDEX idx_conversation_participant2 ON m_conversations (participant2Id)"
    ],
    "listRule": "@request.auth.id != '' && (@request.auth.id = participant1Id || @request.auth.id = participant2Id)",
    "viewRule": "@request.auth.id != '' && (@request.auth.id = participant1Id || @request.auth.id = participant2Id)",
    "createRule": "@request.auth.id != '' && (@request.auth.id = participant1Id || @request.auth.id = participant2Id)",
    "updateRule": "@request.auth.id != '' && (@request.auth.id = participant1Id || @request.auth.id = participant2Id)",
    "deleteRule": null  // Only admins can delete conversations
  });
  
  dao.saveCollection(conversationsCollection);

  // ============================================================================
  // Collection: m_messages
  // Purpose: Individual messages within conversations
  // Real-time: Yes (subscribed by conversation participants)
  // ============================================================================
  
  const messagesCollection = new Collection({
    "name": "m_messages",
    "type": "base",
    "system": false,
    "schema": [
      {
        "name": "conversationId",
        "type": "relation",
        "required": true,
        "options": {
          "collectionId": conversationsCollection.id,
          "cascadeDelete": true,
          "min": 1,
          "max": 1
        }
      },
      {
        "name": "senderId",
        "type": "relation",
        "required": true,
        "options": {
          "collectionId": "_pb_users_auth_",
          "cascadeDelete": false,
          "min": 1,
          "max": 1,
          "displayFields": ["username"]
        }
      },
      {
        "name": "receiverId",
        "type": "relation",
        "required": true,
        "options": {
          "collectionId": "_pb_users_auth_",
          "cascadeDelete": false,
          "min": 1,
          "max": 1,
          "displayFields": ["username"]
        }
      },
      {
        "name": "content",
        "type": "text",
        "required": true,
        "options": {
          "min": 1,
          "max": 5000
        }
      },
      {
        "name": "messageType",
        "type": "select",
        "required": true,
        "options": {
          "maxSelect": 1,
          "values": [
            "text",
            "image",
            "system"
          ]
        }
      },
      {
        "name": "status",
        "type": "select",
        "required": true,
        "options": {
          "maxSelect": 1,
          "values": [
            "sending",
            "sent",
            "delivered",
            "read",
            "failed"
          ]
        }
      },
      {
        "name": "sentAt",
        "type": "date",
        "required": true
      },
      {
        "name": "deliveredAt",
        "type": "date",
        "required": false
      },
      {
        "name": "readAt",
        "type": "date",
        "required": false
      },
      {
        "name": "editedAt",
        "type": "date",
        "required": false
      },
      {
        "name": "deletedAt",
        "type": "date",
        "required": false
      }
    ],
    "indexes": [
      "CREATE INDEX idx_message_conversation ON m_messages (conversationId, sentAt DESC)",
      "CREATE INDEX idx_message_sender ON m_messages (senderId, sentAt DESC)",
      "CREATE INDEX idx_message_receiver ON m_messages (receiverId, sentAt DESC)",
      "CREATE INDEX idx_message_status ON m_messages (status)",
      "CREATE INDEX idx_message_unread ON m_messages (receiverId, status, sentAt DESC)"
    ],
    "listRule": "@request.auth.id != '' && (@request.auth.id = senderId || @request.auth.id = receiverId)",
    "viewRule": "@request.auth.id != '' && (@request.auth.id = senderId || @request.auth.id = receiverId)",
    "createRule": "@request.auth.id != '' && @request.auth.id = senderId",
    "updateRule": "@request.auth.id != '' && (@request.auth.id = senderId || @request.auth.id = receiverId)",
    "deleteRule": "@request.auth.id != '' && @request.auth.id = senderId"
  });
  
  dao.saveCollection(messagesCollection);

  // ============================================================================
  // Collection: m_typing_indicators
  // Purpose: Real-time typing status (ephemeral data, auto-expire)
  // Real-time: Yes (high-frequency updates)
  // ============================================================================
  
  const typingIndicatorsCollection = new Collection({
    "name": "m_typing_indicators",
    "type": "base",
    "system": false,
    "schema": [
      {
        "name": "conversationId",
        "type": "relation",
        "required": true,
        "options": {
          "collectionId": conversationsCollection.id,
          "cascadeDelete": true,
          "min": 1,
          "max": 1
        }
      },
      {
        "name": "userId",
        "type": "relation",
        "required": true,
        "options": {
          "collectionId": "_pb_users_auth_",
          "cascadeDelete": true,
          "min": 1,
          "max": 1
        }
      },
      {
        "name": "isTyping",
        "type": "bool",
        "required": true
      },
      {
        "name": "lastUpdated",
        "type": "date",
        "required": true
      }
    ],
    "indexes": [
      "CREATE UNIQUE INDEX idx_typing_user ON m_typing_indicators (conversationId, userId)",
      "CREATE INDEX idx_typing_conversation ON m_typing_indicators (conversationId, isTyping)"
    ],
    "listRule": "@request.auth.id != ''",
    "viewRule": "@request.auth.id != ''",
    "createRule": "@request.auth.id != '' && @request.auth.id = userId",
    "updateRule": "@request.auth.id != '' && @request.auth.id = userId",
    "deleteRule": "@request.auth.id != '' && @request.auth.id = userId"
  });
  
  dao.saveCollection(typingIndicatorsCollection);

  // ============================================================================
  // Collection: m_read_receipts
  // Purpose: Track when messages are read (for analytics & receipts)
  // Real-time: Yes (triggers status updates)
  // ============================================================================
  
  const readReceiptsCollection = new Collection({
    "name": "m_read_receipts",
    "type": "base",
    "system": false,
    "schema": [
      {
        "name": "messageId",
        "type": "relation",
        "required": true,
        "options": {
          "collectionId": messagesCollection.id,
          "cascadeDelete": true,
          "min": 1,
          "max": 1
        }
      },
      {
        "name": "userId",
        "type": "relation",
        "required": true,
        "options": {
          "collectionId": "_pb_users_auth_",
          "cascadeDelete": true,
          "min": 1,
          "max": 1
        }
      },
      {
        "name": "readAt",
        "type": "date",
        "required": true
      }
    ],
    "indexes": [
      "CREATE UNIQUE INDEX idx_receipt_message_user ON m_read_receipts (messageId, userId)",
      "CREATE INDEX idx_receipt_message ON m_read_receipts (messageId)",
      "CREATE INDEX idx_receipt_user ON m_read_receipts (userId, readAt DESC)"
    ],
    "listRule": "@request.auth.id != ''",
    "viewRule": "@request.auth.id != ''",
    "createRule": "@request.auth.id != '' && @request.auth.id = userId",
    "updateRule": null,  // Read receipts are immutable once created
    "deleteRule": null   // Read receipts cannot be deleted
  });
  
  dao.saveCollection(readReceiptsCollection);

}, (db) => {
  // Rollback migration - delete collections in reverse order
  const dao = new Dao(db);
  
  try {
    dao.deleteCollection(dao.findCollectionByNameOrId("m_read_receipts"));
  } catch (e) {}
  
  try {
    dao.deleteCollection(dao.findCollectionByNameOrId("m_typing_indicators"));
  } catch (e) {}
  
  try {
    dao.deleteCollection(dao.findCollectionByNameOrId("m_messages"));
  } catch (e) {}
  
  try {
    dao.deleteCollection(dao.findCollectionByNameOrId("m_conversations"));
  } catch (e) {}
});
