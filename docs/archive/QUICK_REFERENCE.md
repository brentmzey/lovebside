# Quick Reference Card

## 🚀 Commands You'll Use

```bash
# Compile and check for errors
./gradlew :shared:compileKotlinJvm --no-daemon

# Run full test suite
./gradlew :composeApp:run --args="--test"

# Run app (includes smoke test)
./gradlew :composeApp:run

# Count compilation errors
./gradlew :shared:compileKotlinJvm --no-daemon 2>&1 | grep -c "^e:"

# See first error only
./gradlew :shared:compileKotlinJvm --no-daemon 2>&1 | grep "^e:" | head -1
```

## 📁 Key Files

| File | Purpose |
|------|---------|
| `PocketBaseClient.kt` | Main API client |
| `PocketBaseTestHelper.kt` | Test utilities |
| `PocketBaseManualTests.kt` | Test suite |
| `ValuesRepository.kt` | Example repository |
| `HOW_TO_TEST_AND_COMPILE.md` | Testing guide |
| `NEXT_STEPS_SUMMARY.md` | What to do next |

## 🎯 Testing Checklist

- [ ] Compile succeeds
- [ ] Smoke test passes
- [ ] Full tests pass
- [ ] Logs are clear
- [ ] Errors are graceful

## 📊 Log Levels

| Level | When | Example |
|-------|------|---------|
| DEBUG 🔍 | Details | `🔍 PocketBase.getList: page=1` |
| INFO ℹ️ | Success | `ℹ️ ✅ Success - fetched 5 items` |
| WARN ⚠️ | Issues | `⚠️ Rate limited` |
| ERROR ❌ | Failures | `❌ HTTP 404 - Not found` |

## 🔧 Common Fixes

| Issue | Fix |
|-------|-----|
| Compilation error | Check imports, use `love.bside.app.core.Result` |
| Test fails | Check internet, verify PocketBase URL |
| No logs | Set `Environment.DEVELOPMENT` |
| 404 error | Verify collection exists in PocketBase |

## 📚 Docs Quick Links

- **How to test?** → `HOW_TO_TEST_AND_COMPILE.md`
- **Tests failing?** → `TESTING_GUIDE.md`
- **What patterns?** → `COMPILATION_FIX_PROGRESS.md`
- **What's done?** → `FINAL_STATUS_SUMMARY.md`
- **What next?** → `NEXT_STEPS_SUMMARY.md`

## ✅ Success Looks Like

```
✅ PocketBase connection successful!
✅ Fetched 5 key values
✅ All health checks passed!
```

## ❌ Errors Look Like

```
❌ HTTP 404 - Collection not found
💡 Tip: Verify collection exists
```

---

**Need help?** Check the full docs above! 📚
