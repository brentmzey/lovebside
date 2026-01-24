/// <reference path="../pb_data/types.d.ts" />

migrate((db) => {
    const collectionName = "s_profiles";

    try {
        const existing = db.findCollectionByNameOrId(collectionName);
        // If found, we could update it, but for now we assume it's fine.
        // Ideally we'd diff and update, but basic 'ensure' is: if missing, create.
        return;
    } catch (e) {
        // Not found, proceed to create
    }

    const collection = new Collection({
        "name": collectionName,
        "type": "base",
        "system": false,
        "schema": [
            {
                "system": false,
                "id": "pb_field_userId",
                "name": "userId",
                "type": "text",
                "required": true,
                "presentable": false,
                "unique": false,
                "options": {
                    "min": null,
                    "max": null,
                    "pattern": ""
                }
            },
            {
                "system": false,
                "id": "pb_field_firstName",
                "name": "firstName",
                "type": "text",
                "required": false,
                "presentable": false,
                "unique": false,
                "options": {
                    "min": null,
                    "max": null,
                    "pattern": ""
                }
            },
            {
                "system": false,
                "id": "pb_field_lastName",
                "name": "lastName",
                "type": "text",
                "required": false,
                "presentable": false,
                "unique": false,
                "options": {
                    "min": null,
                    "max": null,
                    "pattern": ""
                }
            },
            {
                "system": false,
                "id": "pb_field_birthDate",
                "name": "birthDate",
                "type": "text",
                "required": false,
                "presentable": false,
                "unique": false,
                "options": {
                    "min": null,
                    "max": null,
                    "pattern": ""
                }
            },
            {
                "system": false,
                "id": "pb_field_seeking",
                "name": "seeking",
                "type": "select",
                "required": false,
                "presentable": false,
                "unique": false,
                "options": {
                    "maxSelect": 1,
                    "values": [
                        "Friendship",
                        "Relationship",
                        "Both"
                    ]
                }
            }
        ],
        "indexes": [],
        "listRule": "",
        "viewRule": "",
        "createRule": "@request.auth.id != ''",
        "updateRule": "@request.auth.id != ''",
        "deleteRule": "@request.auth.id != ''",
        "options": {}
    });

    return dao.saveCollection(collection);
}, (db) => {
    const dao = new Dao(db);
    const collection = dao.findCollectionByNameOrId("s_profiles");
    return dao.deleteCollection(collection);
})
