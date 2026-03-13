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

## 📊 Current Status (as of November 28, 2025)

### ✅ What Works

**Full build (all targets + unit tests):**
```bash
gradle build
```

This single command now compiles Android, iOS (device + simulator), Desktop, Web (including the production `jsBrowserProductionWebpack` bundle), and the server module while executing every configured unit test suite.

**Spot-check commands verified on 2025-11-28:**
- `gradle :composeApp:jsBrowserProductionWebpack`
- `gradle :shared:testReleaseUnitTest`
- `gradle :shared:testDebugUnitTest`
- `gradle :pocketbase-kt-sdk:iosSimulatorArm64Test`
- `gradle :server:test`

### ⚠️ Remaining Focus

- Gradle still emits configuration-time `jsNpmAggregated` warnings; these should be cleaned up before Gradle 10.
- Deprecation warnings continue to appear under `--warning-mode all` and need triage.
- Capture build metrics so we can optimize configuration cache reuse and overall build time.

---

## 🗺️ Roadmap to `gradle build`

### Phase 1: Fix Compilation Issues ✅ *(Completed November 2025)*
- [x] Update Gradle wrapper to match system (9.2.0)
- [x] Verify all KMP targets compile
- [x] Fix JS production webpack issue
- [x] Remove `-x jsBrowserProductionWebpack` exclusion

### Phase 2: Fix Test Suite ✅ *(Completed November 2025)*
- [x] Investigate failing Android/shared tests
- [x] Review shared module tests
- [x] Fix or quarantine flaky tests
- [x] Ensure all tests pass with `gradle build`

### Phase 3: Optimize Build Configuration 🔧
- [ ] Review Gradle build cache settings
- [ ] Configure incremental compilation
- [ ] Optimize dependency resolution
- [ ] Add build performance monitoring

### Phase 4: Distribution & Artifacts 📦
- [ ] Configure Android app signing
- [ ] Set up iOS framework export
- [ ] Package desktop distributions (DMG, EXE, DEB)
- [ ] Create web deployment bundle
- [ ] Generate server Docker image

### Phase 5: CI/CD Integration 🔄
- [ ] Re-enable GitHub Actions (selectively)
- [ ] Configure automated testing
- [ ] Set up artifact publishing
- [ ] Add version management

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

### Milestone 1: Basic Build ✅ *(Completed 2024-11-16)*
- [x] All targets compile successfully (with temporary exclusions)
- [x] Artifacts generated
- [x] System Gradle integration
- [x] Documentation complete

**Command (historical):** `gradle assemble -x jsBrowserProductionWebpack`

### Milestone 2: Full Compilation ✅ *(Completed 2025-11-15)*
- [x] All targets compile without exclusions
- [x] JS production webpack working
- [x] No workarounds needed

**Command:** `gradle assemble`

### Milestone 3: Tests Passing ✅ *(Completed 2025-11-28)*
- [x] All unit tests pass inside `gradle build`
- [x] Device/simulator tests run cleanly via `verify-targets`
- [x] Test results captured in `build/reports/tests` for follow-up metrics

**Command:** `gradle build`

### Milestone 4: Performance & CI/CD ♻️ *(In Progress)*
- [ ] Automate builds + publishing via GitHub Actions
- [ ] Improve configuration-cache hit rate & telemetry
- [ ] Wire up release automation + artifact signing

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
gradle build
```

---

## 📚 Related Documentation

- [README.md](../README.md) - Main project documentation
- [scripts/README.md](../scripts/README.md) - Development script guide
- [BUILD_STATUS.md](./BUILD_STATUS.md) - CI/CD configuration

---

## 🐛 Issue Tracking

### Issue #1: Configuration-Time npm Resolution
**Priority:** Medium  
**Assignee:** Unassigned  
**Next Steps:**
1. Audit `jsNpmAggregated` usage and lazy-load where possible.
2. Verify fixes under `--configuration-cache` and Gradle 9.3+.
3. Close out warnings before Gradle 10.

### Issue #2: Gradle 10 Readiness
**Priority:** Medium  
**Assignee:** Unassigned  
**Next Steps:**
1. Re-run `gradle build --warning-mode all` and catalogue deprecations.
2. Patch build logic/plugins to eliminate deprecated APIs.
3. Add regression test in CI once GitHub Actions is re-enabled.

---

**Last Updated:** November 28, 2025  
**Status:** Phase 3 - Optimization in Progress ♻️
