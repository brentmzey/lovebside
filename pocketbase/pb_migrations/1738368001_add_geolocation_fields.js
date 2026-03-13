/// <reference path="../pb_data/types.d.ts" />

/**
 * Migration: Add Geolocation Fields to s_profiles
 * -- PORTABLE SQL (Standard SQL):
 * -- ALTER TABLE s_profiles ADD COLUMN latitude REAL DEFAULT NULL;
 * -- ALTER TABLE s_profiles ADD COLUMN longitude REAL DEFAULT NULL;
 * -- ALTER TABLE s_profiles ADD COLUMN location_city TEXT DEFAULT NULL;
 * -- ALTER TABLE s_profiles ADD COLUMN location_updated_at TEXT DEFAULT NULL;
 * -- CREATE INDEX idx_profiles_geo ON s_profiles (latitude, longitude);
 */
migrate((db) => {
  const dao = new Dao(db);

  try {
    const col = dao.findCollectionByNameOrId("s_profiles");
    const existingFields = col.schema.fields().map(f => f.name);

    const fieldsToAdd = [
      { name: "latitude", type: "number" },
      { name: "longitude", type: "number" },
      { name: "locationCity", type: "text" },
      { name: "locationUpdatedAt", type: "date" },
    ];

    let modified = false;
    for (const field of fieldsToAdd) {
      if (!existingFields.includes(field.name)) {
        col.schema.addField(new SchemaField({
          name: field.name,
          type: field.type,
          required: false
        }));
        modified = true;
        console.log(`  ✅ Added ${field.name} to s_profiles`);
      }
    }

    if (modified) {
      dao.saveCollection(col);
      console.log("✅ Geolocation fields added to s_profiles");
    } else {
      console.log("✅ Geolocation fields already exist in s_profiles");
    }
  } catch (e) {
    console.log(`⚠️ Could not add geolocation fields: ${e}`);
  }
}, (db) => {
  console.log("⚠️ Down migration: geolocation fields removal skipped");
});
