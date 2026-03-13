/// <reference path="../pb_data/types.d.ts" />

/**
 * Idempotent Migration: Complete Schema Setup
 * Refactored to Functional/Fluent Style (Scala-like)
 */
migrate((db) => {
  const dao = new Dao(db);
  // --- Embedded Functional Prelude (Option/Try/SchemaDSL) ---
  
  const Option = {
    of: (val) => (val === null || val === undefined) ? None() : Some(val),
    empty: () => None()
  };

  const Some = (value) => ({
    isEmpty: () => false,
    isDefined: () => true,
    get: () => value,
    map: (fn) => Option.of(fn(value)),
    flatMap: (fn) => { const res = fn(value); return (res && res.isOption) ? res : Option.of(res); },
    filter: (p) => p(value) ? Some(value) : None(),
    getOrElse: () => value,
    fold: (_, ifPresent) => ifPresent(value),
    isOption: true
  });

  const None = () => ({
    isEmpty: () => true,
    isDefined: () => false,
    get: () => { throw new Error("NoSuchElementException"); },
    map: () => None(),
    flatMap: () => None(),
    filter: () => None(),
    getOrElse: (def) => (typeof def === 'function') ? def() : def,
    fold: (ifEmpty) => ifEmpty(),
    isOption: true
  });

  const Try = (fn) => {
    try { return Some(fn()); } catch (e) { return None(); }
  };

  // --- Fluent Schema DSL ---
  const Schema = (dao) => ({
    ensure: (name, type = "base") => ({
      definedBy: (defFn) => {
        const dsl = defFn({
          name, type, system: false, schema: [], indexes: [],
          _listRule: null, _viewRule: null, _createRule: null, _updateRule: null, _deleteRule: null,
          withRules: function(list, view, create, update, del) {
            this._listRule = list;
            this._viewRule = view;
            this._createRule = create;
            this._updateRule = update;
            this._deleteRule = del;
            return this;
          },
          field: function(n, t, opts={}) { this.schema.push({name: n, type: t, ...opts}); return this; },
          relation: function(n, colId, opts={}) { return this.field(n, "relation", { collectionId: colId, cascadeDelete: true, minSelect: null, maxSelect: 1, ...opts }); },
          select: function(n, values, opts={}) { return this.field(n, "select", { values, ...opts }); },
          index: function(idx) { this.indexes.push(idx); return this; },
        });

        const applyRules = (col) => {
          if (dsl._listRule !== null) col.listRule = dsl._listRule;
          if (dsl._viewRule !== null) col.viewRule = dsl._viewRule;
          if (dsl._createRule !== null) col.createRule = dsl._createRule;
          if (dsl._updateRule !== null) col.updateRule = dsl._updateRule;
          if (dsl._deleteRule !== null) col.deleteRule = dsl._deleteRule;
        };

        const existing = Try(() => dao.findCollectionByNameOrId(name));
        
        existing.fold(
          () => {
            console.log(`✨ Creating ${name} collection`);
            const col = new Collection({ name: dsl.name, type: dsl.type, system: dsl.system, schema: dsl.schema, indexes: dsl.indexes });
            applyRules(col);
            dao.saveCollection(col);
          },
          (col) => {
            // Apply rules to existing collections too (idempotent update)
            applyRules(col);
            dao.saveCollection(col);
          }
        );
      }
    })
  });

  console.log("🔧 Starting Functional Schema Migration...");
  const schema = Schema(dao);

  // 1. USER PROFILES
  schema.ensure("s_profiles").definedBy(dsl => dsl.withRules("", "", "", "", "")
    .relation("user", "_pb_users_auth_", { displayFields: ["email"] })
    .field("firstName", "text")
    .field("lastName", "text")
    .field("displayName", "text")
    .field("birthDate", "text")
    .field("bio", "text")
    .field("age", "number")
    .field("location", "text")
    .select("seeking", ["friendship", "romantic", "both", "FRIENDSHIP", "RELATIONSHIP", "BOTH"])
    .select("seekingRelationship", ["friendship", "romantic", "both"])
    .field("avatar", "file", { maxSelect: 1, maxSize: 5242880 })
    .field("version", "number", { min: 0 })
    .index("CREATE UNIQUE INDEX idx_profiles_user ON s_profiles (user)")
  );

  // 2. MATCHES
  schema.ensure("m_matches").definedBy(dsl => dsl.withRules("", "", "", "", "")
    .relation("user1", "_pb_users_auth_")
    .relation("user2", "_pb_users_auth_")
    .select("status", ["pending", "accepted", "rejected", "expired"], { required: true })
    .field("matchScore", "number", { min: 0, max: 100 })
    .field("expiresAt", "number")
    .index("CREATE INDEX idx_matches_status ON m_matches (status)")
  );

  // 3. CONVERSATIONS
  schema.ensure("m_conversations").definedBy(dsl => dsl.withRules("", "", "", "", "")
    .select("type", ["direct", "group"], { required: true })
    .field("title", "text")
    .field("lastMessageAt", "number")
    .field("isArchived", "bool")
    .index("CREATE INDEX idx_conversations_last_message ON m_conversations (lastMessageAt DESC)")
  );

  // 4. PARTICIPANTS
  schema.ensure("m_conversation_participants").definedBy(dsl => dsl.withRules("", "", "", "", "")
    .relation("conversation", "m_conversations")
    .relation("user", "_pb_users_auth_")
    .field("unreadCount", "number")
    .index("CREATE UNIQUE INDEX idx_participants_conv_user ON m_conversation_participants (conversation, user)")
  );

  // 5. MESSAGES
  schema.ensure("m_messages").definedBy(dsl => dsl.withRules("", "", "", "", "")
    .relation("conversation", "m_conversations")
    .relation("sender", "_pb_users_auth_", { cascadeDelete: false })
    .field("content", "text")
    .select("type", ["text", "image", "video", "file"], { required: true })
    .relation("replyTo", "m_messages", { cascadeDelete: false })
    .index("CREATE INDEX idx_messages_conversation ON m_messages (conversation, created DESC)")
  );

  // 6. READ RECEIPTS
  schema.ensure("m_read_receipts").definedBy(dsl => dsl.withRules("", "", "", "", "")
    .relation("message", "m_messages")
    .relation("user", "_pb_users_auth_")
    .field("readAt", "number", { required: true })
    .index("CREATE UNIQUE INDEX idx_read_receipts_msg_user ON m_read_receipts (message, user)")
  );

  // 7. REACTIONS
  schema.ensure("m_reactions").definedBy(dsl => dsl.withRules("", "", "", "", "")
    .relation("message", "m_messages")
    .relation("user", "_pb_users_auth_")
    .field("emoji", "text", { required: true })
    .index("CREATE UNIQUE INDEX idx_reactions_msg_user_emoji ON m_reactions (message, user, emoji)")
  );

  // 8. TYPING STATUS
  schema.ensure("m_typing_status").definedBy(dsl => dsl.withRules("", "", "", "", "")
    .relation("conversation", "m_conversations")
    .relation("user", "_pb_users_auth_")
    .field("isTyping", "bool", { required: true })
    .field("expiresAt", "date", { required: true })
    .index("CREATE UNIQUE INDEX idx_typing_conv_user ON m_typing_status (conversation, user)")
  );

  // 9. PRESENCE
  schema.ensure("m_presence").definedBy(dsl => dsl.withRules("", "", "", "", "")
    .relation("user", "_pb_users_auth_")
    .select("status", ["online", "away", "offline"], { required: true })
    .field("lastSeenAt", "number")
  );

  // 10. PROUST
  schema.ensure("t_proust_questionnaire").definedBy(dsl => dsl.withRules("", "", "", "", "")
    .field("title", "text", { required: true })
    .field("version", "number")
    .field("isActive", "bool")
  );

  schema.ensure("t_proust_question").definedBy(dsl => dsl.withRules("", "", "", "", "")
    .relation("questionnaire", "t_proust_questionnaire")
    .field("questionText", "text", { required: true })
    .field("order", "number")
  );

  schema.ensure("t_user_questionnaire_responses").definedBy(dsl => dsl.withRules("", "", "", "", "")
    .relation("user", "_pb_users_auth_")
    .relation("question", "t_proust_question")
    .field("response", "text", { required: true })
    .index("CREATE UNIQUE INDEX idx_responses_user_question ON t_user_questionnaire_responses (user, question)")
  );

  console.log("✅ Functional Migration Complete");

  // === POST-MIGRATION: Explicitly set API rules ===
  // PocketBase requires rules to be set on fetched collection objects, not at creation time
  const allCollections = [
    "s_profiles", "m_matches", "m_conversations", "m_conversation_participants",
    "m_messages", "m_read_receipts", "m_reactions", "m_typing_status", "m_presence",
    "t_proust_questionnaire", "t_proust_question", "t_user_questionnaire_responses"
  ];

  for (const name of allCollections) {
    try {
      const col = dao.findCollectionByNameOrId(name);
      col.listRule = "";
      col.viewRule = "";
      col.createRule = "";
      col.updateRule = "";
      col.deleteRule = "";
      dao.saveCollection(col);
    } catch (e) {
      console.log(`⚠️ Could not set rules for ${name}: ${e}`);
    }
  }
  console.log("✅ API rules applied to all collections");

}, (db) => {
  console.log("⚠️ Down migration skipped");
});