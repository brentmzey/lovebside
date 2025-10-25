# PocketBase Setup & Integration Guide

**Last Updated**: January 19, 2025  
**Status**: Ready for Integration Testing

---

## 🎯 Overview

This guide walks through setting up PocketBase for the B-Side application, including schema creation, permissions configuration, and integration testing.

---

## ✅ Quick Start (5 minutes)

### 1. Start PocketBase

```bash
cd /Users/brentzey/bside/pocketbase
./pocketbase serve
```

**Admin UI**: http://127.0.0.1:8090/_/

**First-time setup**:
1. Create admin account (email + password)
2. This account is for schema management only
3. Save credentials securely

### 2. Verify Server Connects

```bash
# In another terminal
cd /Users/brentzey/bside
./gradlew :server:run
```

Server should start on http://localhost:8080

---

## 🏗️ Schema Setup

### Option A: Manual Setup (Recommended for First Time)

#### Step 1: Create Collections

Log into http://127.0.0.1:8090/_/ and create these collections:

##### 1. s_profiles
```
Type: Base Collection
Name: s_profiles

Fields:
- userId (Relation)
  → Collection: users
  → Cascade delete: YES
  → Required: YES
  → Unique: YES
  
- firstName (Plain Text)
  → Required: YES
  → Min length: 1
  → Max length: 50
  
- lastName (Plain Text)
  → Required: YES
  → Min length: 1
  → Max length: 50
  
- birthDate (Date)
  → Required: YES
  
- bio (Plain Text)
  → Max length: 500
  
- location (Plain Text)
  → Max length: 100
  
- seeking (Select - Single)
  → Required: YES
  → Values: FRIENDSHIP, RELATIONSHIP, BOTH

API Rules:
- List: @request.auth.id != ""
- View: @request.auth.id != "" && (userId = @request.auth.id || @request.auth.id != "")
- Create: @request.auth.id != "" && userId = @request.auth.id
- Update: @request.auth.id != "" && userId = @request.auth.id
- Delete: @request.auth.id != "" && userId = @request.auth.id

Indexes:
- CREATE UNIQUE INDEX idx_profiles_userId ON s_profiles (userId)
- CREATE INDEX idx_profiles_seeking ON s_profiles (seeking)
```

##### 2. s_key_values
```
Type: Base Collection
Name: s_key_values

Fields:
- key (Plain Text)
  → Required: YES
  → Unique: YES
  → Max length: 100
  
- category (Select - Single)
  → Required: YES
  → Values: PERSONALITY, VALUES, INTERESTS, LIFESTYLE
  
- description (Plain Text)
  → Max length: 500
  
- displayOrder (Number)
  → Min: 0
  → Default: 0

API Rules:
- List: (empty = public read)
- View: (empty = public read)
- Create: (null = admin only)
- Update: (null = admin only)
- Delete: (null = admin only)

Indexes:
- CREATE UNIQUE INDEX idx_key_values_key ON s_key_values (key)
- CREATE INDEX idx_key_values_category ON s_key_values (category)
```

##### 3. s_user_values
```
Type: Base Collection
Name: s_user_values

Fields:
- userId (Relation)
  → Collection: users
  → Cascade delete: YES
  → Required: YES
  
- keyValueId (Relation)
  → Collection: s_key_values
  → Required: YES
  
- importance (Number)
  → Required: YES
  → Min: 1
  → Max: 10

API Rules:
- List: @request.auth.id != "" && userId = @request.auth.id
- View: @request.auth.id != "" && userId = @request.auth.id
- Create: @request.auth.id != "" && userId = @request.auth.id
- Update: @request.auth.id != "" && userId = @request.auth.id
- Delete: @request.auth.id != "" && userId = @request.auth.id

Indexes:
- CREATE INDEX idx_user_values_userId ON s_user_values (userId)
- CREATE INDEX idx_user_values_keyValueId ON s_user_values (keyValueId)
- CREATE UNIQUE INDEX idx_user_values_unique ON s_user_values (userId, keyValueId)
```

##### 4. s_prompts
```
Type: Base Collection
Name: s_prompts

Fields:
- text (Plain Text)
  → Required: YES
  → Max length: 200
  
- category (Select - Single)
  → Values: ICEBREAKER, DEEP, FUN, VALUES
  
- isActive (Bool)
  → Default: true

API Rules:
- List: (empty = public read)
- View: (empty = public read)
- Create: (null = admin only)
- Update: (null = admin only)
- Delete: (null = admin only)

Indexes:
- CREATE INDEX idx_prompts_category ON s_prompts (category)
- CREATE INDEX idx_prompts_isActive ON s_prompts (isActive)
```

##### 5. s_user_answers
```
Type: Base Collection
Name: s_user_answers

Fields:
- userId (Relation)
  → Collection: users
  → Cascade delete: YES
  → Required: YES
  
- promptId (Relation)
  → Collection: s_prompts
  → Required: YES
  
- answer (Plain Text)
  → Required: YES
  → Max length: 1000

API Rules:
- List: @request.auth.id != ""
- View: @request.auth.id != ""
- Create: @request.auth.id != "" && userId = @request.auth.id
- Update: @request.auth.id != "" && userId = @request.auth.id
- Delete: @request.auth.id != "" && userId = @request.auth.id

Indexes:
- CREATE INDEX idx_user_answers_userId ON s_user_answers (userId)
- CREATE INDEX idx_user_answers_promptId ON s_user_answers (promptId)
- CREATE UNIQUE INDEX idx_user_answers_unique ON s_user_answers (userId, promptId)
```

##### 6. s_matches
```
Type: Base Collection
Name: s_matches

Fields:
- userId (Relation)
  → Collection: users
  → Required: YES
  
- matchedUserId (Relation)
  → Collection: users
  → Required: YES
  
- compatibilityScore (Number)
  → Required: YES
  → Min: 0
  → Max: 100
  
- sharedValues (JSON)
  
- status (Select - Single)
  → Required: YES
  → Default: PENDING
  → Values: PENDING, ACCEPTED, REJECTED, BLOCKED
  
- lastCalculated (Date & Time)

API Rules:
- List: @request.auth.id != "" && (userId = @request.auth.id || matchedUserId = @request.auth.id)
- View: @request.auth.id != "" && (userId = @request.auth.id || matchedUserId = @request.auth.id)
- Create: (null = system only)
- Update: @request.auth.id != "" && (userId = @request.auth.id || matchedUserId = @request.auth.id)
- Delete: @request.auth.id != "" && userId = @request.auth.id

Indexes:
- CREATE INDEX idx_matches_userId ON s_matches (userId)
- CREATE INDEX idx_matches_matchedUserId ON s_matches (matchedUserId)
- CREATE INDEX idx_matches_score ON s_matches (compatibilityScore)
- CREATE INDEX idx_matches_status ON s_matches (status)
- CREATE UNIQUE INDEX idx_matches_unique ON s_matches (userId, matchedUserId)
```

##### 7. s_migrations
```
Type: Base Collection
Name: s_migrations

Fields:
- version (Number)
  → Required: YES
  → Unique: YES
  
- name (Plain Text)
  → Required: YES
  
- description (Plain Text)
  
- appliedAt (Date & Time)
  
- executionTimeMs (Number)
  → Required: YES

API Rules:
- List: (null = admin only)
- View: (null = admin only)
- Create: (null = system only)
- Update: (null = never)
- Delete: (null = admin only)

Indexes:
- CREATE UNIQUE INDEX idx_migrations_version ON s_migrations (version)
- CREATE INDEX idx_migrations_appliedAt ON s_migrations (appliedAt)
```

### Option B: Automated Setup (Coming Soon)

We'll create a setup script that uses PocketBase Admin API to create collections automatically.

---

## 🌱 Seed Data

### 1. Create Initial Key Values

```bash
# Use the PocketBase admin panel or this script
curl -X POST http://127.0.0.1:8090/api/collections/s_key_values/records \
  -H "Authorization: YOUR_ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "key": "honesty",
    "category": "VALUES",
    "description": "Truth and transparency in relationships",
    "displayOrder": 1
  }'
```

**Quick seed values** (run these in PocketBase admin console):

```javascript
// PERSONALITY traits
["adventurous", "analytical", "creative", "empathetic", "organized", "spontaneous", "introspective", "outgoing"]

// VALUES
["honesty", "loyalty", "independence", "family", "ambition", "compassion", "integrity", "growth"]

// INTERESTS
["travel", "reading", "fitness", "music", "cooking", "art", "technology", "nature"]

// LIFESTYLE
["active", "homebody", "social", "spiritual", "career_focused", "balanced", "minimalist", "adventurer"]
```

### 2. Create Sample Prompts

```javascript
const prompts = [
  { text: "What is your idea of perfect happiness?", category: "DEEP", isActive: true },
  { text: "What is your greatest fear?", category: "DEEP", isActive: true },
  { text: "Which living person do you most admire?", category: "VALUES", isActive: true },
  { text: "What is your greatest extravagance?", category: "FUN", isActive: true },
  { text: "What do you value most in your friends?", category: "VALUES", isActive: true },
  { text: "If you could change one thing about yourself, what would it be?", category: "DEEP", isActive: true },
  { text: "What is your most treasured possession?", category: "ICEBREAKER", isActive: true },
  { text: "What do you consider the most overrated virtue?", category: "VALUES", isActive: true },
  { text: "What is your current state of mind?", category: "ICEBREAKER", isActive: true },
  { text: "What do you appreciate most in a relationship?", category: "VALUES", isActive: true }
];
```

---

## 🧪 Integration Testing

### Test 1: Server → PocketBase Connection

```bash
# Terminal 1: PocketBase
cd pocketbase
./pocketbase serve

# Terminal 2: Server
./gradlew :server:run

# Terminal 3: Test health
curl http://localhost:8080/health
curl http://127.0.0.1:8090/api/health
```

**Expected**: Both return 200 OK

### Test 2: User Registration Flow

```bash
# Register a new user
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@bside.love",
    "password": "SecurePass123!",
    "passwordConfirm": "SecurePass123!",
    "firstName": "Test",
    "lastName": "User",
    "birthDate": "1990-01-01",
    "seeking": "BOTH"
  }'
```

**Expected Response**:
```json
{
  "success": true,
  "data": {
    "token": "eyJhbGc...",
    "user": {
      "id": "...",
      "email": "test@bside.love"
    },
    "profile": {
      "id": "...",
      "firstName": "Test",
      "lastName": "User",
      "birthDate": "1990-01-01",
      "seeking": "BOTH"
    }
  }
}
```

### Test 3: Authentication

```bash
# Login with credentials
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@bside.love",
    "password": "SecurePass123!"
  }'
```

**Expected**: Returns auth token

### Test 4: Protected Endpoint

```bash
# Get current user profile (requires token)
curl -X GET http://localhost:8080/api/v1/users/me \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"
```

**Expected**: Returns user profile data

### Test 5: Values Management

```bash
# Get available key values
curl -X GET http://localhost:8080/api/v1/values \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"

# Set user values
curl -X POST http://localhost:8080/api/v1/values/user \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  -H "Content-Type: application/json" \
  -d '{
    "values": [
      { "keyValueId": "KEY_VALUE_ID_1", "importance": 10 },
      { "keyValueId": "KEY_VALUE_ID_2", "importance": 8 }
    ]
  }'
```

---

## 🔍 Common Issues & Solutions

### Issue 1: "403 Forbidden" on Collection Access

**Cause**: API rules not configured correctly

**Solution**:
1. Check collection API rules in PocketBase admin
2. Verify rule syntax: `@request.auth.id != ""`
3. Ensure user is authenticated (token in header)
4. Check relation field names match (userId vs user_id)

### Issue 2: "404 Collection Not Found"

**Cause**: Collection doesn't exist or wrong name

**Solution**:
1. Verify collection name exactly: `s_profiles` (not `profiles`)
2. Check collection is created in PocketBase admin
3. Restart PocketBase after creating collections

### Issue 3: "Relation field validation failed"

**Cause**: Referenced record doesn't exist

**Solution**:
1. Check userId exists in users collection
2. Verify foreign key IDs are valid
3. Use `expand` parameter to include related records

### Issue 4: Server Can't Connect to PocketBase

**Cause**: PocketBase not running or wrong URL

**Solution**:
```bash
# Check PocketBase is running
ps aux | grep pocketbase

# Check port 8090 is listening
lsof -i :8090

# Verify connection
curl http://127.0.0.1:8090/api/health

# Check server config
cat server/src/main/resources/application.conf
```

### Issue 5: "Unique Constraint Failed"

**Cause**: Trying to create duplicate record

**Solution**:
1. Check for existing record first
2. Use PATCH/update instead of POST/create
3. Verify unique indexes are correct

---

## 🔐 Security Checklist

Before going to production:

- [ ] Change default admin password
- [ ] Set up HTTPS for PocketBase
- [ ] Configure CORS properly (not `*` in production)
- [ ] Review all API rules (principle of least privilege)
- [ ] Set up backup strategy
- [ ] Enable rate limiting
- [ ] Configure proper logging
- [ ] Test all permission scenarios
- [ ] Verify sensitive data is not exposed in logs
- [ ] Set up monitoring and alerts

---

## 📊 Monitoring & Debugging

### Enable PocketBase Debug Logs

```bash
cd pocketbase
./pocketbase serve --debug
```

### Check Server Logs

```bash
# Server logs show all API calls
./gradlew :server:run

# Look for:
# - PocketBase connection status
# - API request/response bodies
# - Authentication failures
# - Permission denied errors
```

### Verify Schema Matches Code

```bash
# Run schema validator
./gradlew :server:run --args="validate-schema"
```

### Query PocketBase Directly

```bash
# Get all collections
curl http://127.0.0.1:8090/api/collections

# Get specific collection schema
curl http://127.0.0.1:8090/api/collections/s_profiles

# Test query with auth
curl "http://127.0.0.1:8090/api/collections/s_profiles/records?filter=(userId='USER_ID')" \
  -H "Authorization: Bearer TOKEN"
```

---

## 🚀 Next Steps

After setup is complete:

1. **Seed more data**: Add comprehensive key values and prompts
2. **Test matching algorithm**: Create test users and calculate matches
3. **Load testing**: Use tools like `k6` or `artillery`
4. **Backup setup**: Configure automated backups
5. **Deploy**: Set up production PocketBase instance
6. **Monitor**: Set up alerting for errors and performance

---

## 📚 Resources

- **PocketBase Docs**: https://pocketbase.io/docs/
- **API Rules Guide**: https://pocketbase.io/docs/api-rules-and-filters/
- **Collection Types**: https://pocketbase.io/docs/collections/
- **JavaScript SDK**: https://github.com/pocketbase/js-sdk
- **Go SDK**: https://github.com/pocketbase/pocketbase (server-side)

---

## 🆘 Getting Help

If you encounter issues:

1. Check PocketBase logs: `pocketbase/logs/`
2. Check server logs: Look for stack traces
3. Verify schema: Compare POCKETBASE_SCHEMA.md with actual collections
4. Test directly: Use curl to isolate client vs server issues
5. Check permissions: Most issues are API rule related

---

**Status**: Ready for testing! 🎉

Start with Test 1 (connection) and work through each test sequentially.
