/// <reference path="../pb_data/types.d.ts" />

/**
 * Migration: Add Category Preferences / Interest Schema
 * -- PORTABLE SQL (Standard SQL):
 * -- CREATE TABLE IF NOT EXISTS s_interest_categories (
 * --   id TEXT PRIMARY KEY,
 * --   name TEXT NOT NULL UNIQUE,
 * --   icon TEXT,
 * --   display_order INTEGER DEFAULT 0,
 * --   created TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
 * --   updated TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
 * -- );
 * --
 * -- CREATE TABLE IF NOT EXISTS s_user_interests (
 * --   id TEXT PRIMARY KEY,
 * --   user_id TEXT NOT NULL REFERENCES users(id),
 * --   category TEXT NOT NULL REFERENCES s_interest_categories(id),
 * --   value TEXT NOT NULL,
 * --   importance INTEGER DEFAULT 5 CHECK(importance BETWEEN 1 AND 10),
 * --   created TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
 * --   updated TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
 * -- );
 * -- CREATE INDEX idx_user_interests_user ON s_user_interests (user_id);
 * -- CREATE INDEX idx_user_interests_category ON s_user_interests (category);
 * -- CREATE UNIQUE INDEX idx_user_interests_unique ON s_user_interests (user_id, category, value);
 */
migrate((db) => {
  const dao = new Dao(db);
  const Try = (fn) => { try { return fn(); } catch(e) { return null; } };

  // --- s_interest_categories ---
  if (!Try(() => dao.findCollectionByNameOrId("s_interest_categories"))) {
    console.log("✨ Creating s_interest_categories collection");
    const col = new Collection({
      name: "s_interest_categories",
      type: "base",
      system: false,
      schema: [
        { name: "name", type: "text", required: true },
        { name: "icon", type: "text" },
        { name: "displayOrder", type: "number" },
      ],
      indexes: [
        "CREATE UNIQUE INDEX idx_interest_categories_name ON s_interest_categories (name)"
      ]
    });
    col.listRule = "";
    col.viewRule = "";
    col.createRule = "";
    col.updateRule = "";
    col.deleteRule = "";
    dao.saveCollection(col);
  } else {
    console.log("✅ s_interest_categories already exists");
  }

  // --- s_user_interests ---
  if (!Try(() => dao.findCollectionByNameOrId("s_user_interests"))) {
    console.log("✨ Creating s_user_interests collection");
    const col = new Collection({
      name: "s_user_interests",
      type: "base",
      system: false,
      schema: [
        { name: "user", type: "relation", collectionId: "_pb_users_auth_", cascadeDelete: true, maxSelect: 1 },
        { name: "category", type: "relation", collectionId: "s_interest_categories", cascadeDelete: false, maxSelect: 1 },
        { name: "value", type: "text", required: true },
        { name: "importance", type: "number", min: 1, max: 10 },
      ],
      indexes: [
        "CREATE INDEX idx_user_interests_user ON s_user_interests (user)",
        "CREATE INDEX idx_user_interests_category ON s_user_interests (category)",
        "CREATE UNIQUE INDEX idx_user_interests_unique ON s_user_interests (user, category, value)"
      ]
    });
    col.listRule = "";
    col.viewRule = "";
    col.createRule = "";
    col.updateRule = "";
    col.deleteRule = "";
    dao.saveCollection(col);
  } else {
    console.log("✅ s_user_interests already exists");
  }

  // --- Seed default interest categories ---
  try {
    const catCol = dao.findCollectionByNameOrId("s_interest_categories");
    const existing = dao.findRecordsByFilter(catCol.id, "1=1", "-created", 100, 0);
    if (existing.length === 0) {
      const categories = [
        { name: "music", icon: "🎵", displayOrder: 1 },
        { name: "food", icon: "🍕", displayOrder: 2 },
        { name: "travel", icon: "✈️", displayOrder: 3 },
        { name: "sports", icon: "⚽", displayOrder: 4 },
        { name: "movies", icon: "🎬", displayOrder: 5 },
        { name: "books", icon: "📚", displayOrder: 6 },
        { name: "hobbies", icon: "🎨", displayOrder: 7 },
        { name: "fitness", icon: "💪", displayOrder: 8 },
        { name: "pets", icon: "🐾", displayOrder: 9 },
        { name: "faith", icon: "✝️", displayOrder: 10 },
      ];
      for (const cat of categories) {
        const record = new Record(catCol, cat);
        dao.saveRecord(record);
      }
      console.log("  ✅ Seeded default interest categories");
    }
  } catch (e) {
    console.log(`⚠️ Could not seed interest categories: ${e}`);
  }

  console.log("✅ Category preferences migration complete");
}, (db) => {
  console.log("⚠️ Down migration: category preferences removal skipped");
});
