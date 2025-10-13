# PocketBase Schema & Configuration

## Base URL
```
https://bside.pockethost.io/
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

## Setup Instructions

### 1. Create Collections in PocketBase Admin

1. Log in to https://bside.pockethost.io/_/
2. Navigate to "Collections" in the sidebar
3. Create each collection using the schema above
4. Set up the API rules exactly as specified

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
