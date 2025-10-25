# 🚀 Complete Deployment Guide - Step by Step

## 📋 The Complete Flow

```
Your Local Machine → GitHub → GitHub Actions → Production
                              ↓
                        Automated Builds
                              ↓
            ┌─────────────────┼─────────────────┐
            ↓                 ↓                 ↓
        Backend           Clients          Artifacts
      (Auto-Deploy)    (Download)       (Released)
```

---

## 🎯 STEP-BY-STEP PROCESS

### **STEP 1: Push Code to GitHub** (5 minutes)

```bash
cd /Users/brentzey/bside

# Option A: Run automated setup
./setup-cicd.sh

# Option B: Manual commands
git add .github/ server/ composeApp/ shared/ *.md
git add docker-compose.yml setup-cicd.sh build-and-run.sh
git commit -m "Production-ready: CI/CD pipeline + multiplatform builds + deployment"
git push origin development
```

**What happens immediately:**
- Code pushed to GitHub repository
- GitHub Actions triggers automatically
- Workflows start running in parallel

---

### **STEP 2: GitHub Actions Runs** (10-15 minutes) - **AUTOMATIC**

**What GitHub Actions Does FOR YOU:**

#### Build Jobs (Parallel):

**1. Backend Server** ✅
```yaml
Platform: Linux Ubuntu
Output: server-1.0.0-all.jar (27MB)
What: Self-contained JAR with all dependencies
Time: ~3 minutes
```

**2. Android** ✅
```yaml
Platform: Linux Ubuntu
Output: composeApp-debug.apk (20MB)
What: Installable APK for all Android devices
Time: ~5 minutes
Where: Install via adb or sideload
```

**3. Desktop - macOS** ✅
```yaml
Platform: macOS runner
Output: love.bside.app-1.0.0.dmg (116MB)
What: Native macOS installer
Time: ~4 minutes
Install: Double-click DMG → Drag to Applications
```

**4. Desktop - Windows** ✅
```yaml
Platform: Windows runner
Output: love.bside.app-1.0.0.msi
What: Native Windows installer
Time: ~4 minutes
Install: Double-click MSI → Next → Install
```

**5. Desktop - Linux** ✅
```yaml
Platform: Linux Ubuntu
Output: love.bside.app-1.0.0.deb
What: Debian package for Ubuntu/Debian
Time: ~4 minutes
Install: sudo dpkg -i *.deb
```

**6. Web** ✅
```yaml
Platform: Linux Ubuntu
Output: Static JS/HTML/CSS files
What: Browser-compatible web app (Kotlin/JS)
Time: ~3 minutes
Deploy: Upload to CDN or static hosting
```

**7. Tests & Quality** ✅
```yaml
- Run all unit tests
- Run integration tests
- Security vulnerability scan
- Code quality analysis (Detekt)
```

---

### **STEP 3: Download Artifacts** (2 minutes)

Go to: `https://github.com/brentmzey/lovebside/actions`

**Click on latest workflow run → Scroll to "Artifacts"**

Available downloads:
- ✅ `backend-server` → server-1.0.0-all.jar
- ✅ `android-apk-debug` → composeApp-debug.apk
- ✅ `desktop-macos` → love.bside.app-1.0.0.dmg
- ✅ `desktop-windows` → love.bside.app-1.0.0.msi
- ✅ `desktop-linux` → love.bside.app-1.0.0.deb
- ✅ `web-distribution` → Static files

---

### **STEP 4A: Deploy Backend (AUTOMATIC - If Configured)**

**When you push to `main` branch or create a release:**

```yaml
GitHub Actions automatically:
  1. Builds server JAR
  2. SSHs into your server
  3. Copies JAR to /opt/bside/
  4. Restarts systemd service
  5. Verifies health endpoint
  6. Rolls back if failure
```

**Manual deployment (if secrets not configured):**
```bash
# Download server-1.0.0-all.jar from artifacts

# Deploy to your server
scp server-1.0.0-all.jar user@your-server.com:/opt/bside/

# SSH and restart
ssh user@your-server.com
sudo systemctl restart bside-server
curl http://localhost:8080/health  # Verify
```

---

### **STEP 4B: Distribute Client Apps**

#### **Android Distribution**

**Option 1: Google Play Store** (Recommended for public release)
```bash
# 1. Download signed APK from artifacts
# 2. Go to: https://play.google.com/console
# 3. Create app listing
# 4. Upload APK to Internal Testing track
# 5. Promote to Production after testing
```

**Option 2: Direct APK** (Beta testing / Internal)
```bash
# Users download APK
# Enable "Install from Unknown Sources"
# Install APK
# App runs natively on device
```

**Option 3: F-Droid** (Open source stores)
```bash
# Submit to F-Droid repository
# Open source community distribution
```

#### **iOS Distribution** (After fixing K/N compiler)

**Option 1: Apple App Store**
```bash
# 1. Open iosApp/iosApp.xcodeproj in Xcode
# 2. Select "Any iOS Device"
# 3. Product → Archive
# 4. Upload to App Store Connect
# 5. Submit for review
```

**Option 2: TestFlight** (Beta testing)
```bash
# Same as above, but select TestFlight distribution
# Share test link with beta testers
```

**Option 3: Ad Hoc** (Internal team)
```bash
# Export IPA with ad hoc provisioning
# Install via Xcode or configurator
```

#### **Desktop Distribution**

**macOS (DMG)** ✅
```bash
# Users download: love.bside.app-1.0.0.dmg
# Double-click DMG
# Drag app to Applications folder
# Launch from Applications
```

**Windows (MSI)** ✅
```bash
# Users download: love.bside.app-1.0.0.msi
# Double-click installer
# Click "Next" → "Install"
# Launch from Start Menu or Desktop
```

**Linux (DEB)** ✅
```bash
# Users download: love.bside.app-1.0.0.deb
# Install: sudo dpkg -i love.bside.app-1.0.0.deb
# Or: double-click in file manager
# Launch from applications menu
```

**AppImage (Universal Linux)** - Can add to pipeline
```bash
# Single file that runs on any Linux
# No installation required
# Just make executable and run
```

#### **Web Distribution**

**Option 1: Static Hosting (Easiest)** ✅
```bash
# Download web-distribution artifact
# Upload to:
#   - Netlify: netlify deploy --prod
#   - Vercel: vercel --prod
#   - Cloudflare Pages: wrangler pages publish
#   - GitHub Pages: Push to gh-pages branch
#   - AWS S3 + CloudFront
```

**Option 2: Self-Hosted**
```bash
# Nginx configuration (already in docker-compose.yml)
server {
    listen 80;
    server_name app.bside.love;
    root /var/www/bside;
    
    location / {
        try_files $uri $uri/ /index.html;
    }
}
```

---

## 🔒 SECURITY CONFIGURATION

### **Required GitHub Secrets** (After Step 1)

Go to: `https://github.com/brentmzey/lovebside/settings/secrets/actions`

**Click "New repository secret"** for each:

#### **For Backend Deployment:**
```
Name: SERVER_HOST
Value: your-server.com (or IP address)

Name: SERVER_USER
Value: deploy (or your SSH username)

Name: SERVER_SSH_KEY
Value: [Paste your PRIVATE SSH key]
       -----BEGIN OPENSSH PRIVATE KEY-----
       [entire key content]
       -----END OPENSSH PRIVATE KEY-----
```

#### **For Docker Hub (Optional):**
```
Name: DOCKER_USERNAME
Value: yourdockerhubname

Name: DOCKER_PASSWORD
Value: [Docker Hub access token]
```

#### **For Android Signing (Release builds):**
```
Name: ANDROID_KEYSTORE
Value: [Base64 encoded keystore file]

Name: KEYSTORE_PASSWORD
Value: yourkeystorepassword

Name: KEY_ALIAS
Value: your-key-alias

Name: KEY_PASSWORD
Value: yourkeypassword
```

**Create Android keystore:**
```bash
# Generate keystore
keytool -genkey -v -keystore release.keystore \
  -alias bside-release \
  -keyalg RSA -keysize 2048 -validity 10000

# Encode to base64
base64 release.keystore | tr -d '\n' > keystore.txt

# Copy content and paste into ANDROID_KEYSTORE secret
cat keystore.txt | pbcopy  # macOS
```

### **Security Features Enabled:**

✅ **No secrets in code** - All via GitHub Secrets
✅ **SSH key authentication** - No passwords
✅ **TLS/HTTPS** - For all API calls
✅ **Code signing** - For mobile and desktop apps
✅ **Dependency scanning** - CVE checks
✅ **Health checks** - Before/after deployment
✅ **Rollback** - If deployment fails
✅ **Branch protection** - Require reviews
✅ **Audit logs** - Track all changes

---

## 📱 INSTALLATION GUIDE FOR END USERS

### **Android Users**

**Method 1: Google Play Store** (Once published)
```
1. Open Google Play Store
2. Search "B-Side Dating"
3. Tap "Install"
4. Open app
```

**Method 2: Direct APK**
```
1. Download APK from website
2. Settings → Security → Enable "Unknown Sources"
3. Tap APK file
4. Tap "Install"
5. Open app
```

### **iOS Users** (Once published)

**App Store:**
```
1. Open App Store
2. Search "B-Side Dating"
3. Tap "Get"
4. Open app
```

### **Desktop Users**

**macOS:**
```
1. Download DMG file
2. Double-click to mount
3. Drag app to Applications
4. Open from Applications
5. Allow in Security Preferences if needed
```

**Windows:**
```
1. Download MSI installer
2. Double-click installer
3. Click "Next" → "Install"
4. Find in Start Menu
5. Launch app
```

**Linux:**
```
# Ubuntu/Debian
1. Download .deb file
2. sudo dpkg -i love.bside.app-1.0.0.deb
3. Or double-click in file manager
4. Launch from applications menu
```

### **Web Users**

**Any Browser:**
```
1. Go to: https://app.bside.love
2. Use instantly - no installation
3. Add to home screen (mobile)
4. Works offline (PWA)
```

---

## 🎯 COMPLETE DEPLOYMENT WORKFLOW

### **Development → Production**

```bash
# 1. Develop features locally
git checkout -b feature/my-feature
# ... make changes ...
./gradlew build  # Test locally

# 2. Push to GitHub
git push origin feature/my-feature

# 3. Create Pull Request
gh pr create --title "My Feature"
# → PR Check workflow runs
# → Reviewer approves

# 4. Merge to development
gh pr merge
# → CI/CD workflow runs
# → All platforms build
# → Artifacts created

# 5. Test builds from artifacts
# Download and test on each platform

# 6. Merge to main (when ready)
git checkout main
git merge development
git push origin main
# → Deploys backend to staging

# 7. Create release
git tag -a v1.0.0 -m "Release v1.0.0"
git push origin v1.0.0

gh release create v1.0.0 \
  --title "B-Side v1.0.0" \
  --notes "Production release"
# → Release workflow runs
# → Builds signed artifacts
# → Deploys to production
# → Attaches files to release
# → Notifies team
```

---

## 📊 MONITORING AFTER DEPLOYMENT

### **Backend Monitoring**

```bash
# Check server health
curl https://api.bside.love/health

# View server logs
ssh user@server "journalctl -u bside-server -f"

# Check resource usage
ssh user@server "htop"

# Database status
ssh user@server "curl http://localhost:8090/api/health"
```

### **Client Monitoring**

**Set up:**
- Crashlytics (Android/iOS) - Crash reports
- Sentry - Error tracking
- Google Analytics - Usage stats
- Firebase - Push notifications

### **GitHub Actions Monitoring**

```bash
# View recent runs
gh run list

# Watch current run
gh run watch

# View logs
gh run view --log

# Download artifacts
gh run download
```

---

## 🔄 UPDATE / RELEASE CYCLE

### **Typical Release Cycle:**

**Week 1-2: Development**
- Develop features on feature branches
- CI runs on every push
- PR checks ensure quality

**Week 3: Testing**
- Merge to development
- Download artifacts from CI
- Test on all platforms
- Fix bugs

**Week 4: Release**
- Merge to main
- Create git tag
- Release workflow deploys everything
- Notify users of update

**Continuous:**
- Nightly builds catch issues early
- Backend auto-deploys on main branch
- Clients available for download

---

## 💡 PRO TIPS

### **Optimize CI/CD Costs**

```yaml
# Skip CI on documentation changes
git commit -m "Update docs [skip ci]"

# Run only on specific branches
on:
  push:
    branches: [main, development]
    paths-ignore:
      - '**.md'
      - 'docs/**'
```

### **Fast Feedback**

```bash
# Test locally before pushing
./gradlew test
./gradlew :server:shadowJar
./gradlew :composeApp:assembleDebug

# Use local GitHub Actions testing
act -j build-backend  # Test workflow locally
```

### **Parallel Development**

```bash
# Multiple devs can work simultaneously
# Each push triggers CI
# Artifacts available for testing
# No conflicts with shared types!
```

---

## ✅ FINAL CHECKLIST

### **Before First Push:**
- [ ] Review all workflow files
- [ ] Verify Dockerfile is correct
- [ ] Check docker-compose.yml
- [ ] Read CI_CD_SETUP.md
- [ ] Have server credentials ready

### **After Push to GitHub:**
- [ ] Go to Actions tab
- [ ] Enable GitHub Actions
- [ ] Configure all secrets
- [ ] Manually trigger first workflow
- [ ] Watch build complete
- [ ] Download and test artifacts

### **Before Production:**
- [ ] Test backend deployment
- [ ] Test all client platforms
- [ ] Configure monitoring
- [ ] Set up domain and SSL
- [ ] Create signing keys
- [ ] Test rollback procedure
- [ ] Document for team

---

## 🎉 SUMMARY

**You push code → Everything else is automatic!**

1. **Push to GitHub** (1 command)
2. **CI builds everything** (automatic, 10-15 min)
3. **Download artifacts** (from Actions tab)
4. **Backend deploys** (automatic if secrets configured)
5. **Distribute clients** (Google Play, App Store, Direct download, Web hosting)

**Your CI/CD pipeline handles:**
- ✅ Building all platforms
- ✅ Running all tests
- ✅ Security scanning
- ✅ Creating installers
- ✅ Deploying backend
- ✅ Docker images
- ✅ Release management
- ✅ Artifact distribution

**All secure, all automatic, all professional! 🚀**

---

**Ready to start?** Run this now:

```bash
./setup-cicd.sh
```

Or read **CI_CD_SETUP.md** for detailed instructions.
