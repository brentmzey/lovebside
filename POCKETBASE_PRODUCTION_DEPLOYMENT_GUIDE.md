# PocketBase Production Deployment Guide

## Complete Step-by-Step Guide for PocketHost Deployment

Generated: February 1, 2026

---

## Table of Contents
1. [Current Status Overview](#current-status-overview)
2. [Step 1: Inspect Local Schema](#step-1-inspect-local-schema)
3. [Step 2: Export Schema](#step-2-export-schema)
4. [Step 3: Test Migrations Locally](#step-3-test-migrations-locally)
5. [Step 4: Prepare for Production](#step-4-prepare-for-production)
6. [Step 5: Deploy to PocketHost](#step-5-deploy-to-pockethost)
7. [Step 6: Verify Production](#step-6-verify-production)
8. [Rollback Procedures](#rollback-procedures)
9. [Schema Documentation](#schema-documentation)

---

## Current Status Overview

### ✅ Local Database Status
- **Location**: `pocketbase/pb_data/data.db`
- **Migrations Directory**: `pocketbase/pb_migrations/`
- **Latest Migration**: `1769984709_collections_snapshot.js`
- **Total Collections**: 20 (5 system + 15 custom)
- **All Migrations Applied**: ✅ Yes

### Collections Summary

#### System Collections (5)
1. `_mfas` - Multi-factor authentication
2. `_otps` - One-time passwords
3. `_externalAuths` - OAuth providers
4. `_authOrigins` - Authentication origins
5. `_superusers` - Admin users

#### Auth Collections (1)
6. `t_user` - User authentication and profiles

#### Application Collections (14)
7. `pb_migrations` - Migration tracking
8. `t_proust_questionnaire` - Questionnaire metadata
9. `m_conversations` - Messaging conversations
10. `m_messages` - Chat messages
11. `m_conversation_participants` - Conversation members
12. `t_user_questionnaire_responses` - User responses
13. `m_read_receipts` - Message read status
14. `t_proust_question` - Questionnaire questions
15. `t_tenant_property` - System properties
16. `t_user_property` - User preferences
17. `m_matches` - User matching
18. `m_typing_status` - Real-time typing indicators
19. `s_profiles` - Extended user profiles
20. `m_reactions` - Message reactions
21. `m_presence` - Online presence

---

## Step 1: Inspect Local Schema

### 1.1 Check Database Tables
```bash
cd pocketbase
sqlite3 pb_data/data.db "SELECT name FROM sqlite_master WHERE type='table' ORDER BY name;"
```

### 1.2 List All Collections
```bash
sqlite3 pb_data/data.db "SELECT id, name, type FROM _collections ORDER BY name;"
```

### 1.3 View Applied Migrations
```bash
sqlite3 pb_data/data.db "SELECT file, applied FROM _migrations ORDER BY applied DESC LIMIT 10;"
```

**Expected Output:**
```
1738368000_idempotent_schema_complete.js|2026-02-01 XX:XX:XX
```

---

## Step 2: Export Schema

### 2.1 Generate Collections Snapshot (JUST COMPLETED ✅)
```bash
cd pocketbase
./pocketbase migrate collections --dir=./pb_data --migrationsDir=./pb_migrations
```

**Result:** Created `pb_migrations/1769984709_collections_snapshot.js`

### 2.2 View the Latest Snapshot
```bash
cat pb_migrations/1769984709_collections_snapshot.js | head -50
```

### 2.3 Compare with Existing Schema
```bash
# Check for differences
ls -lh pb_migrations/*.js | tail -2
```

**Files:**
- `1738368000_idempotent_schema_complete.js` (6.4K) - Original schema
- `1769984709_collections_snapshot.js` (68K) - Full current snapshot

---

## Step 3: Test Migrations Locally

### 3.1 Create a Test Database Copy
```bash
cd pocketbase
cp -r pb_data pb_data_backup_$(date +%Y%m%d_%H%M%S)
```

### 3.2 Test Migration Down (if needed)
```bash
# Check if migrations support down
./pocketbase migrate down --dir=./pb_data --migrationsDir=./pb_migrations
```

### 3.3 Test Migration Up
```bash
./pocketbase migrate up --dir=./pb_data --migrationsDir=./pb_migrations
```

**Expected Output:**
```
No pending migrations to apply.
```

### 3.4 Verify Data Integrity
```bash
# Count records in key collections
sqlite3 pb_data/data.db "SELECT COUNT(*) as user_count FROM t_user;"
sqlite3 pb_data/data.db "SELECT COUNT(*) as message_count FROM m_messages;"
sqlite3 pb_data/data.db "SELECT COUNT(*) as conversation_count FROM m_conversations;"
```

---

## Step 4: Prepare for Production

### 4.1 Export JSON Schema for PocketHost
```bash
cd pocketbase

# Method 1: Direct SQLite export
sqlite3 pb_data/data.db "SELECT json_group_array(json_object(
  'id', id,
  'name', name,
  'type', type,
  'system', system,
  'fields', json(fields),
  'indexes', json(indexes),
  'listRule', listRule,
  'viewRule', viewRule,
  'createRule', createRule,
  'updateRule', updateRule,
  'deleteRule', deleteRule
)) FROM _collections;" > schemas_archive/pb_schema_export_$(date +%Y%m%d_%H%M%S).json
```

### 4.2 Create Migration Package
```bash
# Create deployment directory
mkdir -p deployment_$(date +%Y%m%d)
cp -r pb_migrations deployment_$(date +%Y%m%d)/
cp pb_data/data.db deployment_$(date +%Y%m%d)/schema_backup.db
cp schemas_archive/pb_schema_export*.json deployment_$(date +%Y%m%d)/

# Create checksum
cd deployment_$(date +%Y%m%d)
find . -type f -exec sha256sum {} \; > checksums.txt
```

### 4.3 Document Environment Variables
Create `deployment_YYYYMMDD/env_template.txt`:
```bash
# PocketBase Production Environment Variables

# Server
PB_ADMIN_EMAIL=admin@yourdomain.com
PB_ADMIN_PASSWORD=<secure_password>

# Database
PB_DATA_DIR=/pb_data

# Migrations
PB_MIGRATIONS_DIR=/pb_migrations

# Security
PB_ENCRYPTION_KEY=<auto_generated_or_custom>
PB_ADMIN_TOKEN=<secure_token>

# URLs
PB_PUBLIC_URL=https://your-instance.pockethost.io
```

---

## Step 5: Deploy to PocketHost

### 5.1 Access PocketHost Dashboard
1. Go to: https://pockethost.io
2. Login to your account
3. Navigate to your instance

### 5.2 Upload Migration Files

#### Option A: Web UI Upload
1. In PocketHost dashboard, go to "Collections"
2. Click "Import Collections"
3. Upload `1769984709_collections_snapshot.js` OR
4. Upload JSON from `schemas_archive/pb_schema_export_YYYYMMDD_HHMMSS.json`

#### Option B: Manual Collection Creation
Use the snapshot file as reference to manually create each collection in the web UI.

### 5.3 Verify Migration on PocketHost

1. **Check Collections**:
   - Navigate to "Collections" tab
   - Verify all 20 collections exist
   - Check system collections are present

2. **Verify Indexes**:
   - Each collection should have its indexes created
   - Check the "Indexes" tab for each collection

3. **Test API Access**:
```bash
# Replace with your PocketHost URL
POCKETHOST_URL="https://your-instance.pockethost.io"

# Test health endpoint
curl "$POCKETHOST_URL/api/health"

# Test collections endpoint (requires auth)
curl "$POCKETHOST_URL/api/collections"
```

---

## Step 6: Verify Production

### 6.1 Run Production Tests
```bash
# Test user authentication
curl -X POST "$POCKETHOST_URL/api/collections/t_user/auth-with-password" \
  -H "Content-Type: application/json" \
  -d '{
    "identity": "test@example.com",
    "password": "test123456"
  }'

# Test messaging collection
curl "$POCKETHOST_URL/api/collections/m_conversations/records" \
  -H "Authorization: Bearer YOUR_AUTH_TOKEN"

# Test profiles collection
curl "$POCKETHOST_URL/api/collections/s_profiles/records" \
  -H "Authorization: Bearer YOUR_AUTH_TOKEN"
```

### 6.2 Performance Checks
```bash
# Check response times
time curl "$POCKETHOST_URL/api/health"

# Check database size (in PocketHost dashboard)
# Navigate to: Settings → Storage
```

### 6.3 Verify Real-time Subscriptions
```javascript
// Test from browser console or Node.js
import PocketBase from 'pocketbase';

const pb = new PocketBase('https://your-instance.pockethost.io');

// Subscribe to messages
pb.collection('m_messages').subscribe('*', (e) => {
  console.log('Message event:', e);
});

// Subscribe to typing status
pb.collection('m_typing_status').subscribe('*', (e) => {
  console.log('Typing event:', e);
});
```

---

## Rollback Procedures

### If Production Migration Fails

#### Option 1: Quick Rollback (PocketHost)
1. Go to PocketHost dashboard
2. Navigate to "Backups"
3. Restore from latest pre-migration backup
4. Verify data integrity

#### Option 2: Schema Reset
1. Export all data from failing collections:
```bash
# For each collection with data
curl "$POCKETHOST_URL/api/collections/COLLECTION_NAME/records" \
  -H "Authorization: Bearer ADMIN_TOKEN" > backup_COLLECTION_NAME.json
```

2. Delete problematic collections in PocketHost UI
3. Re-create from backup schema
4. Import data back

#### Option 3: Local Recovery
1. Use local backup database:
```bash
cd pocketbase
cp pb_data_backup_YYYYMMDD_HHMMSS/data.db pb_data/data.db
```

2. Generate clean snapshot:
```bash
./pocketbase migrate collections
```

3. Re-upload to PocketHost

---

## Schema Documentation

### Critical Collections & Relationships

#### 1. User & Auth System
```
t_user (auth collection)
├── fields: email, password, name, username, avatar
├── relations: → s_profiles (1:1)
├── relations: → t_user_property (1:many)
└── relations: → m_conversation_participants (1:many)
```

#### 2. Messaging System
```
m_conversations
├── fields: type, name, avatar, last_message_at
├── relations: ← m_messages (1:many)
├── relations: ← m_conversation_participants (1:many)
└── indexes: idx_conversation_lastMessage

m_messages
├── fields: content, type, sent_at, sender_id
├── relations: → conversation_id (many:1)
├── relations: → sender_id (many:1)
├── relations: ← m_read_receipts (1:many)
├── relations: ← m_reactions (1:many)
└── indexes: idx_conversation_sent

m_read_receipts
├── fields: message_id, user_id, read_at
├── relations: → message_id (many:1)
├── relations: → user_id (many:1)
└── indexes: idx_msg_read (UNIQUE)
```

#### 3. Profile & Matching System
```
s_profiles
├── fields: user_id, first_name, last_name, birth_date, bio
├── relations: → user_id (1:1)
└── indexes: idx_unique_userId (UNIQUE)

m_matches
├── fields: user_id, matched_user_id, match_score, status
├── relations: → user_id (many:1)
├── relations: → matched_user_id (many:1)
└── indexes: idx_match_pair (UNIQUE)
```

#### 4. Questionnaire System
```
t_proust_questionnaire
├── fields: version, description
└── relations: ← t_proust_question (1:many)

t_proust_question
├── fields: question, response_type
├── relations: → questionnaire_id (many:1)
└── relations: ← t_user_questionnaire_responses (1:many)

t_user_questionnaire_responses
├── fields: user_id, question_id, response
├── relations: → user_id (many:1)
├── relations: → question_id (many:1)
└── indexes: idx_response_unique (UNIQUE)
```

### Index Performance Notes

**Critical Indexes for Production:**
1. `idx_conversation_sent` - Message retrieval by conversation
2. `idx_msg_read` - Read receipt uniqueness
3. `idx_match_pair` - Prevent duplicate matches
4. `idx_unique_userId` - One profile per user
5. `idx_conversation_lastMessage` - Conversation sorting

---

## PocketHost-Specific Considerations

### Resource Limits
- **Free Tier**: 100 MB storage, 10k API calls/month
- **Paid Tiers**: Scaling based on plan
- **File Storage**: Separate from database quota
- **Real-time Connections**: Monitor WebSocket limits

### Best Practices
1. **Backups**: Enable automatic daily backups
2. **Monitoring**: Use PocketHost analytics dashboard
3. **Rate Limiting**: Implement client-side throttling
4. **Indexes**: Essential for performance at scale
5. **File Storage**: Use CDN for media files if possible

### Environment Configuration
```javascript
// Production SDK initialization
const pb = new PocketBase('https://your-instance.pockethost.io');

// Enable auto-cancellation
pb.autoCancellation(false);

// Set base fetch options
pb.beforeSend = function (url, options) {
  options.headers = {
    ...options.headers,
    'X-App-Version': '1.0.0'
  };
  return { url, options };
};
```

---

## Next Steps After Deployment

### 1. Immediate Post-Deployment
- [ ] Run full test suite against production
- [ ] Monitor error logs for 24 hours
- [ ] Verify real-time features working
- [ ] Check backup creation successful

### 2. Week 1 Monitoring
- [ ] Review API performance metrics
- [ ] Check database growth rate
- [ ] Monitor WebSocket connection stability
- [ ] Gather user feedback

### 3. Optimization
- [ ] Add caching layer if needed
- [ ] Optimize slow queries
- [ ] Review and tune indexes
- [ ] Plan for data archival strategy

---

## Support & Resources

### PocketBase Documentation
- Official Docs: https://pocketbase.io/docs/
- API Reference: https://pocketbase.io/docs/api-records/
- Migrations: https://pocketbase.io/docs/migrations/

### PocketHost Resources
- Dashboard: https://pockethost.io
- Documentation: https://pockethost.io/docs/
- Community Discord: https://discord.gg/pockethost

### Local Development
```bash
# Start local PocketBase for testing
cd pocketbase
./pocketbase serve --http=127.0.0.1:8090

# Access admin UI
open http://127.0.0.1:8090/_/
```

---

## Quick Command Reference

```bash
# Export current schema
cd pocketbase && ./pocketbase migrate collections

# Check migration status
./pocketbase migrate collections --dir=./pb_data --migrationsDir=./pb_migrations

# Create backup
cp -r pb_data pb_data_backup_$(date +%Y%m%d_%H%M%S)

# List all collections
sqlite3 pb_data/data.db "SELECT name FROM _collections ORDER BY name;"

# Count records in a collection
sqlite3 pb_data/data.db "SELECT COUNT(*) FROM COLLECTION_NAME;"

# View collection schema
sqlite3 pb_data/data.db ".schema COLLECTION_NAME"

# Export data as JSON
sqlite3 pb_data/data.db "SELECT json_group_array(json_object(...)) FROM _collections;"
```

---

**Document Version**: 1.0  
**Last Updated**: February 1, 2026  
**Status**: ✅ Ready for Production Deployment
