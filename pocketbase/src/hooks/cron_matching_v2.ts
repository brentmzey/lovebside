/// <reference path="../types/pocketbase.d.ts" />

/**
 * Enhanced Matching Algorithm with Multi-Dimensional Scoring
 * 
 * Combines:
 * 1. Jaccard Similarity (Interests) - 30%
 * 2. Proust Compatibility (Personality) - 25%
 * 3. Location Proximity - 15%
 * 4. Affinity Score (Statistical) - 20%
 * 5. Behavioral Signals - 10%
 * 
 * Total: 100 points
 */

interface MatchProfile {
    id: string;
    userId: string;
    birthDate: string;
    interests: string[];
    values: string[];
    personality_traits: string[]; // Big 5 traits
    seeking: string;
    location: string;
    lat?: number;
    lng?: number;
    relationship_goals: string[];
    lifestyle: string[];
}

interface ProustAnswers {
    [questionId: string]: string;
}

interface MatchingWeights {
    jaccard: number;      // 0.30
    proust: number;       // 0.25
    location: number;     // 0.15
    affinity: number;     // 0.20
    behavioral: number;   // 0.10
}

const WEIGHTS: MatchingWeights = {
    jaccard: 0.30,
    proust: 0.25,
    location: 0.15,
    affinity: 0.20,
    behavioral: 0.10
};

// ========================================
// UTILITY FUNCTIONS
// ========================================

/**
 * Jaccard Similarity: |A ∩ B| / |A ∪ B|
 */
function jaccardSimilarity(setA: string[], setB: string[]): number {
    if (!setA || !setB || (setA.length === 0 && setB.length === 0)) return 0;
    
    const intersection = setA.filter(item => setB.includes(item));
    const union = [...new Set([...setA, ...setB])];
    
    if (union.length === 0) {
        return 0;
    }
    return intersection.length / union.length;
}

/**
 * Cosine Similarity for text-based comparison
 */
function cosineSimilarity(textA: string, textB: string): number {
    if (!textA || !textB) {
        return 0;
    }
    
    const tokensA = textA.toLowerCase().split(/\s+/).filter(t => t.length > 2);
    const tokensB = textB.toLowerCase().split(/\s+/).filter(t => t.length > 2);
    
    const allTokens = [...new Set([...tokensA, ...tokensB])];
    const vecA = allTokens.map(token => tokensA.filter(t => t === token).length);
    const vecB = allTokens.map(token => tokensB.filter(t => t === token).length);
    
    const dotProduct = vecA.reduce((sum, a, i) => sum + a * vecB[i], 0);
    const magA = Math.sqrt(vecA.reduce((sum, a) => sum + a * a, 0));
    const magB = Math.sqrt(vecB.reduce((sum, b) => sum + b * b, 0));
    
    if (magA === 0 || magB === 0) {
        return 0;
    }
    return dotProduct / (magA * magB);
}

/**
 * Haversine Distance (km) between two lat/lng points
 */
function haversineDistance(lat1: number, lng1: number, lat2: number, lng2: number): number {
    const R = 6371; // Earth radius in km
    const dLat = (lat2 - lat1) * Math.PI / 180;
    const dLng = (lng2 - lng1) * Math.PI / 180;
    
    const a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
              Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) *
              Math.sin(dLng / 2) * Math.sin(dLng / 2);
    
    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    return R * c;
}

// ========================================
// SCORING COMPONENTS
// ========================================

/**
 * Component 1: Jaccard Similarity on Interests (30 points)
 */
function scoreInterests(p1: MatchProfile, p2: MatchProfile): number {
    const interestScore = jaccardSimilarity(p1.interests || [], p2.interests || []);
    return interestScore * 30;
}

/**
 * Component 2: Proust Compatibility (25 points)
 */
function scoreProust(ans1: ProustAnswers, ans2: ProustAnswers): number {
    const commonQuestions = Object.keys(ans1).filter(q => ans2[q]);
    
    if (commonQuestions.length === 0) {
        return 0;
    }
    
    let totalSimilarity = 0;
    commonQuestions.forEach(q => {
        totalSimilarity += cosineSimilarity(ans1[q], ans2[q]);
    });
    
    const avgSimilarity = totalSimilarity / commonQuestions.length;
    return avgSimilarity * 25;
}

/**
 * Component 3: Location Proximity (15 points)
 */
function scoreLocation(p1: MatchProfile, p2: MatchProfile): number {
    // Simple location match (city-level)
    if (p1.location && p2.location && p1.location === p2.location) {
        return 15;
    }
    
    // Geographic distance if coordinates available
    if (p1.lat && p1.lng && p2.lat && p2.lng) {
        const distance = haversineDistance(p1.lat, p1.lng, p2.lat, p2.lng);
        
        // Score based on distance: 0-10km = 15pts, 10-25km = 10pts, 25-50km = 5pts, 50+ = 0pts
        if (distance <= 10) {
            return 15;
        }
        if (distance <= 25) {
            return 10;
        }
        if (distance <= 50) {
            return 5;
        }
        return 0;
    }
    
    return 0;
}

/**
 * Component 4: Affinity Score (Statistical Compatibility) - 20 points
 * 
 * Combines:
 * - Values alignment
 * - Personality trait compatibility (Big 5)
 * - Relationship goals alignment
 * - Lifestyle compatibility
 */
function scoreAffinity(p1: MatchProfile, p2: MatchProfile): number {
    let affinityScore = 0;
    let components = 0;
    
    // 4a. Values alignment (25% of affinity = 5 points)
    if (p1.values && p2.values) {
        const valuesScore = jaccardSimilarity(p1.values, p2.values);
        affinityScore += valuesScore * 5;
        components++;
    }
    
    // 4b. Personality traits (25% of affinity = 5 points)
    // Assumption: personality_traits = ["Openness:high", "Conscientiousness:medium", ...]
    if (p1.personality_traits && p2.personality_traits) {
        const traitsScore = jaccardSimilarity(p1.personality_traits, p2.personality_traits);
        affinityScore += traitsScore * 5;
        components++;
    }
    
    // 4c. Relationship goals (25% of affinity = 5 points)
    if (p1.relationship_goals && p2.relationship_goals) {
        const goalsScore = jaccardSimilarity(p1.relationship_goals, p2.relationship_goals);
        affinityScore += goalsScore * 5;
        components++;
    }
    
    // 4d. Lifestyle compatibility (25% of affinity = 5 points)
    if (p1.lifestyle && p2.lifestyle) {
        const lifestyleScore = jaccardSimilarity(p1.lifestyle, p2.lifestyle);
        affinityScore += lifestyleScore * 5;
        components++;
    }
    
    // Return weighted score (max 20 points)
    return components > 0 ? (affinityScore / components) * 4 : 0;
}

/**
 * Component 5: Behavioral Signals (10 points)
 * 
 * Based on:
 * - Profile completeness
 * - Recent activity
 * - Response rate (future)
 */
function scoreBehavioral(p1: MatchProfile, p2: MatchProfile): number {
    let behavioralScore = 0;
    
    // Profile completeness (5 points each)
    const p1Completeness = calculateProfileCompleteness(p1);
    const p2Completeness = calculateProfileCompleteness(p2);
    
    // Reward both having complete profiles
    const avgCompleteness = (p1Completeness + p2Completeness) / 2;
    behavioralScore += avgCompleteness * 10;
    
    return behavioralScore;
}

function calculateProfileCompleteness(p: MatchProfile): number {
    let score = 0;
    let total = 0;
    
    // Check key fields
    if (p.birthDate) { score++; total++; } else { total++; }
    if (p.interests && p.interests.length > 0) { score++; total++; } else { total++; }
    if (p.seeking) { score++; total++; } else { total++; }
    if (p.location) { score++; total++; } else { total++; }
    if (p.values && p.values.length > 0) { score++; total++; } else { total++; }
    if (p.relationship_goals && p.relationship_goals.length > 0) { score++; total++; } else { total++; }
    
    return total > 0 ? score / total : 0;
}

// ========================================
// MAIN MATCHING ALGORITHM
// ========================================

function runEnhancedMatchingAlgorithm() {
    const logs: string[] = [];
    let matchCount = 0;
    let updateCount = 0;
    const startTime = Date.now();

    try {
        // 1. Fetch Profiles
        const profileRecords = $app.dao().findRecordsByFilter("s_profiles", "userId != ''", "-created", 100, 0);
        logs.push(`📊 Found ${profileRecords.length} profiles to process.`);

        const profiles: MatchProfile[] = profileRecords.map(r => ({
            id: r.id,
            userId: r.get("userId"),
            birthDate: r.get("birthDate") || "",
            interests: r.get("interests") || [],
            values: r.get("values") || [],
            personality_traits: r.get("personality_traits") || [],
            seeking: r.get("seeking") || "",
            location: r.get("location") || "",
            lat: r.get("lat") || null,
            lng: r.get("lng") || null,
            relationship_goals: r.get("relationship_goals") || [],
            lifestyle: r.get("lifestyle") || []
        }));

        // 2. Fetch Proust Responses
        const responseRecords = $app.dao().findRecordsByFilter(
            "t_user_questionnaire_responses",
            "user_id != ''",
            "-created",
            1000,
            0
        );

        const userAnswers: { [userId: string]: ProustAnswers } = {};
        responseRecords.forEach(r => {
            const userId = r.get("user_id");
            const questionId = r.get("question_id");
            const response = r.get("response");

            if (!userAnswers[userId]) {
                userAnswers[userId] = {};
            }
            userAnswers[userId][questionId] = response;
        });

        logs.push(`📝 Loaded Proust responses for ${Object.keys(userAnswers).length} users.`);

        // 3. Calculate Matches
        for (let i = 0; i < profiles.length; i++) {
            for (let j = i + 1; j < profiles.length; j++) {
                const p1 = profiles[i];
                const p2 = profiles[j];
                
                if (p1.userId === p2.userId) {
                    continue;
                }

                // Check seeking compatibility
                if (!areSeekingCompatible(p1.seeking, p2.seeking)) continue;

                // Calculate component scores
                const interestsScore = scoreInterests(p1, p2);
                const proustScore = scoreProust(userAnswers[p1.userId] || {}, userAnswers[p2.userId] || {});
                const locationScore = scoreLocation(p1, p2);
                const affinityScore = scoreAffinity(p1, p2);
                const behavioralScore = scoreBehavioral(p1, p2);

                // Final weighted score (0-100)
                const finalScore = Math.round(
                    interestsScore + proustScore + locationScore + affinityScore + behavioralScore
                );

                // Save match if score > threshold
                if (finalScore > 15) {
                    try {
                        saveOrUpdateMatch(p1.userId, p2.userId, finalScore, {
                            interests: Math.round(interestsScore),
                            proust: Math.round(proustScore),
                            location: Math.round(locationScore),
                            affinity: Math.round(affinityScore),
                            behavioral: Math.round(behavioralScore)
                        });
                        matchCount++;
                    } catch (e: any) {
                        logs.push(`❌ Failed to save match ${p1.userId}:${p2.userId} - ${e.message}`);
                    }
                }
            }
        }

        const duration = Date.now() - startTime;
        logs.push(`✅ Completed in ${duration}ms`);
        logs.push(`📈 Created/Updated ${matchCount} matches`);

    } catch (e: any) {
        logs.push(`🚨 Critical Error: ${e.message}`);
    }

    return {
        created: matchCount,
        updated: updateCount,
        logs,
        timestamp: new Date().toISOString()
    };
}

function areSeekingCompatible(seeking1: string, seeking2: string): boolean {
    if (!seeking1 || !seeking2) {
        return false;
    }
    
    // "Both" is compatible with anyone
    if (seeking1 === "Both" || seeking2 === "Both") {
        return true;
    }
    
    // Otherwise must match exactly
    return seeking1 === seeking2;
}

function saveOrUpdateMatch(userId1: string, userId2: string, score: number, breakdown: any) {
    const existing = $app.dao().findRecordsByFilter(
        "m_matches",
        `(user_id = '${userId1}' && matched_user_id = '${userId2}') || (user_id = '${userId2}' && matched_user_id = '${userId1}')`,
        "-created",
        1,
        0
    );

    let record;
    if (existing.length > 0) {
        record = existing[0];
    } else {
        const matchCol = $app.dao().findCollectionByNameOrId("m_matches");
        record = new Record(matchCol);
        record.set("user_id", userId1);
        record.set("matched_user_id", userId2);
        record.set("status", "pending");
    }

    record.set("match_score", score);
    record.set("score_breakdown", JSON.stringify(breakdown)); // Store component scores
    record.set("algorithm_version", "v2.0-affinity");
    record.set("updated", new Date().toISOString());
    
    $app.dao().saveRecord(record);
}

// ========================================
// API ENDPOINTS
// ========================================

routerAdd("POST", "/api/crons/trigger-matching", (c) => {
    const result = runEnhancedMatchingAlgorithm();
    return c.json(200, { status: "success", result });
});

routerAdd("POST", "/api/crons/trigger-matching-incremental", (c) => {
    // Future: Only recalculate for users who updated profiles in last 24h
    return c.json(200, { status: "not_implemented" });
});
