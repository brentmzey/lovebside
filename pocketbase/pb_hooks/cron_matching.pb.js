/// <reference path="../pb_data/types.d.ts" />

cronAdd("0 0 * * *", "calculate_affinity", (c) => {
    // 1. Fetch all profiles with their User IDs
    const profiles = $app.dao().findRecordsByFilter(
        "s_profiles",
        "seeking != ''",
        "-created",
        1000
    );

    $app.logger().info(`[Matching] Starting affinity calculation for ${profiles.length} profiles.`);

    // --- PRE-FETCH PROUST ANSWERS ---
    // Optimization: Fetch all answers for these users to avoid N+1 queries.
    // Since we can't easily do "IN (...)" for many IDs, we'll fetch all recent answers or just ALL if small scale.
    // For now, let's just fetch all (MVP) or use a smarter filter if possible.
    // Warning: This scales poorly. Better approach for Prod: Filter by "updated > yesterday" if running daily, 
    // but we need FULL state.
    // Let's assume < 10k answers for now.

    const allAnswers = $app.dao().findRecordsByFilter(
        "t_user_questionnaire_responses",
        "answer_text != ''", // valid answers
        "-updated",
        5000 // Limit
    );

    // index by userId -> { questionId: answerText }
    const userAnswers = {}; // Map<UserId, Map<QuestionId, String>>

    allAnswers.forEach(r => {
        const uid = r.getString("user_id");
        const qid = r.getString("question_id");
        const txt = r.getString("answer_text").toLowerCase().trim();

        if (!userAnswers[uid]) userAnswers[uid] = {};
        userAnswers[uid][qid] = txt;
    });

    for (let i = 0; i < profiles.length; i++) {
        const profileA = profiles[i];
        const uaId = profileA.getString("user_id");

        for (let j = i + 1; j < profiles.length; j++) {
            const profileB = profiles[j];
            const ubId = profileB.getString("user_id");

            // --- SCORING ---
            let score = 0;

            // A. Interests (40%)
            const interestsA = profileA.getStringSlice("interests") || [];
            const interestsB = profileB.getStringSlice("interests") || [];

            if (interestsA.length > 0 && interestsB.length > 0) {
                const intersection = interestsA.filter(x => interestsB.includes(x));
                const union = new Set([...interestsA, ...interestsB]);
                const jaccard = intersection.length / union.size;
                score += (jaccard * 40);
            }

            // B. Location (20%)
            if (profileA.getString("location") && profileA.getString("location") === profileB.getString("location")) {
                score += 20;
            }

            // C. Proust Compatibility (40%)
            // Compare answers for same questions
            const ansA = userAnswers[uaId] || {};
            const ansB = userAnswers[ubId] || {};

            // Find common questions
            const questionsA = Object.keys(ansA);
            const questionsB = Object.keys(ansB);
            const commonQ = questionsA.filter(q => questionsB.includes(q));

            if (commonQ.length > 0) {
                let matchCount = 0;
                commonQ.forEach(q => {
                    const txtA = ansA[q];
                    const txtB = ansB[q];
                    // Simple logic: key phrase match or exact match?
                    // Let's do partial match or exact.
                    // If exact match (rare for free text): 100%
                    // If one contains the other (e.g. "Paris" in "I love Paris"): 50%
                    if (txtA === txtB) {
                        matchCount += 1.0;
                    } else if (txtA.includes(txtB) || txtB.includes(txtA)) {
                        matchCount += 0.5;
                    }
                });

                // Normalize by number of common questions
                const questionScore = (matchCount / commonQ.length) * 40;
                score += questionScore;
            }

            // --- THRESHOLD ---
            if (score >= 15) { // Lower threshold to allow partial interest matches
                try {
                    // Check if match already exists to avoid unique constraint error spam (optional but cleaner)
                    // Actually, let's just Try/Catch for upsert behavior if unique index exists.
                    // Correct collection name: m_matches (standardized)
                    const record = new Record($app.dao().findCollectionByNameOrId("m_matches"));
                    record.set("userId", uaId);
                    record.set("matchedUserId", ubId);
                    record.set("matchScore", Math.round(score));
                    record.set("status", "pending");
                    // Add reasoning for UI explanation?
                    // record.set("reason", `Interests: ${matchCount} common`); 

                    $app.dao().saveRecord(record);
                    $app.logger().info(`[Matching] New Match: ${uaId} <-> ${ubId} (Score: ${Math.round(score)})`);
                } catch (e) {
                    // If error is not unique constraint, log it.
                    // PocketBase errors often contain "integrity constraint"
                    const msg = e.toString();
                    if (!msg.includes("UNIQUE constraint failed") && !msg.includes("integrity constraint")) {
                        $app.logger().error(`[Matching] Failed to save match ${uaId} <-> ${ubId}: ${msg}`);
                    }
                }
            }
        }
    }
    $app.logger().info("[Matching] Daily calculation complete.");
});
