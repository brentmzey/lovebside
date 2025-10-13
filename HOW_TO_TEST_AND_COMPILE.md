# How to Test and Compile - Complete Guide

## 🎯 Quick Start

### 1. Compile the Project
```bash
cd /Users/brentzey/bside

# Compile shared module (fastest check)
./gradlew :shared:compileKotlinJvm --no-daemon

# Build shared JAR
./gradlew :shared:jvmJar

# Run desktop app with tests
./gradlew :composeApp:run
```

### 2. Run PocketBase Tests
```bash
# Run with test flag
./gradlew :composeApp:run --args="--test"

# Or use the shorthand
./gradlew :composeApp:run --args="-t"
```

### 3. Check Logs
Look for output like this in console:
```
🚀 Starting PocketBase manual test suite...
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
...
✅ All manual tests completed!
```

## 📚 Step-by-Step Testing Process

### Step 1: Verify Compilation

First, make sure everything compiles:

```bash
# Check for errors
./gradlew :shared:compileKotlinJvm --no-daemon 2>&1 | grep "^e:"

# Count errors
./gradlew :shared:compileKotlinJvm --no-daemon 2>&1 | grep -c "^e:"

# If 0 errors, you're good to go!
```

**Expected Result**: 0 compilation errors

**If you see errors**:
1. Check the error messages
2. Look at COMPILATION_FIX_PROGRESS.md for guidance
3. Focus on the first error (others may be cascading)

### Step 2: Run Quick Smoke Test

```bash
# Start the desktop app (includes automatic smoke test in dev mode)
./gradlew :composeApp:run
```

**What to look for in console**:
```
🔥 Running quick smoke test...
✅ Smoke test passed!
```

**If smoke test fails**:
- Check internet connection
- Verify PocketBase URL is correct
- Check TESTING_GUIDE.md for troubleshooting

### Step 3: Run Full Test Suite

```bash
./gradlew :composeApp:run --args="--test"
```

**Expected Output**:
```
═══════════════════════════════════════════════════════════
         B-SIDE POCKETBASE TEST SUITE
═══════════════════════════════════════════════════════════

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

📋 Test 5: Testing filtered queries...
✅ Filtered query returned 3 items
```

### Step 4: Verify Error Handling

The tests should show proper error handling:

```kotlin
// Example error output you might see:
❌ Connection test failed: Network error
💡 Tip: Check your internet connection and PocketBase URL
```

### Step 5: Check Logging Quality

Look for these log levels:

**DEBUG (��)** - Detailed operation info:
```
🔍 PocketBase.getList(s_key_values): page=1, perPage=5, filter=null
```

**INFO (ℹ️)** - Successful operations:
```
ℹ️ PocketBase.getList(s_key_values): ✅ Success - fetched 5/25 items
```

**ERROR (❌)** - Failures with context:
```
❌ PocketBase.getList(s_key_values): HTTP 404 - Collection not found
```

## 🔍 Inspecting the Results

### Where to Find Logs

**Desktop/JVM**:
- Console output (stdout)
- Check terminal where you ran `./gradlew :composeApp:run`

**Android**:
- Logcat in Android Studio
- Filter by "B-Side" or "PocketBase"

**iOS**:
- Xcode Console
- Filter by app name

### What Good Logs Look Like

```
// Successful operation
ℹ️ PocketBase.getList(s_key_values): ✅ Success - fetched 5/25 items

// With retry
🔍 NetworkResilience: Retrying... Attempt 1/3
ℹ️ PocketBase.getList(s_key_values): ✅ Success - fetched 5/25 items

// With error and recovery
❌ PocketBase.getList(s_key_values): HTTP 500 - Server error
🔍 NetworkResilience: Retrying... Attempt 2/3
ℹ️ PocketBase.getList(s_key_values): ✅ Success - fetched 5/25 items
```

### What Bad Logs Look Like

```
// No context
Error: null
Failed

// With context (BETTER)
❌ PocketBase.getList(s_key_values): HTTP 404 - Collection not found
   at PocketBaseClient.getList(PocketBaseClient.kt:66)
💡 Tip: Verify collection 's_key_values' exists in PocketBase admin
```

## 🧪 Manual Testing Scenarios

### Scenario 1: Test Connection
```kotlin
val httpClient = ApiClient.create()
val pocketBaseClient = PocketBaseClient(httpClient)

PocketBaseTestHelper.testConnection(pocketBaseClient)
    .onSuccess { 
        println("✅ Connected to PocketBase") 
    }
    .onError { error -> 
        println("❌ Connection failed: ${error.getUserMessage()}") 
    }
```

### Scenario 2: Test Authentication
```kotlin
PocketBaseTestHelper.testAuth(
    pocketBaseClient, 
    "test@example.com", 
    "password123"
).onSuccess { token ->
    println("✅ Authenticated! Token: ${token.take(20)}...")
}.onError { error ->
    println("❌ Auth failed: ${error.getUserMessage()}")
}
```

### Scenario 3: Test Data Fetching
```kotlin
pocketBaseClient.getList<Map<String, Any>>(
    collection = "s_key_values",
    page = 1,
    perPage = 5
).onSuccess { result ->
    println("✅ Fetched ${result.items.size} items")
    result.items.forEach { item ->
        println("   - ${item["key"]} (${item["category"]})")
    }
}.onError { error ->
    println("❌ Fetch failed: ${error.getUserMessage()}")
}
```

## 📊 Verifying Error Handling

### Test Happy Path
```kotlin
// This should succeed
pocketBaseClient.getList<Map<String, Any>>(
    collection = "s_key_values",  // Correct collection
    perPage = 5
)
```

### Test Error Cases
```kotlin
// Test 404
pocketBaseClient.getList<Map<String, Any>>(
    collection = "non_existent",  // Wrong collection
    perPage = 5
).onError { error ->
    // Should be AppException.Business.ResourceNotFound
    println("Correctly caught error: ${error::class.simpleName}")
}

// Test invalid filter
pocketBaseClient.getList<Map<String, Any>>(
    collection = "s_key_values",
    filter = "invalid filter syntax"
).onError { error ->
    // Should be AppException.Validation or Network error
    println("Correctly caught error: ${error::class.simpleName}")
}
```

## 🎯 Verification Checklist

### Before Moving Forward

- [ ] ✅ Project compiles without errors
- [ ] ✅ Smoke test passes
- [ ] ✅ Full test suite passes
- [ ] ✅ Logs show proper DEBUG/INFO/ERROR levels
- [ ] ✅ Error messages are helpful and actionable
- [ ] ✅ Happy path works (successful requests)
- [ ] ✅ Error path works (graceful error handling)
- [ ] ✅ Retry logic works (network failures recover)
- [ ] ✅ Can connect to PocketBase
- [ ] ✅ Can read from all collections

### Quality Indicators

**Good:**
- Clear, descriptive log messages
- Errors include context and tips
- Operations complete within reasonable time
- Retry logic recovers from transient failures

**Needs Improvement:**
- Cryptic error messages
- Missing log context
- Slow operations (>2 seconds for simple reads)
- Crashes instead of graceful errors

## 🐛 Troubleshooting

### Issue: Compilation Errors

```bash
# See what's wrong
./gradlew :shared:compileKotlinJvm --no-daemon

# Focus on first error
./gradlew :shared:compileKotlinJvm --no-daemon 2>&1 | grep "^e:" | head -1
```

**Common fixes**:
- Missing imports
- Wrong Result type (use `love.bside.app.core.Result`)
- Missing sealed class branches in `when` expressions

### Issue: Tests Don't Run

```bash
# Make sure you're using the right command
./gradlew :composeApp:run --args="--test"

# Not this:
./gradlew :composeApp:run --test  # WRONG
```

### Issue: PocketBase Connection Fails

**Check**:
1. Internet connection
2. PocketBase URL: `https://bside.pockethost.io/`
3. Collections exist (check PocketBase admin)
4. API rules allow read access

### Issue: No Logs Appear

**Solutions**:
1. Check console output
2. Verify `Environment.DEVELOPMENT` is set
3. Check log level configuration
4. Ensure `enableLogging = true` in AppConfig

## 📈 Performance Verification

### Acceptable Benchmarks

| Operation | Expected Time | Max Acceptable |
|-----------|--------------|----------------|
| Connection test | <100ms | 500ms |
| Read 10 items | <200ms | 1s |
| Read 100 items | <500ms | 2s |
| Authentication | <300ms | 1s |
| Create record | <200ms | 1s |

### How to Measure

```kotlin
val startTime = Clock.System.now()
pocketBaseClient.getList<Map<String, Any>>("s_key_values", perPage = 10)
    .onSuccess {
        val duration = Clock.System.now() - startTime
        println("Operation took: ${duration.inWholeMilliseconds}ms")
    }
```

## 🚀 Next Steps

Once everything passes:

1. **Review the logs** - Make sure they're helpful
2. **Test error scenarios** - Verify graceful handling
3. **Check performance** - Operations should be fast
4. **Move to integration** - Test full user flows
5. **Continue development** - Following patterns in COMPILATION_FIX_PROGRESS.md

## 📞 Need Help?

If tests fail or you see issues:

1. Check **TESTING_GUIDE.md** for detailed troubleshooting
2. Review **COMPILATION_FIX_PROGRESS.md** for known issues
3. Inspect **POCKETBASE_SCHEMA.md** for correct setup
4. Look at actual error messages - they should tell you what's wrong

---

**Remember**: Good logging and error handling means:
- ✅ Clear, actionable error messages
- ✅ Proper log levels (DEBUG/INFO/ERROR)
- ✅ Context in every log line
- ✅ Graceful degradation
- ✅ Helpful tips for fixing issues

**Test early, test often, and trust the logs!**
