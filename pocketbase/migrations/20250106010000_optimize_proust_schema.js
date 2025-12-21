/// <reference path="../pb_data/types.d.ts" />

migrate((db) => {
    const dao = new Dao(db);
    const collection = dao.findCollectionByNameOrId("t_user_questionnaire_responses");

    // 1. Change 'response' field type from 'editor' to 'text'
    // This allows raw string processing for NLP/Matching
    const responseField = collection.schema.getFieldByName("response");
    responseField.type = "text";
    // Field options might need clearing if moving from editor to text?
    // Editor has no special options usually, but text might have max.
    // Let's assume defaults are safe.

    dao.saveCollection(collection);

    // 2. Add Indices for Performance
    // "CREATE INDEX idx_response_user ON t_user_questionnaire_responses (user_id)"
    // "CREATE INDEX idx_response_question ON t_user_questionnaire_responses (question_id)"

    // We can execute raw SQL for indices
    db.newQuery("CREATE INDEX IF NOT EXISTS idx_response_user ON t_user_questionnaire_responses (user_id)").execute();
    db.newQuery("CREATE INDEX IF NOT EXISTS idx_response_question ON t_user_questionnaire_responses (question_id)").execute();

}, (db) => {
    // Revert
    const dao = new Dao(db);
    try {
        const collection = dao.findCollectionByNameOrId("t_user_questionnaire_responses");
        const responseField = collection.schema.getFieldByName("response");
        responseField.type = "editor";
        dao.saveCollection(collection);

        db.newQuery("DROP INDEX IF EXISTS idx_response_user").execute();
        db.newQuery("DROP INDEX IF EXISTS idx_response_question").execute();
    } catch (e) {
        // collection might be deleted
    }
})
