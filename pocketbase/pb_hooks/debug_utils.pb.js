/// <reference path="../pb_data/types.d.ts" />

routerAdd("GET", "/api/debug/collections", (c) => {
    try {
        const collections = $app.dao().findCollections();
        return c.json(200, collections.map(col => ({ name: col.name })));
    } catch (e) {
        return c.json(500, { error: e.toString() });
    }
});

routerAdd("POST", "/api/debug/create-user", (c) => {
    try {
        // Diagnostic Logging
        // $app.logger().info("Debug Context Keys: " + Object.keys(c).join(", "));

        let email = "";
        let password = "";

        // Try 1: Bind (JSON Body)
        try {
            const data = {};
            c.bind(data);
            if (data.email) {
                email = data.email;
            }
            if (data.password) {
                password = data.password;
            }
        } catch (e) {
            $app.logger().warn("Bind failed: " + e);
        }

        // Try 2: Query Param (standard Go method via JS?)
        // If queryParam failed, maybe use Go's formValue?
        if (!email) {
            try {
                // In some PB versions, c.queryParam is not exposed.
                // Try retrieving from raw URL?
                // request().url.query().get()
                // But let's try formValue
                email = c.formValue("email");
                password = c.formValue("password");
            } catch (e) { }
        }

        $app.logger().info("Debug Create User resolved: " + email);

        if (!email || !password) {
            return c.json(400, {
                code: 400,
                message: "Email and password required (Diagnostic)",
                data: { email: email }
            });
        }

        const usersCol = $app.dao().findCollectionByNameOrId("users");

        try {
            const existing = $app.dao().findAuthRecordByEmail("users", email);
            if (existing) {
                return c.json(200, { message: "User already exists", id: existing.id });
            }
        } catch (_) { }

        const record = new Record(usersCol);
        record.setUsername(email.split("@")[0] + Math.floor(Math.random() * 10000));
        record.setEmail(email);
        record.setPassword(password);

        $app.dao().saveRecord(record);

        return c.json(200, { message: "User created", id: record.id });
    } catch (e) {
        $app.logger().error("Debug Hook Panic: " + e.toString());
        return c.json(500, {
            code: 500,
            message: "Hook Panic: " + e.toString(),
            error: e.toString()
        });
    }
});
