"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
/// <reference path="../types/pocketbase.d.ts" />
const results_1 = require("../types/results");
routerAdd("GET", "/api/debug/collections", (c) => {
    try {
        const collections = $app.dao().findCollections();
        return c.json(200, (0, results_1.success)(collections.map(col => ({
            id: col.id,
            name: col.name,
            type: col.type
        }))));
    }
    catch (e) {
        return c.json(500, (0, results_1.failure)(e.toString(), 500));
    }
});
routerAdd("POST", "/api/debug/create-user", (c) => {
    try {
        let email = "";
        let password = "";
        // Try 1: Bind (JSON Body)
        try {
            const data = {};
            c.bind(data);
            if (data.email)
                email = data.email;
            if (data.password)
                password = data.password;
        }
        catch (e) {
            // $app.logger().warn("Bind failed: " + e);
        }
        // Try 2: Query Param (Goja URL access)
        if (!email) {
            try {
                // Accessing underlying Go http.Request -> URL -> Query()
                const req = c.request();
                const url = req.url;
                const query = url.query();
                // $app.logger().info("Query Keys: " + Object.keys(query).join(", "));
                // Try .get(), .Get()
                try {
                    email = query.get("email");
                }
                catch (e) { }
                if (!email) {
                    try {
                        email = query.Get("email");
                    }
                    catch (e) { }
                }
                try {
                    password = query.get("password");
                }
                catch (e) { }
                if (!password) {
                    try {
                        password = query.Get("password");
                    }
                    catch (e) { }
                }
            }
            catch (e) {
                $app.logger().warn("Query access failed: " + e.toString());
            }
        }
        // Try 3: Form Value
        if (!email) {
            try {
                email = c.formValue("email");
                password = c.formValue("password");
            }
            catch (e) { }
        }
        $app.logger().info("Debug Create User resolved: " + email);
        if (!email || !password) {
            return c.json(400, (0, results_1.failure)("Email and password required", 400, { email }));
        }
        const usersCol = $app.dao().findCollectionByNameOrId("users");
        let existingId = null;
        try {
            const existing = $app.dao().findAuthRecordByEmail("users", email);
            if (existing) {
                existingId = existing.id;
            }
        }
        catch (_) { }
        if (existingId) {
            return c.json(200, (0, results_1.success)({ id: existingId }, "User already exists"));
        }
        const record = new Record(usersCol);
        record.setUsername(email.split("@")[0] + Math.floor(Math.random() * 10000));
        record.setEmail(email);
        record.setPassword(password);
        $app.dao().saveRecord(record);
        return c.json(200, (0, results_1.success)({ id: record.id }, "User created"));
    }
    catch (e) {
        $app.logger().error("Debug Hook Panic: " + e.toString());
        return c.json(500, (0, results_1.failure)("Hook Panic: " + e.toString(), 500));
    }
});
