/// <reference path="../pb_data/types.d.ts" />

migrate((db) => {
  const dao = new Dao(db);

  // Helper from the functional prelude in the main migration
  const Try = (fn) => {
    try { return { isDefined: () => true, get: () => fn() }; } catch (e) { return { isDefined: () => false }; }
  };

  const collections = ["s_profiles", "m_messages", "t_user_questionnaire_responses"];

  collections.forEach(name => {
    const col = dao.findCollectionByNameOrId(name);
    
    if (name === "s_profiles") {
      // Add semantic compressed fields if not exists
      const fields = [
        { name: "bioBrotliBase64", type: "text", options: { description: "brotli:base64:text" } },
        { name: "aboutMeBrotliBase64", type: "text", options: { description: "brotli:base64:text" } }
      ];

      fields.forEach(f => {
        const exists = col.schema.fields().find(field => field.name === f.name);
        if (!exists) {
          col.schema.addField(new SchemaField({
            name: f.name,
            type: f.type,
            options: f.options
          }));
        }
      });
    }

    if (name === "m_messages") {
      const exists = col.schema.fields().find(field => field.name === "contentBrotliBase64");
      if (!exists) {
        col.schema.addField(new SchemaField({
          name: "contentBrotliBase64",
          type: "text",
          options: { description: "brotli:base64:text" }
        }));
      }
    }

    if (name === "t_user_questionnaire_responses") {
      const exists = col.schema.fields().find(field => field.name === "responseBrotliBase64");
      if (!exists) {
        col.schema.addField(new SchemaField({
          name: "responseBrotliBase64",
          type: "text",
          options: { description: "brotli:base64:text" }
        }));
      }
    }

    dao.saveCollection(col);
  });

  console.log("✅ Idempotent Extreme Compression fields added to schema");
}, (db) => {
  // Down migration not implemented for complexity/safety
});
