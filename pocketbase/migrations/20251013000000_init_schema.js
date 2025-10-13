
// pb_migrations/20251013000000_init_schema.js

migrate((db) => {
  const dao = new Dao(db);

  // --- Collection: s_profiles ---
  const profilesCollection = new Collection({
    "name": "s_profiles",
    "type": "base",
    "system": false,
    "schema": [
      {
        "name": "userId",
        "type": "relation",
        "required": true,
        "options": {
          "collectionId": "_pb_users_auth_", // PocketBase's internal users collection ID
          "cascadeDelete": true,
          "min": 1,
          "max": 1
        }
      },
      { "name": "firstName", "type": "text", "required": true },
      { "name": "lastName", "type": "text", "required": true },
      { "name": "birthDate", "type": "date", "required": true },
      { "name": "bio", "type": "editor" },
      { "name": "location", "type": "text" },
      {
        "name": "seeking",
        "type": "select",
        "required": true,
        "options": {
          "maxSelect": 1,
          "values": ["Friendship", "Relationship", "Both"]
        }
      }
    ],
    "indexes": ["CREATE UNIQUE INDEX `idx_unique_userId` ON `s_profiles` (`userId`)"],
    "listRule": "@request.auth.id != '' && @request.auth.id = userId", // Can only see your own profile
    "viewRule": "@request.auth.id != ''", // Any logged in user can view a profile (for matching)
    "createRule": "@request.auth.id != '' && @request.auth.id = userId", // Can only create your own profile
    "updateRule": "@request.auth.id = userId", // Can only update your own profile
    "deleteRule": "@request.auth.id = userId", // Can only delete your own profile
  });
  dao.saveCollection(profilesCollection);


  // --- Collection: s_proust_questionnaires ---
  const questionnairesCollection = new Collection({
    "name": "s_proust_questionnaires",
    "type": "base",
    "system": false,
    "schema": [
      { "name": "questionText", "type": "text", "required": true },
      { "name": "questionOrder", "type": "number", "required": true },
      { "name": "isActive", "type": "bool" }
    ],
    "indexes": ["CREATE INDEX `idx_questionOrder` ON `s_proust_questionnaires` (`questionOrder`)"],
    "listRule": null, // Publicly listable
    "viewRule": null, // Publicly viewable
    "createRule": "@request.auth.role = 'admin'",
    "updateRule": "@request.auth.role = 'admin'",
    "deleteRule": "@request.auth.role = 'admin'",
  });
  dao.saveCollection(questionnairesCollection);

  // --- Collection: s_user_answers ---
  const userAnswersCollection = new Collection({
      "name": "s_user_answers",
      "type": "base",
      "system": false,
      "schema": [
          { "name": "userId", "type": "relation", "required": true, "options": { "collectionId": "_pb_users_auth_", "cascadeDelete": true, "min": 1, "max": 1 } },
          { "name": "questionId", "type": "relation", "required": true, "options": { "collectionId": questionnairesCollection.id, "cascadeDelete": false, "min": 1, "max": 1 } },
          { "name": "answerText", "type": "editor", "required": true }
      ],
      "indexes": ["CREATE UNIQUE INDEX `idx_user_question` ON `s_user_answers` (`userId`, `questionId`)"],
      "listRule": "@request.auth.id = userId",
      "viewRule": "@request.auth.id = userId",
      "createRule": "@request.auth.id = userId",
      "updateRule": "@request.auth.id = userId",
      "deleteRule": "@request.auth.id = userId",
  });
  dao.saveCollection(userAnswersCollection);

  // --- Collection: s_key_values ---
  const keyValuesCollection = new Collection({
      "name": "s_key_values",
      "type": "base",
      "system": false,
      "schema": [
          { "name": "valueText", "type": "text", "required": true },
          { "name": "category", "type": "select", "required": true, "options": { "maxSelect": 1, "values": ["Personality", "Lifestyle", "Goals"] } }
      ],
      "listRule": null, // Publicly listable
      "viewRule": null,
      "createRule": "@request.auth.role = 'admin'",
      "updateRule": "@request.auth.role = 'admin'",
      "deleteRule": "@request.auth.role = 'admin'",
  });
  dao.saveCollection(keyValuesCollection);

  // --- Collection: s_user_values ---
  const userValuesCollection = new Collection({
      "name": "s_user_values",
      "type": "base",
      "system": false,
      "schema": [
          { "name": "userId", "type": "relation", "required": true, "options": { "collectionId": "_pb_users_auth_", "cascadeDelete": true, "min": 1, "max": 1 } },
          { "name": "valueId", "type": "relation", "required": true, "options": { "collectionId": keyValuesCollection.id, "cascadeDelete": false, "min": 1, "max": 1 } }
      ],
      "indexes": ["CREATE UNIQUE INDEX `idx_user_value` ON `s_user_values` (`userId`, `valueId`)"],
      "listRule": "@request.auth.id = userId",
      "viewRule": "@request.auth.id = userId",
      "createRule": "@request.auth.id = userId",
      "updateRule": "@request.auth.id = userId",
      "deleteRule": "@request.auth.id = userId",
  });
  dao.saveCollection(userValuesCollection);

  // --- Collection: t_matches ---
  const matchesCollection = new Collection({
      "name": "t_matches",
      "type": "base",
      "system": false,
      "schema": [
          { "name": "userOneId", "type": "relation", "required": true, "options": { "collectionId": "_pb_users_auth_", "cascadeDelete": true, "min": 1, "max": 1 } },
          { "name": "userTwoId", "type": "relation", "required": true, "options": { "collectionId": "_pb_users_auth_", "cascadeDelete": true, "min": 1, "max": 1 } },
          { "name": "matchScore", "type": "number", "required": true },
          { "name": "matchStatus", "type": "select", "required": true, "options": { "maxSelect": 1, "values": ["Pending", "Matched", "Declined"] } },
          { "name": "generatedAt", "type": "date", "required": true }
      ],
      "indexes": ["CREATE UNIQUE INDEX `idx_match_users` ON `t_matches` (`userOneId`, `userTwoId`)"],
      "listRule": "@request.auth.id = userOneId || @request.auth.id = userTwoId",
      "viewRule": "@request.auth.id = userOneId || @request.auth.id = userTwoId",
      "createRule": "@request.auth.role = 'admin'", // Or service role
      "updateRule": "@request.auth.id = userOneId || @request.auth.id = userTwoId",
      "deleteRule": "@request.auth.role = 'admin'",
  });
  dao.saveCollection(matchesCollection);

  // --- Collection: t_prompts ---
  const promptsCollection = new Collection({
      "name": "t_prompts",
      "type": "base",
      "system": false,
      "schema": [
          { "name": "matchId", "type": "relation", "required": true, "options": { "collectionId": matchesCollection.id, "cascadeDelete": true, "min": 1, "max": 1 } },
          { "name": "promptText", "type": "text", "required": true },
          { "name": "promptType", "type": "select", "required": true, "options": { "maxSelect": 1, "values": ["Icebreaker", "DateIdea"] } }
      ],
      "listRule": null, // Or based on match involvement
      "viewRule": null,
      "createRule": "@request.auth.role = 'admin'", // Or service role
      "updateRule": "@request.auth.role = 'admin'",
      "deleteRule": "@request.auth.role = 'admin'",
  });
  dao.saveCollection(promptsCollection);

}, (db) => {
  const dao = new Dao(db);

  dao.deleteCollection(dao.findCollectionByNameOrId("s_profiles"));
  dao.deleteCollection(dao.findCollectionByNameOrId("s_proust_questionnaires"));
  dao.deleteCollection(dao.findCollectionByNameOrId("s_user_answers"));
  dao.deleteCollection(dao.findCollectionByNameOrId("s_key_values"));
  dao.deleteCollection(dao.findCollectionByNameOrId("s_user_values"));
  dao.deleteCollection(dao.findCollectionByNameOrId("t_matches"));
  dao.deleteCollection(dao.findCollectionByNameOrId("t_prompts"));
});
