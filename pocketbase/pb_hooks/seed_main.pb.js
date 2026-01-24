/// <reference path="../pb_data/types.d.ts" />

routerAdd("POST", "/api/seed", (c) => {
    // Attempt to get dao from context or fallback to global $app
    const dao = $app.dao();
    const usersCollection = dao.findCollectionByNameOrId("users");

    // Check if we already have users
    const records = dao.findRecordsByFilter(usersCollection.id, "email != ''", "created", 10, 0);
    if (records.length >= 5) {
        return c.json(200, { message: `Skipping seeding. Found ${records.length} users.` });
    }

    const dummyUsers = [
        {
            email: "alice@test.com",
            name: "Alice Wonderland",
            avatar: "https://images.unsplash.com/photo-1438761681033-6461ffad8d80?ixlib=rb-4.0.3&auto=format&fit=crop&w=150&q=80",
            connection_type: "friendship",
            password: "password123",
            completed_proust_questionnaire: true
        },
        {
            email: "bob@test.com",
            name: "Bob Builder",
            avatar: "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?ixlib=rb-4.0.3&auto=format&fit=crop&w=150&q=80",
            connection_type: "romantic",
            password: "password123",
            completed_proust_questionnaire: true
        },
        {
            email: "charlie@test.com",
            name: "Charlie Chaplin",
            avatar: "https://images.unsplash.com/photo-1547425260-76bcadfb4f2c?ixlib=rb-4.0.3&auto=format&fit=crop&w=150&q=80",
            connection_type: "friendship",
            password: "password123",
            completed_proust_questionnaire: false
        },
        {
            email: "diana@test.com",
            name: "Diana Prince",
            avatar: "https://images.unsplash.com/photo-1494790108377-be9c29b29330?ixlib=rb-4.0.3&auto=format&fit=crop&w=150&q=80",
            connection_type: "romantic",
            password: "password123",
            completed_proust_questionnaire: true
        },
        {
            email: "evan@test.com",
            name: "Evan Alm",
            avatar: "https://images.unsplash.com/photo-1539571696357-5a69c17a67c6?ixlib=rb-4.0.3&auto=format&fit=crop&w=150&q=80",
            connection_type: "friendship",
            password: "password123",
            completed_proust_questionnaire: true
        }
    ];

    let createdCount = 0;

    try {
        dao.runInTransaction((txDao) => {
            for (const u of dummyUsers) {
                try {
                    const existing = txDao.findAuthRecordByEmail(usersCollection.id, u.email);
                    if (existing) continue;
                } catch (_) { }

                const record = new Record(usersCollection);
                record.setEmail(u.email);
                record.setPassword(u.password);
                record.set("name", u.name);
                record.set("connection_type", u.connection_type);
                record.set("completed_proust_questionnaire", u.completed_proust_questionnaire);
                record.setVerified(true);

                txDao.saveRecord(record);
                createdCount++;
            }
        });
    } catch (err) {
        $app.logger().error("[Seed] Transaction failed: " + err);
        return c.json(500, { message: "Seeding failed", error: err.toString() });
    }

    return c.json(200, { message: `Seeding completed. Created ${createdCount} users.` });
});
