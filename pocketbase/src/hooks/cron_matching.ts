/// <reference path="../types/pocketbase.d.ts" />

interface MatchProfile {
    id: string;
    userId: string;
    birthDate: string;
    interests: string[];
    seeking: string;
    location: string;
}

function calculateProustSimilarity(ansA: string, ansB: string): number {
    if (!ansA || !ansB) {
        return 0;
    }
    const tokensA = new Set(ansA.toLowerCase().split(/\s+/));
    const tokensB = new Set(ansB.toLowerCase().split(/\s+/));

    let intersectionCount = 0;
    tokensA.forEach(t => {
        if (tokensB.has(t)) {
            intersectionCount++;
        }
    });

    const unionCount = new Set([...tokensA, ...tokensB]).size;
    if (unionCount === 0) {
        return 0;
    }
    return intersectionCount / unionCount;
}

function runMatchingAlgorithm() {
    const logs: string[] = [];
    let matchCount = 0;
    let updateCount = 0;

    try {
        // 1. Fetch Profiles
        const profileRecords = $app.dao().findRecordsByFilter("s_profiles", "userId != ''", "-id", 100, 0);
        logs.push(`Found ${profileRecords.length} profiles to process.`);

        const profiles: MatchProfile[] = profileRecords.map(r => ({
            id: r.id,
            userId: r.get("userId"),
            birthDate: r.get("birthDate"),
            interests: r.get("interests") || [], // Ensure array
            seeking: r.get("seeking"),
            location: r.get("location")
        }));

        // 2. Fetch Proust Responses
        const responseRecords = $app.dao().findRecordsByFilter("t_user_questionnaire_responses", "user_id != ''", "-created", 1000, 0);

        const userAnswers: { [key: string]: { [qId: string]: string } } = {};

        responseRecords.forEach(r => {
            const uId = r.get("user_id");
            const qId = r.get("question_id");
            const txt = r.get("response");

            if (!userAnswers[uId]) {
                userAnswers[uId] = {};
            }
            userAnswers[uId][qId] = txt;
        });

        // 3. Comparison
        for (let i = 0; i < profiles.length; i++) {
            for (let j = i + 1; j < profiles.length; j++) {
                const p1 = profiles[i];
                const p2 = profiles[j];
                const u1 = p1.userId;
                const u2 = p2.userId;

                if (u1 === u2) {
                    continue;
                }

                // Seeking Compatibility
                const s1 = p1.seeking;
                const s2 = p2.seeking;
                let isCompatible = false;

                if (s1 === "Both" || s2 === "Both") {
                    isCompatible = true;
                } else if (s1 === s2) {
                    isCompatible = true;
                }

                // Allow Friendship/Relationship cross-match if one implies friend? No, seeking is distinct.
                // Assuming "Friendship" seekers only match "Friendship" or "Both".

                if (!isCompatible) {
                    continue;
                }

                // Scoring
                let score = 0;

                // A. Interests (Jaccard)
                const i1 = p1.interests || [];
                const i2 = p2.interests || [];
                const commonInterests = i1.filter(x => i2.includes(x));
                const allInterests = new Set([...i1, ...i2]);

                if (allInterests.size > 0) {
                    score += (commonInterests.length / allInterests.size) * 40;
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
                    const avgSim = proustScore / commonQuestions;
                    score += avgSim * 30;
                }

                // C. Location
                if (p1.location && p2.location && p1.location === p2.location) {
                    score += 30;
                }

                const finalScore = Math.round(score);

                // UPSERT MATCH
                if (finalScore > 10) {
                    try {
                        const existing = $app.dao().findRecordsByFilter(
                            "m_matches",
                            `(user_id = '${u1}' && matched_user_id = '${u2}') || (user_id = '${u2}' && matched_user_id = '${u1}')`,
                            "-created",
                            1,
                            0
                        );

                        let record;
                        if (existing.length > 0) {
                            record = existing[0];
                            updateCount++;
                        } else {
                            const matchCol = $app.dao().findCollectionByNameOrId("m_matches");
                            record = new Record(matchCol);
                            record.set("user_id", u1);
                            record.set("matched_user_id", u2);
                            record.set("status", "pending");
                            matchCount++;
                        }

                        record.set("match_score", finalScore);
                        $app.dao().saveRecord(record);

                    } catch (e: any) {
                        logs.push(`Failed to save match for ${u1}:${u2} - ${e.toString()}`);
                    }
                }
            }
        }
    } catch (e: any) {
        logs.push("Critical Error: " + e.toString());
    }

    return { created: matchCount, updated: updateCount, logs };
}

routerAdd("POST", "/api/crons/trigger-matching", (c) => {
    const result = runMatchingAlgorithm();
    return c.json(200, { status: "success", result });
});
