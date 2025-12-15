/// <reference path="../pb_data/types.d.ts" />
migrate((db) => {
    const dao = new Dao(db);

    // 1. Conversations
    const conversations = new Collection({
        name: "m_conversations",
        type: "base",
        schema: [
            {
                name: "conversationType",
                type: "select",
                required: true,
                options: {
                    values: ["direct", "group", "channel"],
                    maxSelect: 1
                }
            },
            {
                name: "conversationName",
                type: "text",
                required: false
            },
            {
                name: "conversationAvatar",
                type: "file",
                required: false,
                options: {
                    maxSelect: 1,
                    maxSize: 5242880, // 5MB
                    mimeTypes: ["image/jpeg", "image/png", "image/webp"]
                }
            },
            {
                name: "lastMessageText",
                type: "text",
                required: false
            },
            {
                name: "lastMessageAt",
                type: "date",
                required: false
            },
            {
                name: "totalMessageCount",
                type: "number",
                required: false,
                options: {
                    onlyInt: true
                }
            },
            {
                name: "maxParticipants",
                type: "number",
                required: true,
                options: {
                    min: 1,
                    max: 100,
                    onlyInt: true
                }
            },
            {
                name: "isArchived",
                type: "bool",
                required: false
            }
        ],
        // Access Rules
        listRule: "@request.auth.id != '' && @collection.m_conversation_participants.userId ?= @request.auth.id && @collection.m_conversation_participants.conversationId ?= id",
        viewRule: "@request.auth.id != '' && @collection.m_conversation_participants.userId ?= @request.auth.id && @collection.m_conversation_participants.conversationId ?= id",
        createRule: "@request.auth.id != ''",
        updateRule: "@request.auth.id != '' && @collection.m_conversation_participants.userId ?= @request.auth.id && @collection.m_conversation_participants.conversationId ?= id",
        deleteRule: "@request.auth.id != '' && @collection.m_conversation_participants.userId ?= @request.auth.id && @collection.m_conversation_participants.conversationId ?= id && @collection.m_conversation_participants.role ?~ 'admin'",
        indexes: [
            "CREATE INDEX `idx_conversation_type` ON `m_conversations` (`conversationType`)",
            "CREATE INDEX `idx_conversation_lastMessage` ON `m_conversations` (`lastMessageAt` DESC)",
            "CREATE INDEX `idx_conversation_archived` ON `m_conversations` (`isArchived`, `lastMessageAt` DESC)"
        ]
    });
    dao.saveCollection(conversations);

    // 2. Participants
    const participants = new Collection({
        name: "m_conversation_participants",
        type: "base",
        schema: [
            {
                name: "conversationId",
                type: "relation",
                required: true,
                options: {
                    collectionId: conversations.id,
                    maxSelect: 1,
                    cascadeDelete: true
                }
            },
            {
                name: "userId",
                type: "relation",
                required: true,
                options: {
                    collectionId: "_pb_users_auth_",
                    maxSelect: 1,
                    cascadeDelete: true
                }
            },
            {
                name: "role",
                type: "select",
                required: true,
                options: {
                    values: ["admin", "member", "readonly"],
                    maxSelect: 1
                }
            },
            {
                name: "unreadCount",
                type: "number",
                required: false,
                options: {
                    min: 0
                }
            },
            {
                name: "lastReadAt",
                type: "date",
                required: false
            },
            {
                name: "joinedAt",
                type: "date",
                required: true
            },
            {
                name: "leftAt",
                type: "date",
                required: false
            },
            {
                name: "isMuted",
                type: "bool",
                required: false
            },
            {
                name: "isPinned",
                type: "bool",
                required: false
            }
        ],
        // Access Rules
        listRule: "@request.auth.id != '' && @collection.m_conversation_participants.userId ?= @request.auth.id && @collection.m_conversation_participants.conversationId ?= conversationId",
        viewRule: "@request.auth.id != '' && @collection.m_conversation_participants.userId ?= @request.auth.id && @collection.m_conversation_participants.conversationId ?= conversationId",
        createRule: "@request.auth.id != ''",
        updateRule: "@request.auth.id != '' && userId = @request.auth.id",
        deleteRule: "@request.auth.id != '' && userId = @request.auth.id",
        indexes: [
            "CREATE UNIQUE INDEX `idx_participant_unique` ON `m_conversation_participants` (`conversationId`, `userId`)",
            "CREATE INDEX `idx_participant_user` ON `m_conversation_participants` (`userId`, `leftAt`, `isPinned` DESC)",
            "CREATE INDEX `idx_participant_conversation` ON `m_conversation_participants` (`conversationId`, `role`)"
        ]
    });
    dao.saveCollection(participants);

    // 3. Messages
    const messages = new Collection({
        name: "m_messages",
        type: "base",
        schema: [
            {
                name: "conversationId",
                type: "relation",
                required: true,
                options: {
                    collectionId: conversations.id,
                    maxSelect: 1,
                    cascadeDelete: true
                }
            },
            {
                name: "senderId",
                type: "relation",
                required: true,
                options: {
                    collectionId: "_pb_users_auth_",
                    maxSelect: 1,
                    cascadeDelete: false
                }
            },
            {
                name: "content",
                type: "text",
                required: true,
                options: {
                    min: 1
                }
            },
            {
                name: "messageType",
                type: "select",
                required: true,
                options: {
                    values: ["text", "image", "file", "system"],
                    maxSelect: 1
                }
            },
            {
                name: "attachments",
                type: "file",
                required: false,
                options: {
                    maxSelect: 10,
                    maxSize: 10485760, // 10MB
                    mimeTypes: ["image/jpeg", "image/png", "image/webp", "application/pdf", "video/mp4"]
                }
            },
            {
                name: "sentAt",
                type: "date",
                required: true
            },
            {
                name: "editedAt",
                type: "date",
                required: false
            },
            {
                name: "deletedAt",
                type: "date",
                required: false
            },
            {
                name: "readByCount",
                type: "number",
                required: false,
                options: {
                    min: 0,
                    onlyInt: true
                }
            },
            {
                name: "replyToMessageId",
                type: "relation",
                required: false,
                options: {
                    collectionId: "m_messages", // Self-reference will need careful handling or generic ID
                    maxSelect: 1,
                    cascadeDelete: false
                }
            },
            {
                name: "threadRootId",
                type: "text",
                required: false
            },
            {
                name: "threadDepth",
                type: "number",
                required: false
            },
            {
                name: "threadReplyCount",
                type: "number",
                required: false
            }
        ],
        // Access Rules
        listRule: "@request.auth.id != '' && @collection.m_conversation_participants.userId ?= @request.auth.id && @collection.m_conversation_participants.conversationId ?= conversationId",
        viewRule: "@request.auth.id != '' && @collection.m_conversation_participants.userId ?= @request.auth.id && @collection.m_conversation_participants.conversationId ?= conversationId",
        createRule: "@request.auth.id != '' && @request.auth.id = senderId && @collection.m_conversation_participants.userId ?= @request.auth.id && @collection.m_conversation_participants.conversationId ?= conversationId",
        updateRule: "@request.auth.id != '' && @request.auth.id = senderId",
        deleteRule: "@request.auth.id != '' && @request.auth.id = senderId",
        indexes: [
            "CREATE INDEX `idx_msg_conv_cover` ON `m_messages` (`conversationId`, `deletedAt`, `sentAt` DESC, `senderId`)",
            "CREATE INDEX `idx_msg_sender` ON `m_messages` (`senderId`, `sentAt` DESC) WHERE `deletedAt` IS NULL",
            "CREATE INDEX `idx_msg_unread` ON `m_messages` (`conversationId`, `readByCount`, `sentAt` DESC) WHERE `deletedAt` IS NULL AND `readByCount` < 10"
        ]
    });

    // Handling self-reference immediately normally works if collection initialized first
    // But safer to create collection first, then update if needed. Here we just try to save.
    dao.saveCollection(messages);

    // 4. Read Receipts
    const receipts = new Collection({
        name: "m_read_receipts",
        type: "base",
        schema: [
            {
                name: "messageId",
                type: "relation",
                required: true,
                options: {
                    collectionId: messages.id,
                    cascadeDelete: true,
                    maxSelect: 1
                }
            },
            {
                name: "userId",
                type: "relation",
                required: true,
                options: {
                    collectionId: "_pb_users_auth_",
                    cascadeDelete: true,
                    maxSelect: 1
                }
            },
            {
                name: "readAt",
                type: "date",
                required: true
            }
        ],
        // Access Rules
        listRule: "@request.auth.id != ''",
        viewRule: "@request.auth.id != ''",
        createRule: "@request.auth.id != '' && userId = @request.auth.id",
        updateRule: null,
        deleteRule: null,
        indexes: [
            "CREATE UNIQUE INDEX `idx_receipt_unique` ON `m_read_receipts` (`messageId`, `userId`)",
            "CREATE INDEX `idx_receipt_msg` ON `m_read_receipts` (`messageId`, `readAt` DESC)",
            "CREATE INDEX `idx_receipt_user` ON `m_read_receipts` (`userId`, `readAt` DESC)"
        ]
    });
    dao.saveCollection(receipts);

}, (db) => {
    const dao = new Dao(db);
    try { dao.deleteCollection(dao.findCollectionByNameOrId("m_read_receipts")); } catch (_) { }
    try { dao.deleteCollection(dao.findCollectionByNameOrId("m_messages")); } catch (_) { }
    try { dao.deleteCollection(dao.findCollectionByNameOrId("m_conversation_participants")); } catch (_) { }
    try { dao.deleteCollection(dao.findCollectionByNameOrId("m_conversations")); } catch (_) { }
})
