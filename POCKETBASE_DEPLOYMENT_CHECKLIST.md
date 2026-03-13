# PocketBase Production Deployment Checklist

## Pre-Deployment ✅

### Local Verification (COMPLETED)
- [x] All migrations applied locally
- [x] Database integrity verified
- [x] Collections snapshot generated (`1769984709_collections_snapshot.js`)
- [x] JSON schema exported (`pb_schema_production_export_20260201_162908.json`)
- [x] Backup created
- [x] Documentation completed

### Schema Status
- **Total Collections**: 20
- **System Collections**: 5 (_mfas, _otps, _externalAuths, _authOrigins, _superusers)
- **Auth Collections**: 1 (t_user)
- **Application Collections**: 14 (messaging, profiles, questionnaires, etc.)

### Files Ready for Deployment
```
✅ pocketbase/pb_migrations/1769984709_collections_snapshot.js (68K)
✅ pocketbase/schemas_archive/pb_schema_production_export_20260201_162908.json (74K)
✅ pocketbase/pb_migrations/1738368000_idempotent_schema_complete.js (6.4K)
```

---

## Deployment Steps

### Phase 1: Preparation
- [ ] Review `POCKETBASE_PRODUCTION_DEPLOYMENT_GUIDE.md`
- [ ] Verify PocketHost account access
- [ ] Note current PocketHost instance URL
- [ ] Download local database backup
- [ ] Prepare admin credentials

### Phase 2: PocketHost Upload
- [ ] Login to PocketHost dashboard
- [ ] Navigate to your instance
- [ ] Go to "Collections" section
- [ ] Choose deployment method:
  - **Method A**: Import from JSON schema file
  - **Method B**: Import from JS migration file
  - **Method C**: Manual collection creation using snapshot as reference

### Phase 3: Collection Import

#### If Using JSON Import (Recommended)
- [ ] Click "Import Collections"
- [ ] Upload `pb_schema_production_export_20260201_162908.json`
- [ ] Review preview of collections to be created
- [ ] Confirm import
- [ ] Wait for completion

#### If Using Migration File
- [ ] Access PocketHost file system (if available)
- [ ] Upload migration file to `pb_migrations/` directory
- [ ] Run migration command via PocketHost console
- [ ] Verify migration success

### Phase 4: Verification

#### Collections Check
- [ ] Verify all 20 collections created
- [ ] Check system collections exist
- [ ] Verify t_user (auth collection) configured correctly
- [ ] Confirm all indexes created

#### Test Each Major Collection
- [ ] `t_user` - Create test user via API
- [ ] `m_conversations` - List conversations endpoint works
- [ ] `m_messages` - Test message creation
- [ ] `s_profiles` - Test profile CRUD
- [ ] `m_read_receipts` - Verify read status tracking
- [ ] `m_reactions` - Test reaction functionality
- [ ] `m_typing_status` - Check real-time typing
- [ ] `m_matches` - Test matching system
- [ ] `t_proust_questionnaire` - Verify questionnaire loading

### Phase 5: Security & Access Rules

#### Review Access Rules Per Collection
- [ ] t_user: `listRule: "id = @request.auth.id"`
- [ ] m_messages: `createRule: "sender_id = @request.auth.id"`
- [ ] m_read_receipts: `createRule: "user_id = @request.auth.id"`
- [ ] s_profiles: `updateRule: "@request.auth.id != \"\""`
- [ ] m_conversations: `viewRule: "@request.auth.id != \"\""`

#### Admin Configuration
- [ ] Set admin email
- [ ] Set strong admin password
- [ ] Enable 2FA for admin account (if available)
- [ ] Configure email settings
- [ ] Test password reset flow

### Phase 6: Real-time Features
- [ ] Enable real-time API (WebSocket)
- [ ] Test message subscriptions
- [ ] Test typing indicators
- [ ] Test presence updates
- [ ] Verify connection limits
- [ ] Monitor WebSocket stability

### Phase 7: API Testing

#### Authentication Tests
```bash
# Set your PocketHost URL
export POCKETHOST_URL="https://your-instance.pockethost.io"

# Test 1: Health check
curl $POCKETHOST_URL/api/health

# Test 2: Create test user
curl -X POST $POCKETHOST_URL/api/collections/t_user/records \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "Test123456!",
    "passwordConfirm": "Test123456!"
  }'

# Test 3: Authenticate
curl -X POST $POCKETHOST_URL/api/collections/t_user/auth-with-password \
  -H "Content-Type: application/json" \
  -d '{
    "identity": "test@example.com",
    "password": "Test123456!"
  }'

# Save the token from response
export AUTH_TOKEN="<token_from_response>"
```

#### Collection Access Tests
```bash
# Test 4: List conversations
curl $POCKETHOST_URL/api/collections/m_conversations/records \
  -H "Authorization: Bearer $AUTH_TOKEN"

# Test 5: List profiles
curl $POCKETHOST_URL/api/collections/s_profiles/records \
  -H "Authorization: Bearer $AUTH_TOKEN"

# Test 6: Create conversation
curl -X POST $POCKETHOST_URL/api/collections/m_conversations/records \
  -H "Authorization: Bearer $AUTH_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "type": "direct",
    "name": "Test Conversation"
  }'

# Test 7: Send message
curl -X POST $POCKETHOST_URL/api/collections/m_messages/records \
  -H "Authorization: Bearer $AUTH_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "conversation_id": "<conversation_id>",
    "sender_id": "<user_id>",
    "content": "Test message",
    "type": "text",
    "sent_at": "<current_iso_timestamp>"
  }'
```

- [ ] All API tests pass
- [ ] Response times acceptable (<500ms)
- [ ] No 500 errors
- [ ] Access rules enforced correctly

### Phase 8: Performance & Monitoring

#### Database Performance
- [ ] Check query execution times in PocketHost logs
- [ ] Verify indexes are being used
- [ ] Monitor database size
- [ ] Check for slow queries

#### Resource Usage
- [ ] CPU usage normal
- [ ] Memory usage acceptable
- [ ] Storage within limits
- [ ] API call quota tracking enabled

#### Monitoring Setup
- [ ] Enable error logging
- [ ] Set up performance alerts
- [ ] Configure backup schedule (daily recommended)
- [ ] Enable API request logging
- [ ] Set up uptime monitoring

### Phase 9: Data Migration (If Applicable)

#### If Migrating Existing Data
- [ ] Export data from old system
- [ ] Transform data to match new schema
- [ ] Import users first (t_user)
- [ ] Import profiles (s_profiles)
- [ ] Import conversations (m_conversations)
- [ ] Import messages (m_messages)
- [ ] Import read receipts (m_read_receipts)
- [ ] Import reactions (m_reactions)
- [ ] Import matches (m_matches)
- [ ] Verify data integrity
- [ ] Check foreign key relationships
- [ ] Test data access via API

### Phase 10: Client Integration

#### Update Client Applications
- [ ] Update PocketBase SDK URL
- [ ] Update authentication flow
- [ ] Test real-time subscriptions
- [ ] Verify file upload/download
- [ ] Test offline sync (if applicable)
- [ ] Update environment variables

#### SDK Configuration
```javascript
// composeApp/src/commonMain/kotlin/Config.kt or similar
const val POCKETBASE_URL = "https://your-instance.pockethost.io"

// or in JavaScript/TypeScript
const pb = new PocketBase('https://your-instance.pockethost.io');
```

- [ ] Production URL configured
- [ ] Authentication working
- [ ] API calls successful
- [ ] Real-time features working
- [ ] File uploads working
- [ ] Error handling tested

---

## Post-Deployment (First 24 Hours)

### Immediate Checks (First Hour)
- [ ] All services responding
- [ ] No 500 errors in logs
- [ ] Real-time connections stable
- [ ] Database writes successful
- [ ] File uploads working
- [ ] Authentication flow working

### Monitoring (First 24 Hours)
- [ ] Check error logs every 2 hours
- [ ] Monitor API response times
- [ ] Track database growth rate
- [ ] Monitor WebSocket connections
- [ ] Check backup completion
- [ ] Review user feedback

### Performance Metrics
- [ ] API response time: <500ms average
- [ ] Database query time: <100ms average
- [ ] WebSocket latency: <100ms
- [ ] File upload speed: acceptable
- [ ] Error rate: <0.1%

---

## Week 1 Monitoring

### Daily Checks
- [ ] Review error logs
- [ ] Check database size growth
- [ ] Monitor API usage
- [ ] Verify backups running
- [ ] Check for slow queries

### Weekly Review
- [ ] Analyze performance trends
- [ ] Review user feedback
- [ ] Check for optimization opportunities
- [ ] Plan capacity scaling if needed
- [ ] Update documentation

---

## Rollback Plan

### If Critical Issues Occur

#### Quick Rollback (< 1 hour of deployment)
- [ ] Access PocketHost dashboard
- [ ] Navigate to Backups
- [ ] Restore from pre-deployment backup
- [ ] Verify data integrity
- [ ] Test critical paths
- [ ] Notify stakeholders

#### Partial Rollback (Specific Collections)
- [ ] Identify problematic collection(s)
- [ ] Export current data
- [ ] Delete collection in PocketHost
- [ ] Re-create from backup schema
- [ ] Re-import data
- [ ] Test thoroughly

#### Full System Rollback
- [ ] Document all issues
- [ ] Export any new production data
- [ ] Restore full database backup
- [ ] Revert client applications
- [ ] Test entire system
- [ ] Schedule retry with fixes

---

## Success Criteria

### Deployment Considered Successful When:
- ✅ All 20 collections created and accessible
- ✅ All indexes created successfully
- ✅ Authentication flow working end-to-end
- ✅ Real-time features operational
- ✅ API response times <500ms
- ✅ No critical errors in logs
- ✅ Backups running successfully
- ✅ Client applications connected
- ✅ User feedback positive
- ✅ All acceptance tests passing

---

## Support Contacts

### Internal Team
- **DevOps Lead**: [Name/Contact]
- **Backend Lead**: [Name/Contact]
- **Frontend Lead**: [Name/Contact]

### External Support
- **PocketHost Support**: support@pockethost.io
- **PocketBase Discord**: https://discord.gg/pocketbase
- **Emergency Contact**: [24/7 Contact]

---

## Quick Reference

### Important URLs
- **PocketHost Dashboard**: https://pockethost.io/dashboard
- **Production Instance**: https://your-instance.pockethost.io
- **Admin UI**: https://your-instance.pockethost.io/_/
- **API Docs**: https://your-instance.pockethost.io/_/docs

### Important Commands
```bash
# Health check
curl https://your-instance.pockethost.io/api/health

# List collections
curl https://your-instance.pockethost.io/api/collections

# Authenticate
curl -X POST https://your-instance.pockethost.io/api/collections/t_user/auth-with-password \
  -H "Content-Type: application/json" \
  -d '{"identity":"user@example.com","password":"password"}'
```

### Schema Files Location
```
Local:
  - pocketbase/pb_migrations/1769984709_collections_snapshot.js
  - pocketbase/schemas_archive/pb_schema_production_export_20260201_162908.json
  
Documentation:
  - POCKETBASE_PRODUCTION_DEPLOYMENT_GUIDE.md
  - POCKETBASE_DEPLOYMENT_CHECKLIST.md (this file)
```

---

## Checklist Summary

**Total Items**: 100+
**Estimated Time**: 4-6 hours
**Risk Level**: Medium
**Rollback Time**: 15-30 minutes

**Current Status**: ✅ Pre-deployment complete, ready for Phase 1

---

**Last Updated**: February 1, 2026  
**Version**: 1.0  
**Reviewed By**: [Name]  
**Approved By**: [Name]
