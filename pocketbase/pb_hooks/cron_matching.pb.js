/// <reference path="../pb_data/types.d.ts" />

// Reusable Matching Logic
function runMatchingAlgorithm(limit = 1000) {
    const logs = [];
    const log = {
        info: (msg) => { $app.logger().info(msg); logs.push(msg); },
        error: (msg) => { $app.logger().error(msg); logs.push("ERROR: " + msg); }
    };

    log.info(`[Matching] Starting algorithm (Limit: ${limit})...`);

    // 1. Fetch all profiles with their User IDs
    const profiles = $app.dao().findRecordsByFilter(
        "s_profiles",
        "seeking != ''",
        "-created", // Newest first
        limit
    );

    log.info(`[Matching DEBUG] Found ${profiles.length} profiles to process.`);

    // 2. Pre-fetch Proust Answers
    const allAnswers = $app.dao().findRecordsByFilter(
        "t_user_questionnaire_responses",
        "answer_text != ''",
        "-updated",
        limit * 10
    );

    // Index: UserId -> { QuestionId: AnswerText }
    const userAnswers = {};
    allAnswers.forEach(r => {
        const uid = r.getString("user_id");
        const qid = r.getString("question_id");
        const txt = r.getString("answer_text").toLowerCase().trim();

        if (!userAnswers[uid]) userAnswers[uid] = {};
        userAnswers[uid][qid] = txt;
    });

    // Helper: Token-based Jaccard Similarity for Text
    const calculateTextSimilarity = (textA, textB) => {
        if (!textA || !textB) return 0;
        if (textA === textB) return 1.0;

        // Tokenize by non-word chars
        const tokensA = new Set(textA.split(/\W+/).filter(w => w.length > 2));
        const tokensB = new Set(textB.split(/\W+/).filter(w => w.length > 2));

        if (tokensA.size === 0 || tokensB.size === 0) return 0;

        const intersection = [...tokensA].filter(x => tokensB.has(x)).length;
        const union = new Set([...tokensA, ...tokensB]).size;

        return intersection / union;
    };

    let newMatches = 0;

    for (let i = 0; i < profiles.length; i++) {
        const profileA = profiles[i];
        const uaId = profileA.getString("user_id");

        for (let j = i + 1; j < profiles.length; j++) {
            const profileB = profiles[j];
            const ubId = profileB.getString("user_id");

            log.info(`[Matching DEBUG] Comparing ${uaId} vs ${ubId}`);

            /* ------------------------------------------------------------------
               SCORING ALGORITHM
               ------------------------------------------------------------------ */
            let score = 0;
            let explanation = [];

            // A. Interests (40%)
            const interestsA = profileA.getStringSlice("interests") || [];
            const interestsB = profileB.getStringSlice("interests") || [];

            if (interestsA.length > 0 && interestsB.length > 0) {
                const intersection = interestsA.filter(x => interestsB.includes(x));
                const union = new Set([...interestsA, ...interestsB]);
                const jaccard = intersection.length / union.size;
                const points = jaccard * 40;
                score += points;
                if (points > 5) explanation.push(`Active interests (${Math.round(jaccard * 100)}%)`);
                log.info(`[Matching DEBUG] Interests Jaccard: ${jaccard}, Points: ${points}`);
            }

            // B. Location (20%)
            // Simple exact match for now. Future: Radius search.
            const locA = profileA.getString("location");
            const locB = profileB.getString("location");
            if (locA && locA === locB) {
                score += 20;
                explanation.push("Same Location");
                log.info(`[Matching DEBUG] Location Match: +20`);
            }

            // C. Proust Compatibility (40%)
            const ansA = userAnswers[uaId] || {};
            const ansB = userAnswers[ubId] || {};
            const questionsA = Object.keys(ansA);
            const commonQ = questionsA.filter(q => (userAnswers[ubId] && userAnswers[ubId][q]));

            if (commonQ.length > 0) {
                let totalSim = 0;
                commonQ.forEach(q => {
                    const sim = calculateTextSimilarity(ansA[q], ansB[q]);
                    totalSim += sim;
                });

                const avgSim = totalSim / commonQ.length;
                const points = avgSim * 40;
                score += points;
                if (points > 5) explanation.push(`Compatible answers (${Math.round(avgSim * 100)}%)`);
                log.info(`[Matching DEBUG] Proust Sim: ${avgSim}, Points: ${points}`);
            }

            log.info(`[Matching DEBUG] Total Score for ${uaId}-${ubId}: ${score}`);

            /* ------------------------------------------------------------------
               PERSISTENCE
               ------------------------------------------------------------------ */
            // Threshold: 15 points
            if (score >= 15) {
                try {
                    // Check existence logic could go here, or rely on Unique Index upsert
                    const collection = $app.dao().findCollectionByNameOrId("m_matches");
                    const existing = $app.dao().findFirstRecordByFilter("m_matches", `userId='${uaId}' && matchedUserId='${ubId}'`);

                    const record = existing || new Record(collection);
                    if (!existing) {
                        record.set("userId", uaId);
                        record.set("matchedUserId", ubId);
                        record.set("status", "pending");
                    }

                    record.set("matchScore", Math.round(score));

                    // We don't have a 'reason' field in schema yet, but good for debug
                    // record.set("reason", explanation.join(", "));

                    $app.dao().saveRecord(record);
                    newMatches++;
                    log.info(`[Matching] Upserted match ${uaId}-${ubId} with score ${score}`);
                } catch (e) {
                    log.error(`[Matching] Failed ${uaId}<->${ubId}: ${e}`);
                }
            }
        }
    }
    log.info(`[Matching] Complete. Upserted ${newMatches} matches.`);
    return { count: newMatches, logs: logs };
}

// Cron Job (Production)
cronAdd("0 0 * * *", "daily_matching", (c) => {
    runMatchingAlgorithm(1000);
});

// Test Endpoint (Development/Testing)
// POST /api/test/trigger-matching
routerAdd("POST", "/api/test/trigger-matching", (c) => {
    // Optional security check
    // const admin = c.get("admin"); 
    // if (!admin) return c.json(403, { error: "Admin only" });

    const result = runMatchingAlgorithm(100);
    return c.json(200, {
        message: "Matching algorithm executed manually",
        matches_processed: result.count,
        debug_logs: result.logs
    });
});
