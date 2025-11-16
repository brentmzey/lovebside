# Gradle Build Roadmap

This document tracks the progress toward a fully functional `gradle build` command that handles all project compilation, testing, and artifact distribution.

## 🎯 Goal

Execute a single command to build, test, and package the entire B-Side multiplatform project:

```bash
gradle build
# OR
gradle clean build
```

This should:
- ✅ Compile all Kotlin Multiplatform targets
- ✅ Run all tests
- ✅ Generate all artifacts (APKs, frameworks, JARs, bundles)
- ✅ Produce distribution-ready packages

---

## 📊 Current Status (as of November 16, 2024)

### ✅ What Works

**Compilation (all targets):**
```bash
gradle assemble -x jsBrowserProductionWebpack
```

Successfully builds:
- ✅ Android (ARM, x86) - Debug & Release APKs
- ✅ iOS (ARM64 device + simulator) - Frameworks
- ✅ JVM Desktop - JAR files
- ✅ JavaScript/Web - Development bundle
- ✅ Server - Shadow JAR

**Current workaround needed:**
- `-x jsBrowserProductionWebpack` to skip problematic webpack task

### ⚠️ Known Issues

1. **JS/Web Production Webpack** (Issue #1)
   - **Status:** Blocked
   - **Task:** `jsBrowserProductionWebpack`
   - **Error:** Cannot find node module "webpack/bin/webpack.js"
   - **Root Cause:** Kotlin/JS Gradle plugin dependency resolution with Gradle 9.x
   - **Impact:** Production web bundle not created
   - **Workaround:** Development build works perfectly via `./scripts/run-web.sh`
   - **Notes:** This is a known Kotlin/JS plugin issue, affects production builds only

2. **Test Failures** (Issue #2)
   - **Status:** Not investigated yet
   - **Task:** `:shared:testReleaseUnitTest`, `:shared:testDebugUnitTest`
   - **Command:** `gradle build` (includes tests)
   - **Impact:** Tests fail, blocking full build
   - **Workaround:** `gradle assemble` (skip tests) or `gradle build -x test`

---

## 🗺️ Roadmap to `gradle build`

### Phase 1: Fix Compilation Issues ⏳
- [x] Update Gradle wrapper to match system (9.2.0)
- [x] Verify all KMP targets compile
- [ ] **Fix JS production webpack issue**
  - Option A: Wait for Kotlin plugin update
  - Option B: Configure custom webpack resolver
  - Option C: Accept dev-only builds for now
- [ ] Remove `-x jsBrowserProductionWebpack` exclusion

**Estimated Completion:** Q1 2025 (depends on Kotlin plugin)

### Phase 2: Fix Test Suite 📋
- [ ] Investigate failing Android tests
- [ ] Review shared module tests
- [ ] Fix or skip flaky tests
- [ ] Ensure all tests pass
- [ ] Remove `-x test` exclusions

**Estimated Completion:** 1-2 weeks

### Phase 3: Optimize Build Configuration 🔧
- [ ] Review Gradle build cache settings
- [ ] Configure incremental compilation
- [ ] Optimize dependency resolution
- [ ] Add build performance monitoring

**Estimated Completion:** 1 week

### Phase 4: Distribution & Artifacts 📦
- [ ] Configure Android app signing
- [ ] Set up iOS framework export
- [ ] Package desktop distributions (DMG, EXE, DEB)
- [ ] Create web deployment bundle
- [ ] Generate server Docker image

**Estimated Completion:** 2-3 weeks

### Phase 5: CI/CD Integration 🔄
- [ ] Re-enable GitHub Actions (selectively)
- [ ] Configure automated testing
- [ ] Set up artifact publishing
- [ ] Add version management

**Estimated Completion:** 1-2 weeks

---

## 🎯 Success Criteria

When complete, these commands should work perfectly:

```bash
# Full build with tests
gradle build

# Clean rebuild
gradle clean build

# Release builds
gradle assembleRelease

# Run all tests
gradle test

# Distribution packages
gradle :composeApp:packageDistributionForCurrentOS
gradle :composeApp:assembleRelease
gradle :server:shadowJar
```

---

## 📝 Incremental Progress

### Milestone 1: Basic Build ✅ **COMPLETE**
- [x] All targets compile successfully
- [x] Artifacts generated (with exclusions)
- [x] System Gradle integration
- [x] Documentation complete

**Command:** `gradle assemble -x jsBrowserProductionWebpack`

### Milestone 2: Full Compilation (Target: Q1 2025)
- [ ] All targets compile without exclusions
- [ ] JS production webpack working
- [ ] No workarounds needed

**Target Command:** `gradle assemble`

### Milestone 3: Tests Passing (Target: Q1 2025)
- [ ] All unit tests pass
- [ ] Integration tests stable
- [ ] Test coverage reporting

**Target Command:** `gradle build`

### Milestone 4: Complete CI/CD (Target: Q2 2025)
- [ ] Automated builds
- [ ] Automated testing
- [ ] Artifact publishing
- [ ] Release automation

**Target Command:** Full CI/CD pipeline with `gradle build`

---

## 🔧 Current Recommended Commands

**For daily development:**
```bash
./scripts/run-desktop.sh    # Fastest iteration
./scripts/run-web.sh         # Web with hot-reload
./scripts/start-all.sh       # Full stack
```

**For validation:**
```bash
./scripts/verify-targets.sh  # Quick compilation check
./scripts/build-all.sh       # Full build wrapper
```

**For manual builds:**
```bash
gradle assemble -x jsBrowserProductionWebpack
```

---

## 📚 Related Documentation

- [README.md](../README.md) - Main project documentation
- [scripts/README.md](../scripts/README.md) - Development script guide
- [BUILD_STATUS.md](./BUILD_STATUS.md) - CI/CD configuration

---

## 🐛 Issue Tracking

### Issue #1: JS Production Webpack
**Priority:** Medium  
**Assignee:** Waiting on Kotlin/JS plugin team  
**Discussion:** https://youtrack.jetbrains.com/issue/KT-XXXXX (TBD)

### Issue #2: Test Failures
**Priority:** High  
**Assignee:** Unassigned  
**Next Steps:** 
1. Run tests individually to isolate failures
2. Review test logs
3. Fix or mark as flaky

---

**Last Updated:** November 16, 2024  
**Status:** Phase 1 - Milestone 1 Complete ✅
