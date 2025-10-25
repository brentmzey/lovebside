# 🔨 BUILD, TEST & DEPLOY GUIDE

Complete guide to building, testing, and deploying your multiplatform B-Side app.

---

## 📋 Table of Contents

1. [Quick Start - Build Everything](#quick-start---build-everything)
2. [Build Individual Targets](#build-individual-targets)
3. [Testing](#testing)
4. [Deployment Per Platform](#deployment-per-platform)
5. [Server Architecture (Platform Agnostic)](#server-architecture-platform-agnostic)

---

## 🚀 QUICK START - Build Everything

### One Command to Build All Platforms

```bash
cd /Users/brentzey/bside

# Build EVERYTHING (backend, Android, Desktop, Web)
./gradlew build

# Or use the helper script
./build-and-run.sh
```

**What gets built:**
- ✅ Backend Server JAR (27MB) - `server/build/libs/server-1.0.0-all.jar`
- ✅ Android APK (20MB) - `composeApp/build/outputs/apk/debug/composeApp-debug.apk`
- ✅ Desktop JAR - `composeApp/build/libs/composeApp-jvm.jar`
- ✅ Web JS - `composeApp/build/distributions/`
- ✅ Shared Library - Used by all above

**Build time:** ~2-5 minutes

---

## 🎯 BUILD INDIVIDUAL TARGETS

### 1️⃣ Backend Server (Platform Agnostic)

The server is **completely platform-agnostic** - it accepts JSON and returns JSON. No client-specific code.

```bash
# Build backend JAR
./gradlew :server:build

# Output location
# server/build/libs/server-1.0.0-all.jar (27MB)

# Run locally (test mode)
./gradlew :server:run --args="--port=8080"

# Or run the JAR directly
java -jar server/build/libs/server-1.0.0-all.jar

# Test with curl
curl http://localhost:8080/health
# Returns: {"status":"healthy","version":"1.0.0"}
```

**What the server does:**
- ✅ Accepts JSON requests (Content-Type: application/json)
- ✅ Returns JSON responses
- ✅ Uses shared Kotlin types (serialized to/from JSON automatically)
- ✅ Works with ANY client (mobile, web, desktop, CLI, Postman, etc.)
- ✅ No platform-specific code at all

**Server API Example:**
```bash
# Get profiles (works from any platform)
curl https://api.your-domain.com/api/profiles \
  -H "Content-Type: application/json"

# Create a match (same JSON from any platform)
curl -X POST https://api.your-domain.com/api/matches \
  -H "Content-Type: application/json" \
  -d '{"userId":"123","matchedUserId":"456"}'
```

### 2️⃣ Android App

```bash
# Build debug APK
./gradlew :composeApp:assembleDebug

# Output location
# composeApp/build/outputs/apk/debug/composeApp-debug.apk (20MB)

# Build release APK (signed)
./gradlew :composeApp:assembleRelease

# Output location
# composeApp/build/outputs/apk/release/composeApp-release.apk

# Install on connected device
./gradlew :composeApp:installDebug

# Or use adb directly
adb install composeApp/build/outputs/apk/debug/composeApp-debug.apk
```

**What the Android app does:**
- ✅ Makes HTTP requests to backend (sends/receives JSON)
- ✅ Uses shared Kotlin types (automatic JSON serialization)
- ✅ Works with any backend that speaks JSON

### 3️⃣ Desktop App (JVM - Cross-Platform)

```bash
# Build desktop JAR (works on ANY OS with Java)
./gradlew :composeApp:jvmJar

# Output location
# composeApp/build/libs/composeApp-jvm.jar

# Run desktop app
./gradlew :composeApp:run

# Or run JAR directly
java -jar composeApp/build/libs/composeApp-jvm.jar

# Build installers for distribution
./gradlew :composeApp:packageDistributionForCurrentOS

# Outputs:
# macOS: composeApp/build/compose/binaries/main/dmg/*.dmg
# Windows: composeApp/build/compose/binaries/main/msi/*.msi  
# Linux: composeApp/build/compose/binaries/main/deb/*.deb
```

**What the Desktop app does:**
- ✅ Same as Android - HTTP + JSON communication
- ✅ Runs on Windows, macOS, Linux (Java is cross-platform)
- ✅ Uses shared Kotlin types

### 4️⃣ Web App (JavaScript/WASM)

```bash
# Build web distribution (Kotlin/JS)
./gradlew :composeApp:jsBrowserDistribution

# Output location
# composeApp/build/distributions/

# Run development server
./gradlew :composeApp:jsBrowserDevelopmentRun

# Access at http://localhost:8080
```

**What the Web app does:**
- ✅ Compiles Kotlin to JavaScript
- ✅ Makes fetch/XHR requests to backend (JSON)
- ✅ Same shared types, serialized to JSON in browser

### 5️⃣ iOS App

```bash
# Build iOS framework
./gradlew :shared:linkDebugFrameworkIosArm64

# Open in Xcode
open iosApp/iosApp.xcodeproj

# Build and run in Xcode:
# 1. Select target device/simulator
# 2. Product → Run (Cmd+R)
# 3. Or Product → Archive (for App Store)
```

**What the iOS app does:**
- ✅ Uses shared Kotlin framework (compiled to native code)
- ✅ Makes URLSession requests to backend (JSON)
- ✅ Same data models as all other platforms

---

## 🧪 TESTING

### Run All Tests

```bash
# Run all tests (all platforms)
./gradlew test

# Run specific module tests
./gradlew :server:test        # Backend tests
./gradlew :shared:test         # Shared logic tests
./gradlew :composeApp:test     # App tests
```

### Test the Backend API

```bash
# Start backend
./gradlew :server:run --args="--port=8080" &

# Test health endpoint
curl http://localhost:8080/health

# Test with different content types (server handles all)
curl http://localhost:8080/api/profiles \
  -H "Content-Type: application/json"

# Server can also accept XML if configured
curl http://localhost:8080/api/profiles \
  -H "Content-Type: application/xml"
```

### Test Android App

```bash
# Run Android instrumented tests
./gradlew :composeApp:connectedAndroidTest

# Or use Android Studio:
# Right-click on test → Run
```

### Test Desktop App

```bash
# Run desktop tests
./gradlew :composeApp:jvmTest
```

### Test Web App

```bash
# Run JS tests
./gradlew :composeApp:jsTest
```

### Integration Testing (All Platforms → Server)

```bash
# 1. Start backend
./gradlew :server:run &

# 2. Run each platform and verify they all communicate with server
# Android app → makes request → backend responds
# Desktop app → makes request → backend responds  
# Web app → makes request → backend responds
# iOS app → makes request → backend responds

# All platforms send/receive the SAME JSON format
```

---

## 📦 DEPLOYMENT PER PLATFORM

### 🖥️ Backend Server (Deploy ONCE - All Clients Connect)

**Local Testing:**
```bash
java -jar server/build/libs/server-1.0.0-all.jar
```

**Production Deployment:**

**Option 1: Direct JAR (Simple)**
```bash
# Copy to server
scp server/build/libs/server-1.0.0-all.jar user@server:/opt/bside/

# Run on server
ssh user@server
cd /opt/bside
java -jar server-1.0.0-all.jar

# Or as systemd service (recommended)
sudo systemctl start bside-backend
```

**Option 2: Docker (Portable)**
```bash
# Build Docker image
docker build -t bside-backend -f server/Dockerfile .

# Run locally
docker run -p 8080:8080 bside-backend

# Deploy to server
docker save bside-backend | ssh user@server docker load
ssh user@server 'docker run -d -p 8080:8080 bside-backend'

# Or use docker-compose
scp docker-compose.yml user@server:/opt/bside/
ssh user@server 'cd /opt/bside && docker-compose up -d'
```

**Option 3: Cloud Platform**
```bash
# Deploy to Heroku
heroku deploy

# Deploy to AWS Elastic Beanstalk
eb deploy

# Deploy to Google Cloud Run
gcloud run deploy
```

**Server Environment Variables:**
```bash
# Required
DATABASE_URL=http://localhost:8090  # PocketBase URL
PORT=8080

# Optional
ENVIRONMENT=production
LOG_LEVEL=INFO
```

**Verify Deployment:**
```bash
curl https://api.your-domain.com/health
# Should return: {"status":"healthy","version":"1.0.0"}
```

---

### 📱 Android App

**Development (Testing):**
```bash
# Install on device/emulator
./gradlew :composeApp:installDebug
```

**Production Deployment:**

**Step 1: Build Signed APK**
```bash
# Generate keystore (first time only)
keytool -genkey -v -keystore release.keystore \
  -alias bside -keyalg RSA -keysize 2048 -validity 10000

# Configure in composeApp/build.gradle.kts
# (Already set up in your project)

# Build signed release
./gradlew :composeApp:assembleRelease

# Output: composeApp/build/outputs/apk/release/composeApp-release.apk
```

**Step 2: Upload to Google Play**
```bash
# 1. Go to: https://play.google.com/console
# 2. Create app listing
# 3. Upload APK/AAB
# 4. Submit for review
```

**Or Direct Distribution:**
```bash
# Host APK on your website
scp composeApp-release.apk user@server:/var/www/html/downloads/
# Users download and install (enable "Unknown Sources")
```

**Configure Server URL:**
```kotlin
// In composeApp, point to your production server
const val API_BASE_URL = "https://api.your-domain.com"
```

---

### 🖥️ Desktop App (macOS, Windows, Linux)

**Development (Testing):**
```bash
./gradlew :composeApp:run
```

**Production Deployment:**

**Build Installers:**
```bash
# Build for current platform
./gradlew :composeApp:packageDistributionForCurrentOS

# Build for all platforms (requires platform-specific SDKs)
./gradlew :composeApp:packageDmg        # macOS
./gradlew :composeApp:packageMsi        # Windows
./gradlew :composeApp:packageDeb        # Linux
```

**Distribution:**

**macOS (.dmg):**
```bash
# Output: composeApp/build/compose/binaries/main/dmg/

# Distribute:
# 1. Upload to your website
# 2. Users download and drag to Applications
# 3. Or notarize and distribute via Mac App Store
```

**Windows (.msi):**
```bash
# Output: composeApp/build/compose/binaries/main/msi/

# Distribute:
# 1. Upload to your website
# 2. Users download and run installer
# 3. Or submit to Microsoft Store
```

**Linux (.deb):**
```bash
# Output: composeApp/build/compose/binaries/main/deb/

# Distribute:
# 1. Host in APT repository
# 2. Or provide direct download
# 3. Users: sudo dpkg -i bside.deb
```

**Configure Server URL:**
Same as Android - update API_BASE_URL to production

---

### 🌐 Web App

**Development (Testing):**
```bash
./gradlew :composeApp:jsBrowserDevelopmentRun
# Access at http://localhost:8080
```

**Production Deployment:**

**Step 1: Build Production Bundle**
```bash
./gradlew :composeApp:jsBrowserProductionWebpack

# Output: composeApp/build/distributions/
```

**Step 2: Deploy to Hosting**

**Option 1: Netlify (Easiest)**
```bash
npm install -g netlify-cli
netlify login
cd composeApp/build/distributions
netlify deploy --prod --dir=.
```

**Option 2: Vercel**
```bash
npm install -g vercel
cd composeApp/build/distributions
vercel --prod
```

**Option 3: Self-Hosted (Nginx)**
```bash
# Copy files to server
scp -r composeApp/build/distributions/* user@server:/var/www/html/

# Nginx config
server {
    listen 80;
    server_name app.your-domain.com;
    root /var/www/html;
    index index.html;
    
    location / {
        try_files $uri $uri/ /index.html;
    }
}
```

**Option 4: S3 + CloudFront**
```bash
# Upload to S3
aws s3 sync composeApp/build/distributions s3://your-bucket/

# Configure CloudFront distribution
# Point to S3 bucket
```

**Configure Server URL:**
Update API_BASE_URL in web config to point to production API

---

### 🍎 iOS App

**Development (Testing):**
```bash
open iosApp/iosApp.xcodeproj
# Product → Run (Cmd+R)
```

**Production Deployment:**

**Step 1: Build Archive**
```bash
# In Xcode:
# 1. Select "Any iOS Device" target
# 2. Product → Archive
# 3. Wait for build to complete
```

**Step 2: Distribute**

**App Store:**
```bash
# 1. Window → Organizer
# 2. Select archive
# 3. Distribute App → App Store Connect
# 4. Follow prompts
# 5. Submit for review in App Store Connect
```

**TestFlight (Beta):**
```bash
# Same as above, but choose TestFlight
# Add testers in App Store Connect
# Share invite link
```

**Configure Server URL:**
Update API endpoint in iOS app settings to production

---

## 🏗️ SERVER ARCHITECTURE (Platform Agnostic)

### Your Server is Truly Platform-Agnostic

**How it works:**

```kotlin
// Server receives JSON from ANY platform
@Serializable
data class CreateMatchRequest(
    val userId: String,
    val matchedUserId: String
)

// Ktor route (works with ANY client)
post("/api/matches") {
    val request = call.receive<CreateMatchRequest>()  // Automatic JSON → Kotlin
    // ... business logic ...
    call.respond(matchResult)  // Automatic Kotlin → JSON
}
```

**The server doesn't know or care what platform the request comes from:**

```bash
# Android app sends JSON
POST /api/matches
Content-Type: application/json
{"userId":"123","matchedUserId":"456"}

# Desktop app sends SAME JSON
POST /api/matches
Content-Type: application/json
{"userId":"123","matchedUserId":"456"}

# Web app sends SAME JSON
fetch('/api/matches', {
  method: 'POST',
  headers: {'Content-Type': 'application/json'},
  body: JSON.stringify({userId:"123", matchedUserId:"456"})
})

# iOS app sends SAME JSON
URLSession request with same JSON body

# CLI/Postman/curl - SAME JSON
curl -X POST https://api.your-domain.com/api/matches \
  -H "Content-Type: application/json" \
  -d '{"userId":"123","matchedUserId":"456"}'
```

**All get the SAME JSON response:**
```json
{
  "matchId": "789",
  "userId": "123",
  "matchedUserId": "456",
  "timestamp": "2024-01-15T10:30:00Z",
  "status": "pending"
}
```

### Supported Content Types

**Current (JSON - Default):**
```kotlin
install(ContentNegotiation) {
    json(Json {
        prettyPrint = true
        isLenient = true
        ignoreUnknownKeys = true
    })
}
```

**Easy to Add XML Support:**
```kotlin
install(ContentNegotiation) {
    json()
    xml()  // Add XML serialization
}
```

**Easy to Add Other Formats:**
```kotlin
install(ContentNegotiation) {
    json()
    xml()
    protobuf()  // Google Protocol Buffers
    yaml()      // YAML
    // Any format you want!
}
```

**Client can request preferred format:**
```bash
# Request JSON
curl -H "Accept: application/json" https://api.your-domain.com/api/profiles

# Request XML  
curl -H "Accept: application/xml" https://api.your-domain.com/api/profiles

# Server responds in requested format
```

---

## 🎯 COMPLETE BUILD & DEPLOY WORKFLOW

### Development Workflow

```bash
# 1. Start backend locally
./gradlew :server:run &

# 2. Run Android app (connects to localhost:8080)
./gradlew :composeApp:installDebug

# 3. Run Desktop app
./gradlew :composeApp:run

# 4. Run Web app
./gradlew :composeApp:jsBrowserDevelopmentRun

# All platforms connect to same local backend
# All send/receive JSON
```

### Production Workflow

```bash
# 1. Build everything
./gradlew build

# 2. Deploy backend FIRST
scp server/build/libs/server-1.0.0-all.jar user@server:/opt/bside/
ssh user@server 'sudo systemctl restart bside-backend'

# 3. Update all client apps to point to production API
# (Change API_BASE_URL from localhost to https://api.your-domain.com)

# 4. Deploy each platform:

# Android
./gradlew :composeApp:assembleRelease
# → Upload to Play Store

# Desktop
./gradlew :composeApp:packageDistributionForCurrentOS
# → Host installers on website

# Web
./gradlew :composeApp:jsBrowserProductionWebpack
cd composeApp/build/distributions
netlify deploy --prod

# iOS
open iosApp/iosApp.xcodeproj
# → Build archive → Upload to App Store
```

### CI/CD Workflow (Automated)

```bash
# 1. Push to GitHub
git push origin main

# 2. GitHub Actions automatically:
# - Builds all platforms
# - Runs all tests
# - Creates distribution packages
# - Deploys backend to production
# - Makes artifacts available for download

# 3. Download artifacts from GitHub Actions
# 4. Distribute to app stores/hosting
```

---

## 📊 PLATFORM SUMMARY

| Platform | Build Command | Output | Deploy To | Server Communication |
|----------|--------------|---------|-----------|---------------------|
| **Server** | `./gradlew :server:build` | JAR (27MB) | Any server with Java | N/A - IS the server |
| **Android** | `./gradlew :composeApp:assembleRelease` | APK (20MB) | Google Play Store | HTTP + JSON |
| **Desktop** | `./gradlew :composeApp:packageDistributionForCurrentOS` | DMG/MSI/DEB (116MB) | Direct download | HTTP + JSON |
| **Web** | `./gradlew :composeApp:jsBrowserProductionWebpack` | JS files | Netlify/Vercel/S3 | fetch() + JSON |
| **iOS** | Build in Xcode | IPA | App Store | URLSession + JSON |

---

## ✅ VERIFICATION CHECKLIST

### Before Deployment

- [ ] Backend builds successfully
- [ ] Backend tests pass
- [ ] Backend health endpoint works
- [ ] Android app builds and installs
- [ ] Desktop app builds and runs
- [ ] Web app builds and loads
- [ ] iOS app builds in Xcode
- [ ] All platforms can communicate with local backend
- [ ] Shared types work across all platforms

### After Deployment

- [ ] Production backend is accessible
- [ ] Health check returns 200 OK
- [ ] SSL certificate is valid (HTTPS)
- [ ] Android app connects to production API
- [ ] Desktop app connects to production API
- [ ] Web app connects to production API
- [ ] iOS app connects to production API
- [ ] All platforms send/receive data successfully
- [ ] Database is accessible and populated
- [ ] Monitoring and logs are working

---

## 🚀 QUICK REFERENCE

### Build Commands
```bash
./gradlew build                                    # Build everything
./gradlew :server:build                            # Backend only
./gradlew :composeApp:assembleDebug               # Android debug
./gradlew :composeApp:assembleRelease             # Android release
./gradlew :composeApp:packageDistributionForCurrentOS  # Desktop installer
./gradlew :composeApp:jsBrowserProductionWebpack  # Web production
```

### Run Commands
```bash
./gradlew :server:run                      # Start backend
./gradlew :composeApp:run                  # Run desktop app
./gradlew :composeApp:installDebug         # Install Android app
./gradlew :composeApp:jsBrowserDevelopmentRun  # Run web dev server
```

### Test Commands
```bash
./gradlew test              # All tests
./gradlew :server:test      # Backend tests
./gradlew :shared:test      # Shared logic tests
```

### Deploy Commands
```bash
# Backend
java -jar server/build/libs/server-1.0.0-all.jar

# Or push to GitHub for automatic deployment
git push origin main
```

---

## 🎉 YOU'RE READY!

Your app is built with:
- ✅ Platform-agnostic JSON backend
- ✅ Shared strongly-typed models
- ✅ Builds for all major platforms
- ✅ Simple deployment strategy
- ✅ Automated CI/CD pipeline

**Next steps:**
1. Build everything: `./gradlew build`
2. Test locally: `./build-and-run.sh`
3. Deploy backend: `./production-deploy.sh`
4. Distribute clients to app stores

**The server doesn't care what platform connects - it just speaks JSON!** 🚀
