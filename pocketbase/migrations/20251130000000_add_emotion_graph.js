// pb_migrations/20251130000000_add_emotion_graph.js

migrate((db) => {
  const dao = new Dao(db);
  const profilesCollection = dao.findCollectionByNameOrId("s_profiles");
  const usersCollectionId = "_pb_users_auth_";

  // ---- g_emotion_terms ----
  const emotionTermsCollection = new Collection({
    "name": "g_emotion_terms",
    "type": "base",
    "system": false,
    "schema": [
      {
        "name": "slug",
        "type": "text",
        "required": true,
        "options": {
          "min": 2,
          "max": 64,
          "pattern": "^[a-z0-9_]+$"
        }
      },
      {
        "name": "label",
        "type": "text",
        "required": true
      },
      {
        "name": "polarity",
        "type": "select",
        "required": true,
        "options": {
          "maxSelect": 1,
          "values": ["POSITIVE", "NEGATIVE", "NEUTRAL"]
        }
      },
      {
        "name": "defaultVerb",
        "type": "text",
        "required": true
      },
      {
        "name": "defaultAdverb",
        "type": "text",
        "required": false
      },
      {
        "name": "intensityMin",
        "type": "number",
        "required": true
      },
      {
        "name": "intensityMax",
        "type": "number",
        "required": true
      },
      {
        "name": "color",
        "type": "text",
        "required": false
      }
    ],
    "indexes": [
      "CREATE UNIQUE INDEX `idx_g_emotion_terms_slug` ON `g_emotion_terms` (`slug`)"
    ],
    "listRule": null,
    "viewRule": null,
    "createRule": "@request.auth.role = 'admin'",
    "updateRule": "@request.auth.role = 'admin'",
    "deleteRule": "@request.auth.role = 'admin'"
  });
  dao.saveCollection(emotionTermsCollection);

  // ---- g_expression_modifiers ----
  const expressionModifiersCollection = new Collection({
    "name": "g_expression_modifiers",
    "type": "base",
    "system": false,
    "schema": [
      {
        "name": "ownerId",
        "type": "relation",
        "required": true,
        "options": {
          "collectionId": usersCollectionId,
          "cascadeDelete": true,
          "min": 1,
          "max": 1
        }
      },
      {
        "name": "slug",
        "type": "text",
        "required": true,
        "options": {
          "min": 2,
          "max": 64,
          "pattern": "^[a-z0-9_]+$"
        }
      },
      {
        "name": "label",
        "type": "text",
        "required": true
      },
      {
        "name": "verbOverride",
        "type": "text",
        "required": false
      },
      {
        "name": "adverb",
        "type": "text",
        "required": false
      },
      {
        "name": "intensityDelta",
        "type": "number",
        "required": false
      },
      {
        "name": "tone",
        "type": "select",
        "required": false,
        "options": {
          "maxSelect": 1,
          "values": ["SOFT", "BOLD", "NEUTRAL"]
        }
      }
    ],
    "indexes": [
      "CREATE UNIQUE INDEX `idx_g_expression_modifiers_owner_slug` ON `g_expression_modifiers` (`ownerId`, `slug`)"
    ],
    "listRule": "@request.auth.id = ownerId",
    "viewRule": "@request.auth.id = ownerId",
    "createRule": "@request.auth.id = ownerId",
    "updateRule": "@request.auth.id = ownerId",
    "deleteRule": "@request.auth.id = ownerId"
  });
  dao.saveCollection(expressionModifiersCollection);

  // ---- g_items ----
  const graphItemsCollection = new Collection({
    "name": "g_items",
    "type": "base",
    "system": false,
    "schema": [
      {
        "name": "ownerId",
        "type": "relation",
        "required": true,
        "options": {
          "collectionId": usersCollectionId,
          "cascadeDelete": true,
          "min": 1,
          "max": 1
        }
      },
      {
        "name": "title",
        "type": "text",
        "required": true,
        "options": {
          "min": 1,
          "max": 120
        }
      },
      {
        "name": "category",
        "type": "select",
        "required": true,
        "options": {
          "maxSelect": 1,
          "values": ["PERSON", "ITEM", "PLACE", "MEMORY", "MEDIA", "OTHER"]
        }
      },
      {
        "name": "profileId",
        "type": "relation",
        "required": false,
        "options": {
          "collectionId": profilesCollection?.id || "",
          "cascadeDelete": false,
          "min": 0,
          "max": 1
        }
      },
      {
        "name": "summary",
        "type": "text",
        "required": false,
        "options": {
          "max": 500
        }
      },
      {
        "name": "referenceUrl",
        "type": "text",
        "required": false
      }
    ],
    "indexes": [
      "CREATE INDEX `idx_g_items_owner_category` ON `g_items` (`ownerId`, `category`)"
    ],
    "listRule": "@request.auth.id = ownerId",
    "viewRule": "@request.auth.id = ownerId",
    "createRule": "@request.auth.id = ownerId",
    "updateRule": "@request.auth.id = ownerId",
    "deleteRule": "@request.auth.id = ownerId"
  });
  dao.saveCollection(graphItemsCollection);

  // ---- g_emotion_edges ----
  const emotionEdgesCollection = new Collection({
    "name": "g_emotion_edges",
    "type": "base",
    "system": false,
    "schema": [
      {
        "name": "ownerId",
        "type": "relation",
        "required": true,
        "options": {
          "collectionId": usersCollectionId,
          "cascadeDelete": true,
          "min": 1,
          "max": 1
        }
      },
      {
        "name": "subjectProfileId",
        "type": "relation",
        "required": false,
        "options": {
          "collectionId": profilesCollection?.id || "",
          "cascadeDelete": false,
          "min": 0,
          "max": 1
        }
      },
      {
        "name": "targetKind",
        "type": "select",
        "required": true,
        "options": {
          "maxSelect": 1,
          "values": ["PERSON", "ITEM", "MEMORY", "EXPERIENCE"]
        }
      },
      {
        "name": "targetProfileId",
        "type": "relation",
        "required": false,
        "options": {
          "collectionId": profilesCollection?.id || "",
          "cascadeDelete": false,
          "min": 0,
          "max": 1
        }
      },
      {
        "name": "targetItemId",
        "type": "relation",
        "required": false,
        "options": {
          "collectionId": graphItemsCollection.id,
          "cascadeDelete": true,
          "min": 0,
          "max": 1
        }
      },
      {
        "name": "emotionTermId",
        "type": "relation",
        "required": true,
        "options": {
          "collectionId": emotionTermsCollection.id,
          "cascadeDelete": false,
          "min": 1,
          "max": 1
        }
      },
      {
        "name": "customVerb",
        "type": "text",
        "required": false,
        "options": {
          "max": 64
        }
      },
      {
        "name": "customAdverb",
        "type": "text",
        "required": false,
        "options": {
          "max": 64
        }
      },
      {
        "name": "intensity",
        "type": "number",
        "required": true,
        "options": {
          "min": 0,
          "max": 100
        }
      },
      {
        "name": "moment",
        "type": "date",
        "required": false
      },
      {
        "name": "contextTags",
        "type": "json",
        "required": false
      },
      {
        "name": "narrative",
        "type": "editor",
        "required": false
      }
    ],
    "indexes": [
      "CREATE INDEX `idx_g_emotion_edges_owner_term` ON `g_emotion_edges` (`ownerId`, `emotionTermId`)",
      "CREATE INDEX `idx_g_emotion_edges_target` ON `g_emotion_edges` (`ownerId`, `targetKind`, `targetItemId`, `targetProfileId`)"
    ],
    "listRule": "@request.auth.id = ownerId",
    "viewRule": "@request.auth.id = ownerId",
    "createRule": "@request.auth.id = ownerId",
    "updateRule": "@request.auth.id = ownerId",
    "deleteRule": "@request.auth.id = ownerId"
  });
  dao.saveCollection(emotionEdgesCollection);

  // ---- g_edge_modifiers ----
  const edgeModifiersCollection = new Collection({
    "name": "g_edge_modifiers",
    "type": "base",
    "system": false,
    "schema": [
      {
        "name": "ownerId",
        "type": "relation",
        "required": true,
        "options": {
          "collectionId": usersCollectionId,
          "cascadeDelete": true,
          "min": 1,
          "max": 1
        }
      },
      {
        "name": "edgeId",
        "type": "relation",
        "required": true,
        "options": {
          "collectionId": emotionEdgesCollection.id,
          "cascadeDelete": true,
          "min": 1,
          "max": 1
        }
      },
      {
        "name": "modifierId",
        "type": "relation",
        "required": true,
        "options": {
          "collectionId": expressionModifiersCollection.id,
          "cascadeDelete": true,
          "min": 1,
          "max": 1
        }
      },
      {
        "name": "emphasis",
        "type": "number",
        "required": false
      },
      {
        "name": "sequence",
        "type": "number",
        "required": false
      }
    ],
    "indexes": [
      "CREATE UNIQUE INDEX `idx_g_edge_modifiers_unique` ON `g_edge_modifiers` (`edgeId`, `modifierId`)"
    ],
    "listRule": "@request.auth.id = ownerId",
    "viewRule": "@request.auth.id = ownerId",
    "createRule": "@request.auth.id = ownerId",
    "updateRule": "@request.auth.id = ownerId",
    "deleteRule": "@request.auth.id = ownerId"
  });
  dao.saveCollection(edgeModifiersCollection);

}, (db) => {
  const dao = new Dao(db);
  [
    "g_edge_modifiers",
    "g_emotion_edges",
    "g_items",
    "g_expression_modifiers",
    "g_emotion_terms"
  ].forEach((name) => {
    const collection = dao.findCollectionByNameOrId(name);
    if (collection) {
      dao.deleteCollection(collection);
    }
  });
});
