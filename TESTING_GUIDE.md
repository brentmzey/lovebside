# Testing Guide - PocketBase Integration

## 🎯 Overview

This guide will help you test and verify your PocketBase integration is working correctly with proper error handling and logging.

## 📋 Pre-requisites

1. **PocketBase Instance**: https://bside.pockethost.io/
2. **Collections Created**: Follow POCKETBASE_SCHEMA.md
3. **Test Data**: At least some key values seeded
4. **Test User**: Optional, for authentication tests

## 🧪 Testing Methods

### Method 1: Desktop App (Recommended for Initial Testing)

The easiest way to test is using the JVM/Desktop target:

```bash
# Run the desktop app which includes test suite
./gradlew :composeApp:run
```

The tests will run automatically on startup and log results to console.

### Method 2: Manual Test Function

Add this to your app initialization:

```kotlin
// In your App.kt or main initialization
val httpClient = ApiClient.create()
val pocketBaseClient = PocketBaseClient(httpClient)

// Run tests
PocketBaseManualTests.runAllTests(pocketBaseClient)
```

### Method 3: Quick Smoke Test

For a quick check:

```kotlin
val passed = PocketBaseManualTests.quickSmokeTest(pocketBaseClient)
if (passed) {
    println("✅ PocketBase is ready!")
} else {
    println("❌ PocketBase has issues - check logs")
}
```

## 📊 Test Suite Components

### 1. Health Check
Tests basic connectivity and read operations.

**What it checks:**
- ✅ Connection to PocketBase
- ✅ Read operation (fetch key values)
- ✅ Authentication (if credentials provided)

**Expected output:**
```
╔════════════════════════════════════════╗
║     PocketBase Health Check Report     ║
╚════════════════════════════════════════╝

  ✅ connection: PASS
  ✅ read: PASS
  ✅ auth: PASS

✅ All health checks passed!
```

### 2. Connection Test
Verifies the PocketBase URL is accessible.

**Expected output:**
```
🧪 Testing PocketBase connection...
✅ PocketBase connection successful! Found 25 key values
```

### 3. CRUD Test
Tests basic create/read operations.

**Expected output:**
```
🧪 Testing CRUD operations...
✅ CRUD test passed! Fetched 5 items
Sample item: {id=abc123, key=adventurous, category=PERSONALITY}
```

### 4. Fetch Key Values
Tests listing and pagination.

**Expected output:**
```
📋 Test 4: Fetching key values...
✅ Fetched 5 key values
   Total items: 25, Total pages: 5
   1. adventurous (PERSONALITY)
   2. analytical (PERSONALITY)
   3. creative (PERSONALITY)
   ...
```

### 5. Filtered Query
Tests PocketBase filter syntax.

**Expected output:**
```
📋 Test 5: Testing filtered queries...
✅ Filtered query returned 3 items
   - adventurous (PERSONALITY)
   - analytical (PERSONALITY)
   - creative (PERSONALITY)
```

## 🔍 Interpreting Results

### Success Case
All tests show ✅ and green checkmarks:
```
✅ Connection test passed
✅ CRUD test passed
✅ Filtered query returned 3 items
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
✅ All manual tests completed! Check logs above for any failures.
```

### Failure Cases

#### Connection Failed
```
❌ Connection test failed: Network error
💡 Tip: Check your internet connection and PocketBase URL
```

**Solutions:**
1. Verify URL: `https://bside.pockethost.io/`
2. Check internet connection
3. Verify PocketHost is not down

#### 404 Not Found
```
❌ Failed to fetch key values: Resource not found
```

**Solutions:**
1. Check collection name is correct: `s_key_values`
2. Verify collections exist in PocketBase admin
3. Check POCKETBASE_SCHEMA.md for correct setup

#### 403 Forbidden
```
❌ Authentication failed: Unauthorized
```

**Solutions:**
1. Check API rules in PocketBase admin
2. Verify collection permissions allow read access
3. For auth operations, verify credentials are correct

#### 400 Bad Request
```
❌ Filtered query failed: Validation error
```

**Solutions:**
1. Check filter syntax: `(field='value')`
2. Verify field names match PocketBase schema
3. Check for typos in filter string

## 📝 Logging Levels

The tests use different log levels:

### DEBUG (🔍)
Internal operations, detailed flow
```
🔍 PocketBase.getList(s_key_values): page=1, perPage=5, filter=null
```

### INFO (ℹ️)
Normal operations, successful actions
```
ℹ️ PocketBase.getList(s_key_values): ✅ Success - fetched 5/25 items
```

### WARN (⚠️)
Potential issues, rate limiting
```
⚠️ PocketBase.authWithPassword(users): Rate limited - too many login attempts
```

### ERROR (❌)
Failures, exceptions
```
❌ PocketBase.getList(s_key_values): HTTP 404 - Collection not found
```

## 🧰 Debugging Tips

### Enable Verbose Logging

In your AppConfig, set:
```kotlin
val config = AppConfig(
    environment = Environment.DEVELOPMENT,
    enableLogging = true
)
```

### Check PocketBase Admin

1. Go to https://bside.pockethost.io/_/
2. Check "Logs" tab for server-side errors
3. Verify collections exist
4. Check API rules are set correctly

### Test Individual Operations

Instead of running full suite, test specific operations:

```kotlin
// Test just connection
PocketBaseTestHelper.testConnection(client)

// Test just auth
PocketBaseTestHelper.testAuth(client, "test@example.com", "password")

// Test specific collection
client.getList<Map<String, Any>>(
    collection = "s_key_values",
    perPage = 1
).onSuccess { result ->
    println("Success: ${result.items}")
}.onError { error ->
    println("Error: ${error.getUserMessage()}")
}
```

### Common Issues & Solutions

| Issue | Symptom | Solution |
|-------|---------|----------|
| Wrong URL | Connection timeout | Verify `https://bside.pockethost.io/api/` |
| Missing collections | 404 errors | Create collections per POCKETBASE_SCHEMA.md |
| Wrong API rules | 403 errors | Update permissions in PocketBase admin |
| Invalid filter | 400 errors | Check filter syntax `(field='value')` |
| Rate limiting | 429 errors | Wait and retry, or increase rate limits |
| Serialization | Parse errors | Verify data models match PocketBase schema |

## ✅ Verification Checklist

Before moving to production:

- [ ] Health check passes
- [ ] Can connect to PocketBase
- [ ] Can read from all collections
- [ ] Can authenticate users
- [ ] Filters work correctly
- [ ] Pagination works
- [ ] Error messages are clear
- [ ] Logging is comprehensive
- [ ] Retry logic works on failures
- [ ] Performance is acceptable (<500ms for reads)

## 🚀 Next Steps

Once all tests pass:

1. ✅ Verify error handling is graceful
2. ✅ Check logging is informative
3. ✅ Test with real user data
4. ✅ Test authentication flow
5. ✅ Test CRUD operations on all collections
6. ✅ Performance test with larger datasets
7. ✅ Integration test full user flows

## 📞 Getting Help

If tests fail:

1. Check console logs for specific error messages
2. Review POCKETBASE_SCHEMA.md for correct setup
3. Verify PocketBase admin settings
4. Check COMPILATION_FIX_PROGRESS.md for known issues
5. Review error handling in PocketBaseClient.kt

## 🎓 Understanding the Test Output

### Successful Test Run
```
🚀 Starting PocketBase manual test suite...
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

📋 Test 1: Running health check...
╔════════════════════════════════════════╗
║     PocketBase Health Check Report     ║
╚════════════════════════════════════════╝

  ✅ connection: PASS
  ✅ read: PASS

✅ All health checks passed!

📋 Test 2: Testing connection...
✅ Connection test passed

📋 Test 3: Testing CRUD operations...
✅ CRUD test passed

📋 Test 4: Fetching key values...
✅ Fetched 5 key values
   Total items: 25, Total pages: 5
   1. adventurous (PERSONALITY)
   2. analytical (PERSONALITY)
   3. creative (PERSONALITY)
   4. empathetic (PERSONALITY)
   5. organized (PERSONALITY)

📋 Test 5: Testing filtered queries...
✅ Filtered query returned 3 items
   - adventurous (PERSONALITY)
   - analytical (PERSONALITY)
   - creative (PERSONALITY)

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
✅ All manual tests completed! Check logs above for any failures.
```

This output tells you:
1. ✅ Connection works
2. ✅ Can read data
3. ✅ Collections are set up
4. ✅ Filters work
5. ✅ Data is properly structured

## 🔬 Advanced Testing

### Test with Authentication
```kotlin
// First create a test user in PocketBase admin
// Then run:
PocketBaseManualTests.testAuthentication(
    client,
    email = "test@example.com",
    password = "TestPassword123!"
)
```

### Test Profile Operations
```kotlin
// After authentication:
PocketBaseManualTests.testProfileOperations(
    client,
    userId = "USER_ID_FROM_AUTH"
)
```

### Load Testing
```kotlin
// Test with many requests
repeat(100) {
    client.getList<Map<String, Any>>(
        collection = "s_key_values",
        page = it % 10,
        perPage = 10
    )
}
```

---

**Last Updated**: January 2025  
**Version**: 1.0.0  
**Status**: Ready for Testing
