# CI/CD Pipeline Documentation

**Last Updated:** 2026-01-24  
**Status:** ✅ Configured & Ready

---

## 📋 Overview

BSide uses **GitHub Actions** for continuous integration and deployment across all platforms:
- Android (Google Play Store)
- iOS (TestFlight/App Store)
- Desktop (macOS, Windows, Linux)
- Web (Netlify/Vercel)

---

## 🔄 Workflows

### 1. CI - Build & Test (`.github/workflows/ci.yml`)

**Triggers:**
- Push to `main` or `development` branches
- Pull requests to `main` or `development`
- Manual trigger via `workflow_dispatch`

**Jobs:**

| Job | Platform | Duration | Description |
|-----|----------|----------|-------------|
| `test-unit` | Linux | ~5 min | Unit tests (all modules) |
| `build-android` | Linux | ~10 min | Android APK (debug) |
| `build-ios` | macOS | ~15 min | iOS framework |
| `build-desktop` | Linux/macOS/Windows | ~10 min | Desktop packages |
| `build-web` | Linux | ~10 min | Web production build |
| `code-quality` | Linux | ~5 min | Lint checks |
| `ci-summary` | Linux | ~1 min | Overall status |

**Artifacts:**
- Test reports (retained 7 days)
- Debug APKs (retained 30 days)
- Desktop distributions (retained 30 days)
- Web builds (retained 30 days)

**Status Badge:**
```markdown
[![CI](https://github.com/brentmzey/lovebside/workflows/CI%20-%20Build%20&%20Test/badge.svg)](https://github.com/brentmzey/lovebside/actions)
```

---

### 2. CD - Release & Deploy (`.github/workflows/release.yml`)

**Triggers:**
- Push tags matching `v*.*.*` (e.g., `v1.0.0`)
- Manual trigger via `workflow_dispatch` with version input

**Jobs:**

| Job | Platform | Duration | Outputs |
|-----|----------|----------|---------|
| `create-release` | Linux | ~1 min | GitHub Release |
| `release-android` | Linux | ~20 min | Signed APK → Play Store |
| `release-ios` | macOS | ~30 min | IPA → TestFlight |
| `release-desktop` | Linux/macOS/Windows | ~15 min | DMG/MSI/DEB packages |
| `release-web` | Linux | ~10 min | Web archive → Netlify |
| `update-homebrew` | Linux | ~2 min | Homebrew formula |
| `update-install-script` | Linux | ~1 min | Install script |

**Release Assets:**
- `bside-{version}.apk` - Android APK
- `bside-{version}.ipa` - iOS IPA (TODO)
- `bside-{version}-macOS.dmg` - macOS installer
- `bside-{version}-Windows.msi` - Windows installer
- `bside-{version}-Linux.deb` - Linux package
- `bside-web-{version}.tar.gz` - Web archive
- `checksums-*.txt` - SHA256 checksums

---

## 🚀 Releasing a New Version

### Method 1: Tag-Based Release (Recommended)

```bash
# 1. Update version in code
# Edit gradle.properties or version.properties

# 2. Commit changes
git add .
git commit -m "chore: Bump version to 1.0.0"

# 3. Create and push tag
git tag -a v1.0.0 -m "Release version 1.0.0"
git push origin v1.0.0

# 4. GitHub Actions automatically:
#    - Creates release
#    - Builds all platforms
#    - Uploads artifacts
#    - Deploys to stores
```

### Method 2: Manual Workflow Dispatch

```bash
# 1. Go to GitHub Actions tab
# 2. Select "CD - Release & Deploy"
# 3. Click "Run workflow"
# 4. Enter version (e.g., 1.0.0)
# 5. Choose if pre-release
# 6. Click "Run workflow"
```

---

## 📦 Installation Methods

### 1. Homebrew (macOS/Linux)

```bash
brew tap brentmzey/bside
brew install bside
```

**Update:**
```bash
brew update
brew upgrade bside
```

---

### 2. Install Script (All Platforms)

```bash
curl -fsSL https://raw.githubusercontent.com/brentmzey/lovebside/main/scripts/install.sh | bash
```

**What it does:**
- Detects OS and architecture
- Downloads latest release
- Installs platform-specific package
- Sets up PATH (if needed)

**Manual download:**
```bash
# Download script
curl -fsSL -o install.sh https://raw.githubusercontent.com/brentmzey/lovebside/main/scripts/install.sh

# Review script (always review before executing!)
less install.sh

# Make executable and run
chmod +x install.sh
./install.sh
```

---

### 3. Direct Download (GitHub Releases)

1. Go to: https://github.com/brentmzey/lovebside/releases
2. Select latest release
3. Download asset for your platform:
   - **Android**: `bside-{version}.apk`
   - **macOS**: `bside-{version}-macOS.dmg`
   - **Windows**: `bside-{version}-Windows.msi`
   - **Linux**: `bside-{version}-Linux.deb`
   - **Web**: `bside-web-{version}.tar.gz`
4. Verify checksum (optional but recommended):
   ```bash
   sha256sum -c checksums-*.txt
   ```
5. Install:
   - **macOS**: Open DMG, drag to Applications
   - **Windows**: Run MSI installer
   - **Linux**: `sudo dpkg -i bside-*.deb`

---

### 4. App Stores

#### Google Play Store
- **Link**: https://play.google.com/store/apps/details?id=love.bside.app
- **Auto-update**: Yes
- **Requirements**: Android 8.0+ (API 26)

#### Apple App Store
- **Link**: (TODO: Add after submission)
- **Auto-update**: Yes
- **Requirements**: iOS 14.0+

---

## 🔐 Secrets Configuration

Required GitHub Secrets for full CI/CD:

### Android Signing
```
ANDROID_KEYSTORE_BASE64 - Base64-encoded keystore file
ANDROID_KEYSTORE_PASSWORD - Keystore password
ANDROID_KEY_ALIAS - Key alias
ANDROID_KEY_PASSWORD - Key password
```

**Setup:**
```bash
# Generate keystore (if not exists)
keytool -genkey -v -keystore bside.keystore -alias bside -keyalg RSA -keysize 2048 -validity 10000

# Encode to base64
base64 bside.keystore | pbcopy  # macOS
base64 bside.keystore | xclip   # Linux
```

### Google Play Deployment
```
PLAY_SERVICE_ACCOUNT_JSON - Service account JSON for Play Console API
```

**Setup:**
1. Go to Google Play Console
2. Setup → API access
3. Create service account
4. Grant permissions
5. Download JSON key
6. Add to GitHub secrets

### iOS Signing
```
IOS_CERTIFICATE_P12 - Base64-encoded certificate
IOS_CERTIFICATE_PASSWORD - Certificate password
IOS_PROVISIONING_PROFILE - Base64-encoded provisioning profile
APPSTORE_ISSUER_ID - App Store Connect issuer ID
APPSTORE_API_KEY_ID - API key ID
APPSTORE_API_PRIVATE_KEY - API private key
```

### Homebrew Tap
```
HOMEBREW_TAP_TOKEN - GitHub token with repo access to homebrew-bside
```

### Web Deployment (Optional)
```
NETLIFY_AUTH_TOKEN - Netlify authentication token
NETLIFY_SITE_ID - Netlify site ID
```

---

## 🧪 Testing Workflows Locally

### Using `act` (GitHub Actions locally)

```bash
# Install act
brew install act  # macOS
# or: https://github.com/nektos/act

# Run CI workflow
act push -W .github/workflows/ci.yml

# Run specific job
act -j test-unit

# With secrets
act -s GITHUB_TOKEN=$GITHUB_TOKEN
```

### Manual Testing

```bash
# Test Android build
./gradlew :composeApp:assembleDebug

# Test iOS build (macOS only)
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64

# Test Desktop build
./gradlew :composeApp:createDistributable

# Test Web build
./gradlew :composeApp:jsBrowserProductionWebpack

# Test install script
bash scripts/install.sh
```

---

## 📊 Monitoring & Metrics

### GitHub Actions Dashboard
- https://github.com/brentmzey/lovebside/actions

### Workflow Status
- CI: [![CI](https://github.com/brentmzey/lovebside/workflows/CI%20-%20Build%20&%20Test/badge.svg)](https://github.com/brentmzey/lovebside/actions)
- CD: [![CD](https://github.com/brentmzey/lovebside/workflows/CD%20-%20Release%20&%20Deploy/badge.svg)](https://github.com/brentmzey/lovebside/actions)

### Metrics
- **CI Pipeline**: ~45 minutes total
- **Release Pipeline**: ~2 hours total
- **Success Rate**: Target 95%+
- **Artifact Size**: 
  - Android APK: ~50MB
  - iOS IPA: ~80MB
  - Desktop: ~100-150MB
  - Web: ~20MB

---

## 🐛 Troubleshooting

### Build Failures

**Android: Signing issues**
```
Error: Keystore was tampered with, or password was incorrect
```
**Fix:** Verify `ANDROID_KEYSTORE_PASSWORD` secret

**iOS: Provisioning profile**
```
Error: No profiles for 'love.bside.app' were found
```
**Fix:** Check provisioning profiles in Apple Developer Portal

**Desktop: Package creation**
```
Error: jpackage not found
```
**Fix:** Ensure JDK 17+ with jpackage module

**Web: Node modules**
```
Error: Module not found
```
**Fix:** Clear cache, reinstall node_modules

### Release Issues

**Tag already exists**
```bash
# Delete local and remote tag
git tag -d v1.0.0
git push origin :refs/tags/v1.0.0

# Create new tag
git tag -a v1.0.0 -m "Release 1.0.0"
git push origin v1.0.0
```

**Workflow not triggering**
- Check workflow file syntax (YAML validation)
- Verify branch protection rules
- Check if workflows are enabled in repo settings

---

## 🔄 Continuous Improvement

### Planned Enhancements

- [ ] Automated changelog generation
- [ ] Version bump automation
- [ ] Parallel E2E tests
- [ ] Performance benchmarking in CI
- [ ] Visual regression testing
- [ ] Security scanning (SAST/DAST)
- [ ] Dependency vulnerability checks
- [ ] Code coverage reporting
- [ ] Slack/Discord notifications
- [ ] Rollback mechanisms

### Optimization Opportunities

- **Cache Strategy**: Improve Gradle cache hit rate
- **Parallelization**: Run more jobs concurrently
- **Incremental Builds**: Only build changed modules
- **Artifact Optimization**: Reduce package sizes
- **Build Times**: Target <30 min for CI

---

## 📚 Resources

- **GitHub Actions Docs**: https://docs.github.com/actions
- **Gradle Build Cache**: https://docs.gradle.org/current/userguide/build_cache.html
- **Android App Signing**: https://developer.android.com/studio/publish/app-signing
- **iOS Distribution**: https://developer.apple.com/documentation/xcode/distributing-your-app-for-beta-testing-and-releases
- **Homebrew Taps**: https://docs.brew.sh/How-to-Create-and-Maintain-a-Tap

---

## 🤝 Contributing to CI/CD

When modifying workflows:

1. **Test locally** with `act` or manual builds
2. **Use workflow_dispatch** for testing changes
3. **Document changes** in this file
4. **Update secrets** if new ones are needed
5. **Notify team** of breaking changes

---

**Maintained by:** BSide DevOps Team  
**Last Review:** 2026-01-24  
**Next Review:** 2026-02-24
