/// <reference path="../pb_data/types.d.ts" />

/**
 * Migration: Add Matching & Swipe Schema
 * -- PORTABLE SQL (Standard SQL):
 * -- CREATE TABLE IF NOT EXISTS m_swipes (
 * --   id TEXT PRIMARY KEY,
 * --   swiper TEXT NOT NULL REFERENCES users(id),
 * --   swiped TEXT NOT NULL REFERENCES users(id),
 * --   direction TEXT NOT NULL CHECK(direction IN ('like','pass','superlike')),
 * --   created TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
 * --   updated TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
 * -- );
 * -- CREATE UNIQUE INDEX idx_swipes_pair ON m_swipes (swiper, swiped);
 * -- CREATE INDEX idx_swipes_swiped ON m_swipes (swiped, direction);
 * --
 * -- CREATE TABLE IF NOT EXISTS m_match_scores (
 * --   id TEXT PRIMARY KEY,
 * --   user1 TEXT NOT NULL REFERENCES users(id),
 * --   user2 TEXT NOT NULL REFERENCES users(id),
 * --   proust_score REAL DEFAULT 0.0,
 * --   geo_score REAL DEFAULT 0.0,
 * --   interest_score REAL DEFAULT 0.0,
 * --   seeking_score REAL DEFAULT 0.0,
 * --   composite_score REAL DEFAULT 0.0,
 * --   computed_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
 * --   created TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
 * --   updated TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
 * -- );
 * -- CREATE UNIQUE INDEX idx_match_scores_pair ON m_match_scores (user1, user2);
 * -- CREATE INDEX idx_match_scores_composite ON m_match_scores (composite_score DESC);
 */
migrate((db) => {
  const dao = new Dao(db);

  // --- m_swipes ---
  const Try = (fn) => { try { return fn(); } catch(e) { return null; } };

  if (!Try(() => dao.findCollectionByNameOrId("m_swipes"))) {
    console.log("✨ Creating m_swipes collection");
    const col = new Collection({
      name: "m_swipes",
      type: "base",
      system: false,
      schema: [
        { name: "swiper", type: "relation", collectionId: "_pb_users_auth_", cascadeDelete: false, maxSelect: 1 },
        { name: "swiped", type: "relation", collectionId: "_pb_users_auth_", cascadeDelete: false, maxSelect: 1 },
        { name: "direction", type: "select", values: ["like", "pass", "superlike"], required: true },
      ],
      indexes: [
        "CREATE UNIQUE INDEX idx_swipes_pair ON m_swipes (swiper, swiped)",
        "CREATE INDEX idx_swipes_swiped ON m_swipes (swiped, direction)"
      ]
    });
    col.listRule = "";
    col.viewRule = "";
    col.createRule = "";
    col.updateRule = "";
    col.deleteRule = "";
    dao.saveCollection(col);
  } else {
    console.log("✅ m_swipes already exists");
  }

  // --- m_match_scores ---
  if (!Try(() => dao.findCollectionByNameOrId("m_match_scores"))) {
    console.log("✨ Creating m_match_scores collection");
    const col = new Collection({
      name: "m_match_scores",
      type: "base",
      system: false,
      schema: [
        { name: "user1", type: "relation", collectionId: "_pb_users_auth_", cascadeDelete: false, maxSelect: 1 },
        { name: "user2", type: "relation", collectionId: "_pb_users_auth_", cascadeDelete: false, maxSelect: 1 },
        { name: "proustScore", type: "number" },
        { name: "geoScore", type: "number" },
        { name: "interestScore", type: "number" },
        { name: "seekingScore", type: "number" },
        { name: "compositeScore", type: "number" },
        { name: "computedAt", type: "date" },
      ],
      indexes: [
        "CREATE UNIQUE INDEX idx_match_scores_pair ON m_match_scores (user1, user2)",
        "CREATE INDEX idx_match_scores_composite ON m_match_scores (compositeScore DESC)"
      ]
    });
    col.listRule = "";
    col.viewRule = "";
    col.createRule = "";
    col.updateRule = "";
    col.deleteRule = "";
    dao.saveCollection(col);
  } else {
    console.log("✅ m_match_scores already exists");
  }

  // --- Add matchType to m_matches if missing ---
  try {
    const matchCol = dao.findCollectionByNameOrId("m_matches");
    const existingFields = matchCol.schema.fields().map(f => f.name);
    if (!existingFields.includes("matchType")) {
      matchCol.schema.addField(new SchemaField({
        name: "matchType",
        type: "select",
        values: ["algorithm", "mutual_like", "manual"],
        required: false
      }));
      dao.saveCollection(matchCol);
      console.log("  ✅ Added matchType to m_matches");
    }
  } catch (e) {
    console.log(`⚠️ Could not update m_matches: ${e}`);
  }

  console.log("✅ Matching schema migration complete");
}, (db) => {
  console.log("⚠️ Down migration: matching schema removal skipped");
});
