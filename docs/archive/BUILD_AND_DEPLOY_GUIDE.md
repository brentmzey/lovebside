# 🚀 Build, Run, Deploy & Distribute Guide

**Complete guide for all platforms: Android, iOS, Web, Desktop, Server**

---

## 📋 Table of Contents

1. [Prerequisites](#prerequisites)
2. [Building](#building)
3. [Running Locally](#running-locally)
4. [Testing](#testing)
5. [Deploying](#deploying)
6. [Distribution](#distribution)
7. [Troubleshooting](#troubleshooting)

---

## Prerequisites

### Required Software

**All Platforms**:
- JDK 17+ ([Install](https://adoptium.net/))
- Git

**Android**:
- Android Studio Arctic Fox or newer
- Android SDK 24+
- Android device or emulator

**iOS**:
- macOS with Xcode 14+
- iOS Simulator or iOS device
- Apple Developer account (for distribution)

**Web**:
- Modern browser (Chrome, Firefox, Safari)
- Node.js 16+ (optional, for better builds)

**Desktop**:
- JDK 17+ (already required)

**Server**:
- JDK 17+
- 2GB+ RAM
- Linux/macOS/Windows

### Environment Setup

```bash
# Clone repository
git clone <your-repo-url>
cd bside

# Verify Java version
java -version  # Should be 17+

# Verify Gradle
./gradlew --version
```

---

## Building

### Build All Platforms

```bash
# Clean build everything
./gradlew clean build

# Expected output:
# BUILD SUCCESSFUL in 30s
```

### Build Individual Platforms

#### Android
```bash
# Debug build
./gradlew :composeApp:assembleDebug

# Release build (requires signing)
./gradlew :composeApp:assembleRelease

# Output: composeApp/build/outputs/apk/debug/composeApp-debug.apk
```

#### iOS
```bash
# Compile Kotlin framework
./gradlew :composeApp:linkDebugFrameworkIosArm64

# Then open in Xcode
open iosApp/iosApp.xcodeproj

# Build in Xcode: Product → Build (Cmd+B)
```

#### Web
```bash
# JavaScript build
./gradlew :composeApp:jsBrowserDistribution

# Output: composeApp/build/dist/js/productionExecutable/

# WebAssembly build (experimental)
./gradlew :composeApp:wasmJsBrowserDistribution

# Output: composeApp/build/dist/wasmJs/productionExecutable/
```

#### Desktop
```bash
# JVM build
./gradlew :composeApp:packageDistributionForCurrentOS

# Output varies by OS:
# macOS: composeApp/build/compose/binaries/main/dmg/*.dmg
# Windows: composeApp/build/compose/binaries/main/msi/*.msi
# Linux: composeApp/build/compose/binaries/main/deb/*.deb
```

#### Server
```bash
# Build executable JAR
./gradlew :server:shadowJar

# Output: server/build/libs/server-all.jar
```

---

## Running Locally

### Development Mode

#### 1. Start Server (Required for all clients)

```bash
# Terminal 1: Start server
./gradlew :server:run

# Wait for:
# "Application started in X.X seconds"
# Server running at: http://localhost:8080
```

**Verify server is running**:
```bash
curl http://localhost:8080/health
# Expected: {"status":"healthy","version":"1.0.0",...}
```

#### 2. Run Android

**Option A: From Command Line**
```bash
# Install on connected device/emulator
./gradlew :composeApp:installDebug

# Launch manually from device
```

**Option B: From Android Studio**
```bash
# Open project in Android Studio
# Select "composeApp" configuration
# Click Run (green triangle)
```

**Configure Server URL**:
```kotlin
// For Android emulator, use special IP
// File: shared/src/androidMain/kotlin/love/bside/app/core/AppConfig.android.kt
val baseUrl = "http://10.0.2.2:8080/api/v1"  // Emulator
// OR
val baseUrl = "http://192.168.1.X:8080/api/v1"  // Physical device (use your IP)
```

#### 3. Run iOS

```bash
# Option A: From Xcode
open iosApp/iosApp.xcodeproj
# Select simulator/device
# Click Run (Cmd+R)

# Option B: Command line (simulator only)
xcodebuild -project iosApp/iosApp.xcodeproj \
  -scheme iosApp \
  -destination 'platform=iOS Simulator,name=iPhone 14' \
  run
```

**Configure Server URL**:
```kotlin
// For iOS simulator, use localhost
// File: shared/src/iosMain/kotlin/love/bside/app/core/AppConfig.ios.kt
val baseUrl = "http://localhost:8080/api/v1"  // Simulator
// OR
val baseUrl = "http://192.168.1.X:8080/api/v1"  // Physical device
```

#### 4. Run Web

```bash
# JavaScript (development with hot reload)
./gradlew :composeApp:jsBrowserDevelopmentRun

# Opens browser at: http://localhost:8080
# (Note: Different port than server, configured in Gradle)

# WebAssembly (experimental)
./gradlew :composeApp:wasmJsBrowserDevelopmentRun
```

**Configure Server URL**:
```kotlin
// For web, use same origin or configure CORS
// File: shared/src/jsMain/kotlin/love/bside/app/core/AppConfig.js.kt
val baseUrl = "/api/v1"  // Same server (requires proxy)
// OR
val baseUrl = "http://localhost:8080/api/v1"  // Different server (requires CORS)
```

#### 5. Run Desktop

```bash
# Run directly
./gradlew :composeApp:run

# Opens desktop window
```

**Configure Server URL**:
```kotlin
// For desktop, use localhost
// File: shared/src/jvmMain/kotlin/love/bside/app/core/AppConfig.jvm.kt
val baseUrl = "http://localhost:8080/api/v1"
```

---

## Testing

### Run All Tests

```bash
# All tests
./gradlew test

# Specific module tests
./gradlew :shared:jvmTest
./gradlew :server:test
```

### Integration Tests (Require Running Server)

```bash
# Terminal 1: Start server
./gradlew :server:run

# Terminal 2: Run integration tests
./gradlew :shared:jvmTest

# Platform-specific integration tests
./gradlew :shared:jvmTest --tests "*IntegrationTest"
./gradlew :shared:androidInstrumentedTest  # Requires device/emulator
```

### Manual API Testing

```bash
# Test registration
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "Test1234!",
    "passwordConfirm": "Test1234!",
    "firstName": "Test",
    "lastName": "User",
    "birthDate": "1990-01-01",
    "seeking": "BOTH"
  }'

# Test login
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"Test1234!"}'

# Save token from response, then test authenticated endpoint
TOKEN="your_token_here"
curl http://localhost:8080/api/v1/users/me \
  -H "Authorization: Bearer $TOKEN"
```

---

## Deploying

### Server Deployment

#### Option 1: Standalone JAR (Recommended)

**Build**:
```bash
./gradlew :server:shadowJar
```

**Deploy**:
```bash
# Copy JAR to server
scp server/build/libs/server-all.jar user@your-server:/app/

# On server
java -jar /app/server-all.jar
```

**Environment Variables**:
```bash
export APP_ENV=production
export JWT_SECRET="your-super-secure-secret-here"
export JWT_REFRESH_SECRET="another-secure-secret"
export POCKETBASE_URL="https://your-pocketbase.pockethost.io"
export ALLOWED_ORIGINS="https://yourdomain.com,https://www.yourdomain.com"
export SERVER_PORT=8080
```

**Systemd Service** (Linux):
```bash
# Create service file
sudo nano /etc/systemd/system/bside-server.service
```

```ini
[Unit]
Description=B-Side Server
After=network.target

[Service]
Type=simple
User=bside
WorkingDirectory=/app
Environment="APP_ENV=production"
Environment="JWT_SECRET=your-secret"
Environment="POCKETBASE_URL=https://your-db.pockethost.io"
ExecStart=/usr/bin/java -jar /app/server-all.jar
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
```

```bash
# Enable and start
sudo systemctl enable bside-server
sudo systemctl start bside-server
sudo systemctl status bside-server
```

#### Option 2: Docker

**Create Dockerfile**:
```dockerfile
# File: Dockerfile
FROM openjdk:17-slim

WORKDIR /app

# Copy JAR
COPY server/build/libs/server-all.jar /app/server.jar

# Environment variables (override at runtime)
ENV APP_ENV=production
ENV SERVER_PORT=8080

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s \
  CMD curl -f http://localhost:8080/health || exit 1

# Run
CMD ["java", "-jar", "server.jar"]
```

**Build and run**:
```bash
# Build Docker image
docker build -t bside-server .

# Run container
docker run -d \
  --name bside-server \
  -p 8080:8080 \
  -e JWT_SECRET="your-secret" \
  -e POCKETBASE_URL="https://your-db.pockethost.io" \
  bside-server
```

**Docker Compose**:
```yaml
# File: docker-compose.yml
version: '3.8'

services:
  server:
    build: .
    ports:
      - "8080:8080"
    environment:
      APP_ENV: production
      JWT_SECRET: ${JWT_SECRET}
      POCKETBASE_URL: ${POCKETBASE_URL}
      ALLOWED_ORIGINS: ${ALLOWED_ORIGINS}
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/health"]
      interval: 30s
      timeout: 3s
      retries: 3
```

```bash
# Run with docker-compose
docker-compose up -d
```

#### Option 3: Cloud Platforms

**DigitalOcean App Platform**:
```yaml
# File: .do/app.yaml
name: bside-server
services:
- name: server
  github:
    repo: your-username/bside
    branch: main
    deploy_on_push: true
  build_command: ./gradlew :server:shadowJar
  run_command: java -jar server/build/libs/server-all.jar
  envs:
  - key: APP_ENV
    value: production
  - key: JWT_SECRET
    type: SECRET
  - key: POCKETBASE_URL
    type: SECRET
  http_port: 8080
```

**Heroku**:
```bash
# Create Procfile
echo "web: java -jar server/build/libs/server-all.jar" > Procfile

# Deploy
heroku create bside-server
heroku config:set JWT_SECRET="your-secret"
heroku config:set POCKETBASE_URL="https://your-db.pockethost.io"
git push heroku main
```

**AWS Elastic Beanstalk**:
```bash
# Install EB CLI
pip install awsebcli

# Initialize
eb init -p docker bside-server

# Create environment
eb create bside-production

# Set environment variables
eb setenv JWT_SECRET="your-secret" POCKETBASE_URL="https://your-db.pockethost.io"

# Deploy
eb deploy
```

### Web Deployment

#### Build for Production

```bash
# JavaScript build
./gradlew :composeApp:jsBrowserDistribution

# Output: composeApp/build/dist/js/productionExecutable/
```

#### Deploy to Static Hosting

**Netlify**:
```bash
# Install Netlify CLI
npm install -g netlify-cli

# Deploy
cd composeApp/build/dist/js/productionExecutable
netlify deploy --prod
```

**Vercel**:
```bash
# Install Vercel CLI
npm install -g vercel

# Deploy
cd composeApp/build/dist/js/productionExecutable
vercel --prod
```

**Firebase Hosting**:
```bash
# Install Firebase CLI
npm install -g firebase-tools

# Initialize
firebase init hosting

# Configure firebase.json
{
  "hosting": {
    "public": "composeApp/build/dist/js/productionExecutable",
    "rewrites": [
      {
        "source": "/api/**",
        "destination": "https://your-server.com/api/"
      }
    ]
  }
}

# Deploy
firebase deploy
```

**AWS S3 + CloudFront**:
```bash
# Upload to S3
aws s3 sync composeApp/build/dist/js/productionExecutable/ \
  s3://your-bucket/ \
  --delete

# Invalidate CloudFront cache
aws cloudfront create-invalidation \
  --distribution-id YOUR_DIST_ID \
  --paths "/*"
```

---

## Distribution

### Android Distribution

#### Google Play Store

**1. Prepare Release**:

```bash
# Generate keystore (first time only)
keytool -genkey -v \
  -keystore bside-release.keystore \
  -alias bside \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000

# Store securely! Don't commit to git!
```

**2. Configure Signing**:

```kotlin
// File: composeApp/build.gradle.kts
android {
    signingConfigs {
        create("release") {
            storeFile = file("../bside-release.keystore")
            storePassword = System.getenv("KEYSTORE_PASSWORD")
            keyAlias = "bside"
            keyPassword = System.getenv("KEY_PASSWORD")
        }
    }
    
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}
```

**3. Build Release APK/AAB**:

```bash
# Set passwords
export KEYSTORE_PASSWORD="your-keystore-password"
export KEY_PASSWORD="your-key-password"

# Build AAB (required for Play Store)
./gradlew :composeApp:bundleRelease

# Output: composeApp/build/outputs/bundle/release/composeApp-release.aab

# Or build APK (for direct distribution)
./gradlew :composeApp:assembleRelease

# Output: composeApp/build/outputs/apk/release/composeApp-release.apk
```

**4. Upload to Play Console**:

1. Go to [Google Play Console](https://play.google.com/console)
2. Create app or select existing
3. Go to Release → Production
4. Upload `composeApp-release.aab`
5. Fill out store listing
6. Submit for review

**5. Update Version**:

```kotlin
// File: composeApp/build.gradle.kts
android {
    defaultConfig {
        versionCode = 2  // Increment for each release
        versionName = "1.0.1"
    }
}
```

#### Alternative Distribution (Direct APK)

```bash
# Build release APK
./gradlew :composeApp:assembleRelease

# Distribute via:
# - Your website
# - Email
# - Cloud storage
# - Internal app distribution tools
```

### iOS Distribution

#### TestFlight (Beta Testing)

**1. Configure Signing**:

```bash
# Open in Xcode
open iosApp/iosApp.xcodeproj

# In Xcode:
# 1. Select project → Signing & Capabilities
# 2. Set Team to your Apple Developer team
# 3. Set Bundle Identifier (e.g., com.yourcompany.bside)
# 4. Enable "Automatically manage signing"
```

**2. Archive**:

```bash
# In Xcode: Product → Archive
# Or via command line:
xcodebuild archive \
  -project iosApp/iosApp.xcodeproj \
  -scheme iosApp \
  -archivePath build/iosApp.xcarchive
```

**3. Upload to TestFlight**:

```bash
# In Xcode: Window → Organizer
# Select archive → Distribute App → TestFlight
# Follow prompts

# Or via command line:
xcodebuild -exportArchive \
  -archivePath build/iosApp.xcarchive \
  -exportPath build \
  -exportOptionsPlist exportOptions.plist

# Upload with Transporter or altool
xcrun altool --upload-app \
  --type ios \
  --file build/iosApp.ipa \
  --username "your-apple-id" \
  --password "app-specific-password"
```

**4. Invite Testers**:

1. Go to [App Store Connect](https://appstoreconnect.apple.com)
2. Select app → TestFlight
3. Add internal/external testers
4. Distribute build

#### App Store

**1. Prepare App Store Listing**:

- Screenshots (6.5", 5.5")
- App icon (1024x1024)
- Description
- Keywords
- Privacy policy URL
- Support URL

**2. Submit for Review**:

```bash
# Build release version
# In Xcode: Product → Archive

# Upload to App Store (not TestFlight)
# In Organizer: Distribute App → App Store Connect
# Select "Release automatically" or "Manual release"
```

**3. Wait for Review** (1-3 days typically)

### Desktop Distribution

#### macOS

**Build DMG**:
```bash
./gradlew :composeApp:packageDmg

# Output: composeApp/build/compose/binaries/main/dmg/Bside-1.0.0.dmg
```

**Notarize** (for distribution outside App Store):
```bash
# Sign
codesign --force --deep --sign "Developer ID Application: Your Name" Bside.app

# Notarize
xcrun notarytool submit Bside-1.0.0.dmg \
  --apple-id "your-apple-id" \
  --password "app-specific-password" \
  --team-id "TEAM_ID"

# Staple
xcrun stapler staple Bside-1.0.0.dmg
```

#### Windows

**Build MSI**:
```bash
./gradlew :composeApp:packageMsi

# Output: composeApp/build/compose/binaries/main/msi/Bside-1.0.0.msi
```

**Sign** (optional but recommended):
```bash
signtool sign /f certificate.pfx /p password Bside-1.0.0.msi
```

#### Linux

**Build DEB**:
```bash
./gradlew :composeApp:packageDeb

# Output: composeApp/build/compose/binaries/main/deb/bside_1.0.0_amd64.deb
```

**Build RPM**:
```bash
./gradlew :composeApp:packageRpm

# Output: composeApp/build/compose/binaries/main/rpm/bside-1.0.0.x86_64.rpm
```

---

## Troubleshooting

### Build Issues

**Gradle sync fails**:
```bash
./gradlew clean
rm -rf ~/.gradle/caches
./gradlew --refresh-dependencies
```

**Android build fails**:
```bash
# In Android Studio: File → Invalidate Caches → Invalidate and Restart
# Or:
rm -rf .gradle build
./gradlew clean build
```

**iOS build fails**:
```bash
# Clean Xcode
cd iosApp
rm -rf build DerivedData
xcodebuild clean
```

### Runtime Issues

**Server won't start - Port in use**:
```bash
# Find process on port 8080
lsof -i :8080

# Kill it
kill -9 <PID>
```

**Android can't connect to server**:
```bash
# For emulator, use: 10.0.2.2:8080
# For device, use your computer's IP: 192.168.1.X:8080

# Check connectivity
adb shell curl http://10.0.2.2:8080/health
```

**iOS can't connect to server**:
```bash
# For simulator, use: localhost:8080
# For device, use your computer's IP

# Check Info.plist allows local networking
# Add ATS exception if needed
```

**Web CORS errors**:
```bash
# Server needs to allow web origin
# File: server/src/main/kotlin/love/bside/server/plugins/HTTP.kt
install(CORS) {
    allowHost("localhost:8080")
    allowHost("yourdomain.com")
    allowCredentials = true
}
```

---

## 📋 Deployment Checklist

### Pre-Deployment

- [ ] All tests pass
- [ ] Code reviewed
- [ ] Version numbers updated
- [ ] Environment variables configured
- [ ] Secrets secured (not in code)
- [ ] Database migrations ready
- [ ] API documentation updated

### Server Deployment

- [ ] Build successful
- [ ] Environment variables set
- [ ] Database accessible
- [ ] Health check responds
- [ ] HTTPS configured
- [ ] CORS configured
- [ ] Monitoring setup
- [ ] Logging configured
- [ ] Backup strategy in place

### Mobile Deployment

**Android**:
- [ ] Release keystore secured
- [ ] Version code incremented
- [ ] ProGuard rules tested
- [ ] APK/AAB built
- [ ] Tested on multiple devices
- [ ] Store listing ready
- [ ] Screenshots prepared

**iOS**:
- [ ] Signing configured
- [ ] Bundle ID correct
- [ ] Version incremented
- [ ] Tested on devices
- [ ] Screenshots prepared
- [ ] Privacy policy ready
- [ ] App Store listing complete

### Web Deployment

- [ ] Production build created
- [ ] Assets optimized
- [ ] API endpoints configured
- [ ] Analytics integrated
- [ ] SEO metadata added
- [ ] Favicon included
- [ ] HTTPS configured

### Desktop Deployment

- [ ] Installer built
- [ ] Signed (if applicable)
- [ ] Tested on target OS
- [ ] Auto-update configured
- [ ] Installation instructions ready

---

## 🎯 Quick Reference

### Development
```bash
./gradlew :server:run              # Start server
./gradlew :composeApp:run          # Run desktop
./gradlew :composeApp:installDebug # Install Android
```

### Building
```bash
./gradlew build                          # All
./gradlew :composeApp:assembleRelease   # Android
./gradlew :server:shadowJar             # Server
./gradlew :composeApp:jsBrowserDistribution # Web
```

### Testing
```bash
./gradlew test                    # All tests
./gradlew :shared:jvmTest        # Integration tests
```

### Deployment
```bash
docker-compose up -d             # Server (Docker)
firebase deploy                   # Web (Firebase)
./gradlew :composeApp:packageDmg # Desktop (macOS)
```

---

**You now have complete build, run, deploy, and distribute instructions!** 🚀

**Next**: See `DISTRIBUTION_CHECKLIST.md` for detailed release process
