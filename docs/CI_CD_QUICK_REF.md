# CI/CD Quick Reference

**Last Updated:** 2026-01-24

---

## 🚀 Quick Commands

### Release New Version
```bash
# Bump version (patch: 0.1.0 → 0.1.1)
./scripts/bump-version.sh patch

# Bump version (minor: 0.1.0 → 0.2.0)
./scripts/bump-version.sh minor

# Bump version (major: 0.1.0 → 1.0.0)
./scripts/bump-version.sh major

# Push to trigger release
git push origin v0.1.1
```

### Install From Source
```bash
# Install script (all platforms)
curl -fsSL https://raw.githubusercontent.com/brentmzey/lovebside/main/scripts/install.sh | bash

# Homebrew (macOS/Linux)
brew tap brentmzey/bside
brew install bside
```

### Build Locally
```bash
# Android APK
./gradlew :composeApp:assembleDebug

# iOS Framework
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64

# Desktop (current OS)
./gradlew :composeApp:createDistributable

# Web
./gradlew :composeApp:jsBrowserProductionWebpack
```

---

## 📋 Workflows

| Workflow | Trigger | Duration | Purpose |
|----------|---------|----------|---------|
| **CI** | Push/PR | ~45 min | Build & test all platforms |
| **CD** | Tag `v*.*.*` | ~2 hours | Release & deploy |

---

## 🎯 Release Checklist

- [ ] All tests passing locally
- [ ] Version bumped in `gradle.properties`
- [ ] Changelog updated
- [ ] Secrets configured (see CI_CD.md)
- [ ] Tag created and pushed
- [ ] Monitor GitHub Actions
- [ ] Verify artifacts uploaded
- [ ] Test install on each platform
- [ ] Update documentation

---

## 📦 Artifacts

### Android
- Debug APK: `build/outputs/apk/debug/*.apk`
- Release APK: `build/outputs/apk/release/*.apk`
- AAB: `build/outputs/bundle/release/*.aab`

### iOS
- Framework: `build/bin/iosArm64/releaseFramework/`
- IPA: (TODO) Archive via Xcode

### Desktop
- macOS DMG: `build/compose/binaries/main/*.dmg`
- Windows MSI: `build/compose/binaries/main/*.msi`
- Linux DEB: `build/compose/binaries/main/*.deb`

### Web
- Production: `build/dist/js/productionExecutable/`

---

## 🔐 Required Secrets

### Minimal (CI only)
- `GITHUB_TOKEN` (automatic)

### Full Deployment
- Android: `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, etc.
- iOS: `IOS_CERTIFICATE_P12`, `APPSTORE_API_KEY_ID`, etc.
- Homebrew: `HOMEBREW_TAP_TOKEN`
- Web: `NETLIFY_AUTH_TOKEN` (optional)

See [CI_CD.md](CI_CD.md) for full list.

---

## 🐛 Common Issues

### Build Failing
```bash
# Clean and rebuild
./gradlew clean
./gradlew build --refresh-dependencies

# Check Java version
java -version  # Should be 17 or 21
```

### Tag Issues
```bash
# Delete and recreate tag
git tag -d v0.1.0
git push origin :refs/tags/v0.1.0
git tag -a v0.1.0 -m "Release 0.1.0"
git push origin v0.1.0
```

### Workflow Not Triggering
1. Check `.github/workflows/` files
2. Verify branch protection rules
3. Ensure workflows enabled in Settings

---

## 📊 Monitoring

- **Actions**: https://github.com/brentmzey/lovebside/actions
- **Releases**: https://github.com/brentmzey/lovebside/releases
- **Badges**: 
  - CI: `[![CI](https://github.com/brentmzey/lovebside/workflows/CI/badge.svg)](https://github.com/brentmzey/lovebside/actions)`
  - CD: `[![CD](https://github.com/brentmzey/lovebside/workflows/CD/badge.svg)](https://github.com/brentmzey/lovebside/actions)`

---

## 📚 Full Documentation

See [CI_CD.md](CI_CD.md) for comprehensive documentation.
