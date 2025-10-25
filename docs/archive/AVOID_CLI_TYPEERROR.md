# How to Avoid CLI TypeError Issues

**Issue:** GitHub Copilot CLI throws `TypeError: Cannot read properties of undefined (reading 'startsWith')` repeatedly during sessions.

## Root Cause

This is a **CLI tool bug**, not a project issue. It appears to happen when:
- File globbing patterns are used (e.g., `*.log`, `*.gradle.kts`)
- Certain bash commands with wildcards trigger the error
- The CLI tries to process command output in specific ways

## Workarounds That Work

### 1. Use `find` instead of globbing
```bash
# ✗ BAD - Triggers TypeError
ls *.log

# ✓ GOOD - Works reliably
find . -name "*.log" -type f
```

### 2. Use explicit paths
```bash
# ✗ BAD - Triggers TypeError
ls composeApp/*.gradle.kts shared/*.gradle.kts

# ✓ GOOD - Works reliably
find . -type f -name "*.gradle.kts" | grep -v build
```

### 3. Add fallback commands
```bash
# ✗ BAD - Triggers TypeError on failure
ls *.log 2>/dev/null

# ✓ GOOD - Always returns something
ls *.log 2>/dev/null || echo "No log files found"
```

### 4. Use grep/head to limit output
```bash
# ✗ BAD - Large output might trigger issues
ls -la

# ✓ GOOD - Controlled output
ls -la | head -20
```

## Session Recovery Strategy

When you hit the TypeError:

1. **Don't panic** - Your project is fine, this is a CLI bug
2. **Check SESSION_HANDOFF.md** - All context is preserved there
3. **Use alternative commands** - See workarounds above
4. **Continue progress** - The work you did was saved

## Files Created to Support Session Continuity

1. **SESSION_HANDOFF.md** - Complete session context and next steps
2. **BUILD_AUTOMATION.md** - Full build documentation
3. **master-build.sh** - Automated build script
4. **This file** - How to avoid the TypeError

## Quick Commands That Work

```bash
# Check project status
./gradlew projects
./gradlew tasks --group="build"

# Build the project
./gradlew build -x test
./master-build.sh --skip-tests

# Find files safely
find . -name "*.kt" -type f | head -20
find . -name "build.gradle.kts" -type f

# List safely
ls -1 | head -20
ls -la | grep "^d" | head -10

# Check build output
tail -50 build-master.log
```

## For Future AI Sessions

When you encounter the TypeError:

1. **Immediately switch to `find` commands** instead of glob patterns
2. **Use command chaining** with `&&` to combine operations
3. **Limit output** with `head`, `tail`, or `grep`
4. **Read SESSION_HANDOFF.md** to understand context
5. **Continue the work** - don't restart from scratch

## Current Build Status

The Gradle build works but has 2 known issues:

1. **kotlinStoreYarnLock** - Run `./gradlew kotlinUpgradeYarnLock`
2. **iOS compilation** - May require Xcode setup fixes

These are **project issues**, not CLI issues. They can be fixed in the next session.

## What Actually Works

✅ Gradle multi-project setup is correct
✅ All Kotlin code compiles (JVM targets)
✅ PocketBase backend runs
✅ Server backend compiles
✅ Android target builds
✅ Documentation is comprehensive
✅ Automation scripts are in place

## Next Session Action Items

1. Fix yarn lock: `./gradlew kotlinUpgradeYarnLock`
2. Debug iOS build issues (check Xcode setup)
3. Add basic unit tests
4. Complete production deployment script
5. Test full build pipeline

---

**Remember:** The TypeError is annoying but doesn't affect your project. Use the workarounds above and keep making progress!
