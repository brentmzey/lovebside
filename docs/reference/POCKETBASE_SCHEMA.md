# PocketBase Schema & Configuration

## Base URL
```
https://bside.pockethost.io/api/
```

## Collections

### 1. users (System Collection)
Authentication collection with default PocketBase fields plus custom fields.

**Fields:**
- `id` (text, primary key, auto-generated)
- `email` (email, required, unique)
- `password` (password, required, min 8 chars)
- `emailVisibility` (bool, default: false)
- `verified` (bool, default: false)
- `created` (datetime, auto)
- `updated` (datetime, auto)

**API Rules:**
- List: `@request.auth.id != ""`
- View: `@request.auth.id != "" && (id = @request.auth.id || @request.auth.collectionName = "users")`
- Create: Public (for signup)
- Update: `id = @request.auth.id`
- Delete: `id = @request.auth.id`

**Indexes:**
- `email` (unique)

---

### 2. s_profiles
User profile information (one-to-one with users).

**Fields:**
- `id` (text, primary key, auto-generated)
- `created` (datetime, auto)
- `updated` (datetime, auto)
- `userId` (relation to users, required, unique)
- `firstName` (text, required, min 1, max 50)
- `lastName` (text, required, min 1, max 50)
- `birthDate` (date, required)
- `bio` (text, optional, max 500)
- `location` (text, optional, max 100)
- `seeking` (select, required, options: ["FRIENDSHIP", "RELATIONSHIP", "BOTH"])

**API Rules:**
- List: `@request.auth.id != ""`
- View: `@request.auth.id != "" && (userId = @request.auth.id || @request.auth.id != "")`
- Create: `@request.auth.id != "" && userId = @request.auth.id`
- Update: `@request.auth.id != "" && userId = @request.auth.id`
- Delete: `@request.auth.id != "" && userId = @request.auth.id`

**Indexes:**
- `userId` (unique)
- `seeking`

---

### 3. s_key_values
Master list of personality traits and values for matching.

**Fields:**
- `id` (text, primary key, auto-generated)
- `created` (datetime, auto)
- `updated` (datetime, auto)
- `key` (text, required, unique, max 100)
- `category` (select, required, options: ["PERSONALITY", "VALUES", "INTERESTS", "LIFESTYLE"])
- `description` (text, optional, max 500)
- `displayOrder` (number, default: 0)

**API Rules:**
- List: Public (read-only for users)
- View: Public
- Create: Admin only
- Update: Admin only
- Delete: Admin only

**Indexes:**
- `key` (unique)
- `category`

---

### 4. s_user_values
User's selected values and their importance ratings.

**Fields:**
- `id` (text, primary key, auto-generated)
- `created` (datetime, auto)
- `updated` (datetime, auto)
- `userId` (relation to users, required)
- `keyValueId` (relation to s_key_values, required)
- `importance` (number, required, min 1, max 10)

**API Rules:**
- List: `@request.auth.id != "" && userId = @request.auth.id`
- View: `@request.auth.id != "" && userId = @request.auth.id`
- Create: `@request.auth.id != "" && userId = @request.auth.id`
- Update: `@request.auth.id != "" && userId = @request.auth.id`
- Delete: `@request.auth.id != "" && userId = @request.auth.id`

**Indexes:**
- `userId`
- `keyValueId`
- Compound unique: `userId + keyValueId`

---

### 5. s_prompts
Predefined conversation prompts for user profiles.

**Fields:**
- `id` (text, primary key, auto-generated)
- `created` (datetime, auto)
- `updated` (datetime, auto)
- `text` (text, required, max 200)
- `category` (select, optional, options: ["ICEBREAKER", "DEEP", "FUN", "VALUES"])
- `isActive` (bool, default: true)

**API Rules:**
- List: Public
- View: Public
- Create: Admin only
- Update: Admin only
- Delete: Admin only

**Indexes:**
- `category`
- `isActive`

---

### 6. s_user_answers (Proust Questionnaire)
User answers to Proust-style questionnaire.

**Fields:**
- `id` (text, primary key, auto-generated)
- `created` (datetime, auto)
- `updated` (datetime, auto)
- `userId` (relation to users, required)
- `promptId` (relation to s_prompts, required)
- `answer` (text, required, max 1000)

**API Rules:**
- List: `@request.auth.id != ""`
- View: `@request.auth.id != ""`
- Create: `@request.auth.id != "" && userId = @request.auth.id`
- Update: `@request.auth.id != "" && userId = @request.auth.id`
- Delete: `@request.auth.id != "" && userId = @request.auth.id`

**Indexes:**
- `userId`
- `promptId`
- Compound unique: `userId + promptId`

---

### 7. s_matches
Calculated matches between users based on compatibility.

**Fields:**
- `id` (text, primary key, auto-generated)
- `created` (datetime, auto)
- `updated` (datetime, auto)
- `userId` (relation to users, required)
- `matchedUserId` (relation to users, required)
- `compatibilityScore` (number, required, min 0, max 100)
- `sharedValues` (json, optional) - Array of shared value IDs
- `status` (select, required, default: "PENDING", options: ["PENDING", "ACCEPTED", "REJECTED", "BLOCKED"])
- `lastCalculated` (datetime, auto)

**API Rules:**
- List: `@request.auth.id != "" && (userId = @request.auth.id || matchedUserId = @request.auth.id)`
- View: `@request.auth.id != "" && (userId = @request.auth.id || matchedUserId = @request.auth.id)`
- Create: Admin or system only
- Update: `@request.auth.id != "" && (userId = @request.auth.id || matchedUserId = @request.auth.id)`
- Delete: `@request.auth.id != "" && userId = @request.auth.id`

**Indexes:**
- `userId`
- `matchedUserId`
- `compatibilityScore`
- `status`
- Compound: `userId + matchedUserId` (unique)

---

### 8. g_emotion_terms (Emotion Taxonomy)
Master list of verbs/adverbs such as **likes**, **loves**, **loathes**, **hates**, **detests**, etc. Managed centrally so every platform renders the same language.

**Fields:**
- `slug` (text, required, unique, lowercase)
- `label` (text, required)
- `polarity` (select: POSITIVE / NEGATIVE / NEUTRAL)
- `defaultVerb` + `defaultAdverb`
- `intensityMin` / `intensityMax` (number range 0–100)
- `color` (hex text, optional)

**API Rules:** Admin-only (only editors/infra can change taxonomy).

**Indexes:** `slug` unique.

---

### 9. g_expression_modifiers (Dynamic verbs/adverbs)
User-owned collection for creating personalized modifiers/adverbs that can be layered on top of the base emotion terms.

**Fields:**
- `ownerId` (relation → `_pb_users_auth_`, required)
- `slug` + `label` (text, owner-scoped unique)
- `verbOverride`, `adverb` (text, optional)
- `intensityDelta` (number, optional, -100..100)
- `tone` (select: SOFT / BOLD / NEUTRAL)

**API Rules:** `@request.auth.id = ownerId` for list/view/create/update/delete. Every modifier is private to its creator.

**Indexes:** Compound unique on (`ownerId`, `slug`).

---

### 10. g_items (Graph Nodes)
Represents anything a user can emote about: another person, a place, a piece of media, or an abstract idea.

**Fields:**
- `ownerId` (relation → `_pb_users_auth_`, required)
- `title` (text, required)
- `category` (select: PERSON / ITEM / PLACE / MEMORY / MEDIA / OTHER)
- `profileId` (relation → `s_profiles`, optional, ties an item to an actual profile)
- `summary` (text, optional)
- `referenceUrl` (text, optional)

**API Rules:** `@request.auth.id = ownerId` for all operations.

**Indexes:** (`ownerId`, `category`).

---

### 11. g_emotion_edges (Graph Edges)
The heart of the graph: connects a user (and optionally their profile) to an item or another profile with a particular emotion term.

**Fields:**
- `ownerId` (relation → `_pb_users_auth_`, required)
- `subjectProfileId` (relation → `s_profiles`, optional)
- `targetKind` (select: PERSON / ITEM / MEMORY / EXPERIENCE)
- `targetProfileId` or `targetItemId` (relation, optional depending on targetKind)
- `emotionTermId` (relation → `g_emotion_terms`, required)
- `customVerb`, `customAdverb` (text, optional overrides)
- `intensity` (number, 0–100)
- `moment` (date, optional)
- `contextTags` (json array of strings)
- `narrative` (rich text)

**API Rules:** owner-only – `@request.auth.id = ownerId` across list/view/create/update/delete.

**Indexes:**
- (`ownerId`, `emotionTermId`)
- (`ownerId`, `targetKind`, `targetItemId`, `targetProfileId`)

---

### 12. g_edge_modifiers (Edge ↔ Modifier XRef)
Many-to-many join table between edges and user-defined modifiers so you can stack verbs/adverbs per connection.

**Fields:**
- `ownerId` (relation → `_pb_users_auth_`, required)
- `edgeId` (relation → `g_emotion_edges`, required)
- `modifierId` (relation → `g_expression_modifiers`, required)
- `emphasis` (number, optional weighting)
- `sequence` (number, optional ordering)

**API Rules:** `@request.auth.id = ownerId` for every verb.

**Indexes:** Unique (`edgeId`, `modifierId`).

---

## Setup Instructions

### Quick Diagnostics on PocketHost

Run the bundled Kotlin diagnostics to verify that the hosted PocketBase instance exposes the public collections before running the mobile/web client:

```bash
./gradlew :pocketbase-kt-sdk:jvmTest --tests io.pocketbase.PocketBaseNetworkTest
```

Key expectations:
- ✅ `healthEndpointReturnsOk` – `/api/health` is reachable and returns HTTP 200.
- ✅ `systemUsersCollectionIsReachable` – `_pb_users_auth_` can be listed anonymously, proving the API gateway is healthy.
- ⚠️ `publicCollectionsReportDiagnosedStatus` – prints remediation guidance and marks the test as *skipped* whenever collections such as `s_prompts` or `s_key_values` are missing or not publicly readable. The skip message references this document for fixes.

Manual curl checks are also helpful:

```bash
curl -s https://bside.pockethost.io/api/collections/s_prompts/records | jq
```

If the response contains `{ "message": "Missing collection context." }`, the collection does not exist in the hosted instance—apply the migrations in `pocketbase/migrations/` or recreate it through the PocketBase Admin UI. If you receive `403 Forbidden`, update the List/View rules so that the onboarding experience can fetch the public catalog, or adjust the client to authenticate before reading.

### 1. Create Collections in PocketBase Admin

1. Log in to https://bside.pockethost.io/_/
2. Navigate to "Collections" in the sidebar
3. Create each collection using the schema above
4. Set up the API rules exactly as specified

> 🛠️ **Shortcut:** apply `pocketbase/migrations/20251130000000_add_emotion_graph.js` via the PocketBase CLI (`./pocketbase migrate`) to create all graph collections, indexes, and rules in one shot. The same migration is tracked in Git so environments stay consistent.

#### Emotion graph checklist (manual UI)
If you prefer the admin UI, create the new collections in this order so relations resolve immediately:
1. **g_emotion_terms** – paste the verbs/adverbs taxonomy (see seeding script below) and lock the rules to admins only.
2. **g_expression_modifiers** – add the `ownerId` relation (required!), slug, verb overrides, and tone select.
3. **g_items** – relation back to `_pb_users_auth_`, optional relation to `s_profiles`, and a select field for the category.
4. **g_emotion_edges** – reference both `g_emotion_terms` and `g_items`, enable JSON for `contextTags`, and copy the owner-only rules verbatim.
5. **g_edge_modifiers** – acts as the join table; re-use the owner-only rule and add the compound unique index on (`edgeId`, `modifierId`).

This ordering guarantees every relation dropdown offers the correct target collection without having to backtrack.

### Running migrations from this repo

| Environment | Command |
|-------------|---------|
| Local dev PocketBase | `cd pocketbase && ./pocketbase migrate up --dir migrations` |
| Docker compose | `docker compose run --rm pocketbase ./pocketbase migrate up --dir migrations` |
| Remote PocketHost (via admin shell) | Upload `pocketbase/migrations/*.js` then run `./pocketbase migrate up --dir migrations` from the PocketHost console. |

The new `20251130000000_add_emotion_graph.js` migration is idempotent; re-running it simply ensures every collection exists with the correct indexes/rules. Use `./pocketbase migrate status` to verify.

### 2. Seed Initial Data

#### Key Values (s_key_values)
```javascript
// Run in PocketBase console or via API
const categories = {
    PERSONALITY: [
        { key: "adventurous", description: "Seeks new experiences and thrills" },
        { key: "analytical", description: "Logical and methodical thinker" },
        { key: "creative", description: "Artistic and imaginative" },
        { key: "empathetic", description: "Sensitive to others' emotions" },
        { key: "organized", description: "Structured and planned" }
    ],
    VALUES: [
        { key: "honesty", description: "Truth and transparency" },
        { key: "loyalty", description: "Commitment and faithfulness" },
        { key: "independence", description: "Self-reliance and autonomy" },
        { key: "family", description: "Family bonds and traditions" },
        { key: "ambition", description: "Drive and achievement" }
    ],
    INTERESTS: [
        { key: "travel", description: "Exploring new places" },
        { key: "reading", description: "Books and literature" },
        { key: "fitness", description: "Health and exercise" },
        { key: "music", description: "Musical appreciation" },
        { key: "cooking", description: "Culinary arts" }
    ],
    LIFESTYLE: [
        { key: "active", description: "Physical and social activity" },
        { key: "homebody", description: "Prefers home environment" },
        { key: "social", description: "Enjoys group activities" },
        { key: "spiritual", description: "Values spiritual practices" },
        { key: "career_focused", description: "Professional ambitions" }
    ]
};
```

#### Prompts (s_prompts)
```javascript
const prompts = [
    { text: "What is your idea of perfect happiness?", category: "DEEP" },
    { text: "What is your greatest fear?", category: "DEEP" },
    { text: "What is the trait you most deplore in yourself?", category: "DEEP" },
    { text: "What is the trait you most deplore in others?", category: "DEEP" },
    { text: "Which living person do you most admire?", category: "VALUES" },
    { text: "What is your greatest extravagance?", category: "FUN" },
    { text: "What is your current state of mind?", category: "ICEBREAKER" },
    { text: "What do you consider the most overrated virtue?", category: "VALUES" },
    { text: "On what occasion do you lie?", category: "DEEP" },
    { text: "What do you most dislike about your appearance?", category: "DEEP" }
];
```

#### Emotion Terms (g_emotion_terms)
```javascript
const emotions = [
    { slug: "likes", label: "likes", polarity: "POSITIVE", defaultVerb: "likes", defaultAdverb: "gently", intensityMin: 15, intensityMax: 35, color: "#7BC7C3" },
    { slug: "loves", label: "loves", polarity: "POSITIVE", defaultVerb: "loves", defaultAdverb: "deeply", intensityMin: 60, intensityMax: 90, color: "#B26BCE" },
    { slug: "adores", label: "adores", polarity: "POSITIVE", defaultVerb: "adores", defaultAdverb: "wholeheartedly", intensityMin: 75, intensityMax: 100, color: "#FFB2C7" },
    { slug: "loathes", label: "loathes", polarity: "NEGATIVE", defaultVerb: "loathes", defaultAdverb: "viscerally", intensityMin: 55, intensityMax: 85, color: "#E55B73" },
    { slug: "hates", label: "hates", polarity: "NEGATIVE", defaultVerb: "hates", defaultAdverb: "passionately", intensityMin: 70, intensityMax: 95, color: "#C02E4F" },
    { slug: "detests", label: "detests", polarity: "NEGATIVE", defaultVerb: "detests", defaultAdverb: "utterly", intensityMin: 80, intensityMax: 100, color: "#8D1739" }
];

emotions.forEach((emotion) => {
    await pb.collection("g_emotion_terms").create(emotion);
});
```

After seeding the taxonomy, each user can bootstrap their own modifiers with a simple script:
```javascript
const modifiers = [
    { slug: "softly", label: "softly", adverb: "softly", tone: "SOFT" },
    { slug: "playfully", label: "playfully", adverb: "playfully", tone: "SOFT" },
    { slug: "intently", label: "intently", adverb: "intently", tone: "BOLD" }
];

modifiers.forEach((modifier) => {
    await pb.collection("g_expression_modifiers").create({ ...modifier, ownerId: pb.authStore.model.id });
});
```

### 3. Configure CORS and Security

In PocketBase settings:
```json
{
    "cors": {
        "enabled": true,
        "allowedOrigins": ["*"],
        "allowedMethods": ["GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"],
        "allowedHeaders": ["*"],
        "exposedHeaders": ["*"],
        "maxAge": 86400
    }
}
```

### 4. API Endpoints

#### Authentication
- **POST** `/api/collections/users/auth-with-password`
  - Body: `{ "identity": "email", "password": "password" }`
  - Returns: Auth token and user record

- **POST** `/api/collections/users/records`
  - Body: `{ "email": "...", "password": "...", "passwordConfirm": "..." }`
  - Returns: Created user record

#### Profiles
- **GET** `/api/collections/s_profiles/records?filter=(userId='USER_ID')`
- **POST** `/api/collections/s_profiles/records`
- **PATCH** `/api/collections/s_profiles/records/:id`

#### Values
- **GET** `/api/collections/s_key_values/records`
- **GET** `/api/collections/s_user_values/records?filter=(userId='USER_ID')`
- **POST** `/api/collections/s_user_values/records`
- **PATCH** `/api/collections/s_user_values/records/:id`

#### Matches
- **GET** `/api/collections/s_matches/records?filter=(userId='USER_ID' || matchedUserId='USER_ID')`
- **PATCH** `/api/collections/s_matches/records/:id`

#### Emotion Graph
- **POST** `/api/collections/g_items/records`
  - Body: `{ "ownerId": "USER_ID", "title": "Saxophone", "category": "ITEM" }`
- **POST** `/api/collections/g_emotion_edges/records`
  - Body: `{ "ownerId": "USER_ID", "targetItemId": "ITEM_ID", "emotionTermId": "loves", "intensity": 88 }`
- **POST** `/api/collections/g_edge_modifiers/records`
  - Body: `{ "ownerId": "USER_ID", "edgeId": "EDGE_ID", "modifierId": "softly" }`

All three endpoints enforce `@request.auth.id = ownerId`, so make sure the bearer token matches the owner.

### 5. Permissions Checklist

- [ ] Users can only see their own profile
- [ ] Users can see other users' profiles for matches
- [ ] Users can only modify their own data
- [ ] Key values are read-only for users
- [ ] Prompts are read-only for users
- [ ] Matches can be viewed by both users
- [ ] User answers visible to matched users only
- [ ] Admin can manage all collections

### 6. Indexes for Performance

Ensure these compound indexes exist:
- `s_user_values`: (`userId`, `keyValueId`)
- `s_user_answers`: (`userId`, `promptId`)
- `s_matches`: (`userId`, `matchedUserId`, `compatibilityScore`)

### 7. Backup Strategy

- Daily automated backups
- Before schema changes
- Before data migrations

## Testing

### Test User Creation
```bash
curl -X POST https://bside.pockethost.io/api/collections/users/records \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "Test1234!",
    "passwordConfirm": "Test1234!"
  }'
```

### Test Authentication
```bash
curl -X POST https://bside.pockethost.io/api/collections/users/auth-with-password \
  -H "Content-Type: application/json" \
  -d '{
    "identity": "test@example.com",
    "password": "Test1234!"
  }'
```

### Test Profile Creation
```bash
curl -X POST https://bside.pockethost.io/api/collections/s_profiles/records \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "userId": "USER_ID",
    "firstName": "Test",
    "lastName": "User",
    "birthDate": "1990-01-01",
    "seeking": "BOTH"
  }'
```

## Troubleshooting

### Common Issues

1. **403 Forbidden**: Check API rules match userId with auth.id
2. **404 Not Found**: Verify collection names have correct prefix
3. **Empty Results**: Check filter syntax `(field='value')`
4. **CORS Errors**: Verify CORS settings in PocketBase admin
5. **Token Expired**: Tokens expire after set time, re-authenticate

### Debug Mode

Enable in PocketBase:
```bash
# In PocketBase CLI
--debug
```

View logs in PocketHost dashboard under "Logs" tab.

## Migration Path

If updating from older schema:
1. Export existing data
2. Create new collections
3. Migrate data with transformation
4. Update API rules
5. Test thoroughly
6. Deploy

## Monitoring

Track these metrics:
- Auth success/failure rate
- Average response times per endpoint
- Cache hit rates
- Match calculation performance
- User engagement with prompts

---

**Last Updated**: January 2025
**Schema Version**: 1.0.0
**PocketBase Version**: 0.22.x+
