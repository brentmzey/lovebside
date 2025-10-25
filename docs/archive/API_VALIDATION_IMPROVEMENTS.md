# API Validation & Schema Improvements Plan
**Date**: January 19, 2025  
**Status**: In Progress  
**Goal**: Bolster the app with comprehensive validation, testing, and schema improvements

---

## 🎯 Overview

This document tracks the systematic improvements being made to ensure data integrity, API reliability, and production readiness without requiring PocketBase to be running during development.

---

## ✅ Completed Improvements

### 1. **Enhanced Request Validation** ✓
- **Location**: `shared/src/commonMain/kotlin/love/bside/app/core/validation/RequestValidators.kt`
- **Features**:
  - Profile data validation (firstName, lastName, bio, location)
  - Birth date validation (age requirements, format)
  - Value importance validation (1-10 range)
  - Answer length validation
  - Match request validation
  
### 2. **Schema Sync Verification** ✓
- **Location**: `server/src/main/kotlin/love/bside/server/schema/SchemaSyncChecker.kt`
- **Features**:
  - Compares API models with DB models
  - Identifies field mismatches
  - Checks type compatibility
  - Reports missing fields
  - Can run without PocketBase

### 3. **API Contract Tests** ✓
- **Location**: `shared/src/commonTest/kotlin/love/bside/app/api/ApiContractTests.kt`
- **Features**:
  - Validates all API request/response models
  - Tests serialization/deserialization
  - Checks required fields
  - Validates constraints
  - No external dependencies

### 4. **Enhanced Error Handling** ✓
- **Location**: `server/src/main/kotlin/love/bside/server/plugins/EnhancedStatusPages.kt`
- **Features**:
  - Structured error responses
  - Request ID tracking
  - Error categorization
  - Client-friendly messages
  - Detailed logging

---

## 🚀 In Progress

### 5. **Server-Side Validation Middleware** 🔄
- **Location**: `server/src/main/kotlin/love/bside/server/plugins/ValidationPlugin.kt`
- **Status**: 60% complete
- **Remaining**:
  - Add validation to all routes
  - Add rate limiting per endpoint
  - Add request size limits

### 6. **Integration Test Suite** 🔄
- **Location**: `server/src/test/kotlin/love/bside/server/integration/`
- **Status**: 40% complete
- **Remaining**:
  - Mock PocketBase responses
  - Test all API endpoints
  - Test error scenarios
  - Test validation failures

---

## 📋 Next Steps (Prioritized)

### Phase 1: Validation Hardening (2-3 hours)

#### A. Add Comprehensive Input Validation
- [ ] Email validation with disposable email blocking
- [ ] Password strength requirements (complexity score)
- [ ] Profile text sanitization (XSS prevention)
- [ ] File upload validation (size, type, dimensions)
- [ ] Location geocoding validation
- [ ] Birth date edge cases (leap years, timezones)

#### B. Add Business Rule Validation
- [ ] User cannot match with themselves
- [ ] User cannot rate value they haven't selected
- [ ] User cannot answer prompt twice
- [ ] Age compatibility rules
- [ ] Geographic distance limits (if applicable)

#### C. Add Database Constraint Validation
- [ ] Unique constraint checks before insert
- [ ] Foreign key existence checks
- [ ] Enum value validation
- [ ] Required field validation
- [ ] Field length validation

### Phase 2: Schema Improvements (2-3 hours)

#### A. Add Missing Indexes
- [ ] `s_user_values`: Composite index (userId, keyValueId)
- [ ] `s_user_answers`: Composite index (userId, promptId)
- [ ] `s_matches`: Index on compatibility score
- [ ] `s_profiles`: Index on location for geo queries
- [ ] `s_matches`: Index on created date for recent matches

#### B. Add Schema Versioning
- [ ] Track schema version in database
- [ ] Automatic migration detection
- [ ] Rollback capability
- [ ] Schema changelog generation

#### C. Add Data Integrity Checks
- [ ] Orphaned record detection
- [ ] Referential integrity checks
- [ ] Data consistency validation
- [ ] Duplicate detection

### Phase 3: API Enhancements (3-4 hours)

#### A. Add Response Pagination
```kotlin
data class PaginatedResponse<T>(
    val data: List<T>,
    val page: Int,
    val perPage: Int,
    val totalCount: Int,
    val totalPages: Int
)
```
- [ ] Implement in all list endpoints
- [ ] Add cursor-based pagination option
- [ ] Add sorting parameters
- [ ] Add filtering parameters

#### B. Add Response Caching
- [ ] Cache key values (rarely change)
- [ ] Cache prompts (rarely change)
- [ ] Cache user profiles (with TTL)
- [ ] Add cache invalidation
- [ ] Add ETag support

#### C. Add Request/Response Compression
- [ ] Enable gzip compression
- [ ] Configure compression levels
- [ ] Add conditional compression
- [ ] Test bandwidth savings

### Phase 4: Testing Infrastructure (3-4 hours)

#### A. Mock PocketBase Client
- [ ] Create MockPocketBaseClient
- [ ] Implement all repository methods
- [ ] Add configurable responses
- [ ] Add error injection
- [ ] Use in tests

#### B. API Integration Tests
- [ ] Test auth flow end-to-end
- [ ] Test profile CRUD operations
- [ ] Test match discovery
- [ ] Test questionnaire flow
- [ ] Test error handling

#### C. Performance Tests
- [ ] Benchmark API response times
- [ ] Test concurrent requests
- [ ] Test large payload handling
- [ ] Identify bottlenecks
- [ ] Optimize slow operations

### Phase 5: Security Hardening (2-3 hours)

#### A. Add Rate Limiting
```kotlin
install(RateLimiting) {
    register {
        rateLimiter(limit = 100, refillPeriod = 60.seconds)
        requestWeight { call, key -> 1 }
    }
    register("auth") {
        rateLimiter(limit = 10, refillPeriod = 60.seconds)
        requestWeight { call, key -> 5 }
    }
}
```

#### B. Add Request Sanitization
- [ ] HTML entity encoding
- [ ] SQL injection prevention
- [ ] XSS prevention
- [ ] CSRF protection
- [ ] Path traversal prevention

#### C. Add Audit Logging
- [ ] Log all authentication attempts
- [ ] Log all data modifications
- [ ] Log all admin actions
- [ ] Include user ID, IP, timestamp
- [ ] Store in separate audit table

---

## 🔧 Implementation Checklist

### Request Validators (High Priority)
- [x] Email validator
- [x] Password validator
- [x] Name validators (firstName, lastName)
- [x] Bio validator
- [ ] Location validator (geocoding)
- [x] Birth date validator
- [x] Value importance validator
- [x] Answer text validator
- [ ] File upload validator

### Schema Validation (High Priority)
- [x] Field type matching
- [x] Required field checks
- [x] Unique constraint checks
- [ ] Foreign key validation
- [ ] Enum value validation
- [ ] Index optimization checks
- [ ] Migration generation

### API Enhancements (Medium Priority)
- [ ] Pagination on all list endpoints
- [ ] Sorting parameters
- [ ] Filtering parameters
- [ ] Response compression
- [ ] Request caching
- [ ] ETag support
- [ ] CORS optimization

### Error Handling (High Priority)
- [x] Structured error responses
- [x] Request ID tracking
- [x] Error categorization
- [ ] Error rate monitoring
- [ ] Error alerting
- [ ] Client error translation

### Testing (High Priority)
- [x] API contract tests
- [ ] Mock PocketBase client
- [ ] Integration test suite
- [ ] Performance benchmarks
- [ ] Security tests
- [ ] End-to-end tests

### Security (Critical Priority)
- [ ] Rate limiting (per endpoint)
- [ ] Request size limits
- [ ] Brute force protection
- [ ] Input sanitization
- [ ] Audit logging
- [ ] Security headers

---

## 📊 Validation Rules Reference

### Profile Validation
```kotlin
// First Name & Last Name
- Required
- Min length: 1 character
- Max length: 50 characters
- No special characters except hyphen, apostrophe, space
- Trim whitespace

// Bio
- Optional
- Max length: 500 characters
- Allow markdown formatting
- No HTML tags
- Trim whitespace

// Location
- Optional
- Max length: 100 characters
- Validate against geocoding service (optional)
- Trim whitespace

// Birth Date
- Required
- Format: YYYY-MM-DD
- Minimum age: 18 years
- Maximum age: 100 years
- No future dates

// Seeking
- Required
- Enum: FRIENDSHIP, RELATIONSHIP, BOTH
```

### Value Validation
```kotlin
// Value Importance
- Required
- Type: Integer
- Range: 1-10
- Used for matching algorithm weight

// Key Value ID
- Required
- Must exist in s_key_values table
- Foreign key constraint
```

### Answer Validation
```kotlin
// Answer Text
- Required
- Min length: 10 characters
- Max length: 500 characters
- Trim whitespace
- No empty paragraphs

// Prompt ID
- Required
- Must exist in s_prompts table
- Foreign key constraint
```

### Match Validation
```kotlin
// User cannot match with themselves
userId != matchedUserId

// Compatibility Score
- Type: Double
- Range: 0.0 - 100.0
- Calculated by algorithm
- Stored with 2 decimal places

// Shared Values
- Type: JSON array
- Contains value IDs both users share
- Used for match explanation
```

---

## 🧪 Testing Strategy

### Unit Tests (Server)
```bash
./gradlew :server:test
```
- Service layer tests with mocks
- Repository tests with mock client
- Validation tests
- Util tests

### Integration Tests (API)
```bash
./gradlew :shared:jvmTest
```
- API client tests
- Repository integration tests
- End-to-end flow tests

### Contract Tests
```bash
./gradlew :shared:commonTest
```
- Request/response model tests
- Serialization tests
- Validation rule tests

### Performance Tests
```bash
k6 run performance/api-load-test.js
```
- Concurrent user simulation
- Response time measurement
- Resource usage monitoring

---

## 📈 Success Metrics

### Code Quality
- [ ] 80%+ test coverage
- [ ] 0 critical security issues
- [ ] 0 high-priority bugs
- [ ] All validations have tests

### Performance
- [ ] API response < 100ms (p95)
- [ ] API response < 50ms (p50)
- [ ] Handle 1000+ concurrent users
- [ ] Database queries < 50ms

### Reliability
- [ ] 99.9% uptime
- [ ] < 0.1% error rate
- [ ] Graceful degradation
- [ ] Automatic recovery

---

## 🔄 Continuous Improvements

### Weekly Tasks
- Review error logs
- Analyze slow queries
- Check security alerts
- Update dependencies

### Monthly Tasks
- Performance audit
- Security audit
- Schema optimization
- Test coverage review

### Quarterly Tasks
- Architecture review
- Scalability planning
- Technology updates
- User feedback integration

---

## 📚 Resources

### Documentation
- `POCKETBASE_SCHEMA.md` - Database schema
- `ENTERPRISE_STATUS_OCT19_2025.md` - Architecture overview
- `TESTING_GUIDE.md` - Testing procedures
- `DEVELOPER_GUIDE.md` - Development workflow

### Tools
- Schema validator: `./gradlew :server:runSchemaValidator`
- API tests: `./gradlew :shared:jvmTest`
- Build verification: `./verify-build.sh`
- Schema sync: `./gradlew :server:checkSchemaSync`

---

**Next Action**: Implement Phase 1 validation hardening (starting with comprehensive input validation)

**Last Updated**: January 19, 2025
