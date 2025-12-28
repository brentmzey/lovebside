"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
/// <reference path="../types/pocketbase.d.ts" />
const results_1 = require("../types/results");
// Token-based Jaccard Similarity for Proust Answers
function calculateProustSimilarity(ansA, ansB) {
    if (!ansA || !ansB)
        return 0;
    const tokensA = new Set(ansA.toLowerCase().split(/\s+/));
    const tokensB = new Set(ansB.toLowerCase().split(/\s+/));
    // Intersection
    const intersection = new Set([...tokensA].filter(x => tokensB.has(x)));
    // Union
    const union = new Set([...tokensA, ...tokensB]);
    if (union.size === 0)
        return 0;
    return intersection.size / union.size;
}
function runMatchingAlgorithm() {
    const logs = [];
    let matchCount = 0;
    try {
        // 1. Fetch Profiles
        // Note: findRecordsByFilter returns Record[]
        const profiles = $app.dao().findRecordsByFilter("s_profiles", "seeking != ''", "-created", 100, 0);
        logs.push(`Found ${profiles.length} profiles to process.`);
        // 2. Fetch Proust Responses (Optimized: Fetch all and map by User)
        // Since we can't easily do complex joins in one go efficiently without SQL, we fetch recent responses
        const responses = $app.dao().findRecordsByFilter("t_user_questionnaire_responses", "", "-created", 500, 0);
        // Map: UserId -> { QuestionId -> AnswerText }
        const userAnswers = {};
        responses.forEach(r => {
            const uId = r.get("user_id");
            const qId = r.get("question_id");
            const txt = r.get("answer_text");
            if (!userAnswers[uId])
                userAnswers[uId] = {};
            userAnswers[uId][qId] = txt;
        });
        // 3. O(N^2) Comparison (Naive for MVP)
        for (let i = 0; i < profiles.length; i++) {
            for (let j = i + 1; j < profiles.length; j++) {
                const p1 = profiles[i];
                const p2 = profiles[j];
                const u1 = p1.get("userId");
                const u2 = p2.get("userId");
                logs.push(`Comparing ${u1} vs ${u2}`);
                // -- SCORING LOGIC --
                let score = 0;
                // A. Interests (Jaccard)
                // Goja: .getStringSlice or direct array access?
                const i1 = p1.get("interests") || [];
                const i2 = p2.get("interests") || [];
                const intersection = i1.filter(x => i2.includes(x));
                const union = new Set([...i1, ...i2]).size;
                if (union > 0) {
                    score += (intersection.length / union) * 40; // 40 pts max
                }
                // B. Proust Compatibility
                const ans1 = userAnswers[u1] || {};
                const ans2 = userAnswers[u2] || {};
                let proustScore = 0;
                let commonQuestions = 0;
                const qIds = Object.keys(ans1);
                qIds.forEach(q => {
                    if (ans2[q]) {
                        commonQuestions++;
                        proustScore += calculateProustSimilarity(ans1[q], ans2[q]);
                    }
                });
                if (commonQuestions > 0) {
                    // Normalize simple average for now
                    const avgSim = proustScore / commonQuestions;
                    logs.push(`  Proust Similarity: ${avgSim.toFixed(2)} on ${commonQuestions} Qs`);
                    score += avgSim * 30; // 30 pts max
                }
                // C. Location (Placeholder)
                // If locations match string
                if (p1.get("location") === p2.get("location")) {
                    score += 30;
                }
                logs.push(`  Total Score: ${score}`);
                // -- UPSERT MATCH --
                if (score > 10) { // Threshold
                    // Check if match exists
                    // Dao logic needed... skipping robust upsert for brevity of initial port
                    // Real implementation should check m_matches for existing record
                    try {
                        const matchCol = $app.dao().findCollectionByNameOrId("m_matches");
                        const match = new Record(matchCol);
                        match.set("userId", u1);
                        match.set("matchedUserId", u2);
                        match.set("matchScore", Math.round(score));
                        match.set("status", "pending");
                        $app.dao().saveRecord(match);
                        matchCount++;
                    }
                    catch (e) {
                        logs.push("  Failed to save match: " + e);
                    }
                }
            }
        }
    }
    catch (e) {
        logs.push("Critical Error: " + e.toString());
    }
    return { count: matchCount, logs };
}
routerAdd("POST", "/api/test/trigger-matching", (c) => {
    const result = runMatchingAlgorithm();
    return c.json(200, (0, results_1.success)(result, "Matching completed"));
});
