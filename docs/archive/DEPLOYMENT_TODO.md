# 🚀 Deployment TODO - Actions Needed

**Track deployment preparation tasks here**

---

## ⚠️ Critical - Must Do Before Any Distribution

### Security Setup
- [ ] **Generate production JWT secrets** (CRITICAL!)
  ```bash
  # Generate strong secrets
  openssl rand -base64 64
  # Store securely (use environment variables, not in code!)
  ```
- [ ] **Review and update CORS settings** for production domains
- [ ] **Remove any test/development credentials** from code
- [ ] **Enable HTTPS** for server (required for production)
- [ ] **Review API rate limiting** settings

### Environment Configuration
- [ ] **Set production environment variables**:
  ```bash
  APP_ENV=production
  JWT_SECRET=<your-generated-secret>
  JWT_REFRESH_SECRET=<your-generated-secret>
  POCKETBASE_URL=<your-production-pocketbase-url>
  ALLOWED_ORIGINS=https://yourdomain.com,https://www.yourdomain.com
  ```
- [ ] **Configure production PocketBase** instance
- [ ] **Run database migrations** on production
- [ ] **Backup strategy** in place

---

## 🔨 Server Deployment Tasks

### Infrastructure Setup (Choose One)

**Option A: Basic VPS (DigitalOcean, Linode, etc.)**
- [ ] Create server instance (2GB RAM minimum)
- [ ] Install Java 17+
- [ ] Configure firewall (allow 80, 443)
- [ ] Install SSL certificate (Let's Encrypt)
- [ ] Set up systemd service
- [ ] Configure nginx reverse proxy
- [ ] Set up logging
- [ ] Set up monitoring

**Option B: Docker**
- [ ] Create Dockerfile (see BUILD_AND_DEPLOY_GUIDE.md)
- [ ] Create docker-compose.yml
- [ ] Set up Docker registry (optional)
- [ ] Configure environment variables
- [ ] Set up logging driver
- [ ] Set up health checks
- [ ] Test locally first

**Option C: Cloud Platform (Heroku, DigitalOcean App Platform, AWS)**
- [ ] Create application
- [ ] Configure build settings
- [ ] Set environment variables
- [ ] Configure health checks
- [ ] Set up auto-scaling (optional)
- [ ] Configure logging
- [ ] Set up monitoring

### Server Tasks
- [ ] Build server JAR: `./gradlew :server:shadowJar`
- [ ] Test JAR locally
- [ ] Upload to server
- [ ] Configure systemd/Docker
- [ ] Start server
- [ ] Verify health endpoint
- [ ] Configure backup schedule
- [ ] Set up SSL certificate
- [ ] Configure nginx/reverse proxy
- [ ] Set up monitoring (Datadog, New Relic, etc.)
- [ ] Set up error tracking (Sentry)
- [ ] Configure log rotation

---

## 📱 Android Distribution Tasks

### Pre-Release
- [ ] **Create release keystore** (CRITICAL - backup securely!)
  ```bash
  keytool -genkey -v \
    -keystore bside-release.keystore \
    -alias bside \
    -keyalg RSA \
    -keysize 2048 \
    -validity 10000
  ```
- [ ] **Store keystore securely** (use password manager)
- [ ] **Configure signing** in `composeApp/build.gradle.kts`
- [ ] **Update version code** (increment for each release)
- [ ] **Update version name** (semantic versioning)
- [ ] **Configure ProGuard rules** (if needed)
- [ ] **Update app icon** (adaptive icon)
- [ ] **Update splash screen**

### Google Play Console Setup
- [ ] Create Google Play Developer account ($25 one-time fee)
- [ ] Create application in Play Console
- [ ] Fill out store listing:
  - [ ] App name
  - [ ] Short description (80 chars)
  - [ ] Full description (4000 chars)
  - [ ] Screenshots (min 2, up to 8) - Phone, Tablet
  - [ ] Feature graphic (1024x500)
  - [ ] App icon (512x512)
  - [ ] Privacy policy URL
  - [ ] Category
  - [ ] Content rating questionnaire
  - [ ] Target audience
- [ ] Set up pricing & distribution
- [ ] Set up countries

### Build & Upload
- [ ] Build release AAB: `./gradlew :composeApp:bundleRelease`
- [ ] Test AAB thoroughly
- [ ] Upload to Internal testing track first
- [ ] Test internal build
- [ ] Upload to Beta (optional)
- [ ] Upload to Production
- [ ] Submit for review

---

## 🍎 iOS Distribution Tasks

### Pre-Release
- [ ] **Join Apple Developer Program** ($99/year)
- [ ] **Create App ID** in Apple Developer portal
- [ ] **Configure signing** in Xcode
- [ ] **Update version number** (CFBundleShortVersionString)
- [ ] **Update build number** (CFBundleVersion)
- [ ] **Configure app icon** (all sizes)
- [ ] **Configure launch screen**
- [ ] **Set deployment target** (iOS 14+ recommended)

### App Store Connect Setup
- [ ] Create app in App Store Connect
- [ ] Fill out app information:
  - [ ] Name
  - [ ] Subtitle (30 chars)
  - [ ] Privacy policy URL
  - [ ] Category (Primary, Secondary)
  - [ ] Age rating
- [ ] Create App Store listing:
  - [ ] Description (4000 chars)
  - [ ] Keywords (100 chars)
  - [ ] Support URL
  - [ ] Marketing URL (optional)
  - [ ] Screenshots (6.5", 5.5", iPad Pro 12.9")
  - [ ] App previews (optional video)
  - [ ] Promotional text (170 chars)

### Build & Upload
- [ ] Archive in Xcode: Product → Archive
- [ ] Upload to TestFlight
- [ ] Fill out "What to Test" notes
- [ ] Invite internal testers
- [ ] Test thoroughly
- [ ] Invite external testers (optional)
- [ ] Submit for App Store review
- [ ] Wait for approval (24-48 hours typically)

---

## 🌐 Web Deployment Tasks

### Build
- [ ] Build production bundle: `./gradlew :composeApp:jsBrowserDistribution`
- [ ] Optimize bundle size
- [ ] Configure API endpoint for production
- [ ] Test production build locally
- [ ] Configure PWA manifest (optional)
- [ ] Configure service worker (optional)

### Hosting Setup (Choose One)

**Option A: Netlify**
- [ ] Create Netlify account
- [ ] Connect Git repository
- [ ] Configure build command
- [ ] Configure publish directory
- [ ] Set environment variables
- [ ] Configure custom domain
- [ ] Enable HTTPS

**Option B: Vercel**
- [ ] Create Vercel account
- [ ] Connect Git repository
- [ ] Configure build settings
- [ ] Set environment variables
- [ ] Configure custom domain
- [ ] Enable HTTPS

**Option C: Firebase Hosting**
- [ ] Create Firebase project
- [ ] Install Firebase CLI
- [ ] Initialize hosting
- [ ] Configure firebase.json
- [ ] Set up API rewrites
- [ ] Deploy
- [ ] Configure custom domain

**Option D: AWS S3 + CloudFront**
- [ ] Create S3 bucket
- [ ] Configure bucket for static hosting
- [ ] Create CloudFront distribution
- [ ] Configure SSL certificate
- [ ] Upload build files
- [ ] Configure custom domain

### Post-Deployment
- [ ] Test in production
- [ ] Configure analytics (Google Analytics, etc.)
- [ ] Configure error tracking (Sentry)
- [ ] Set up SEO (meta tags, sitemap)
- [ ] Test on multiple browsers
- [ ] Test on mobile devices

---

## 🖥️ Desktop Distribution Tasks

### macOS
- [ ] Build DMG: `./gradlew :composeApp:packageDmg`
- [ ] Test installation
- [ ] Sign application (for distribution outside App Store)
  ```bash
  codesign --force --deep --sign "Developer ID Application: Your Name" Bside.app
  ```
- [ ] Notarize (required for macOS 10.15+)
  ```bash
  xcrun notarytool submit Bside.dmg --apple-id "email" --password "app-password"
  ```
- [ ] Upload to website or App Store

### Windows
- [ ] Build MSI: `./gradlew :composeApp:packageMsi` (on Windows)
- [ ] Test installation
- [ ] Sign installer (optional but recommended)
- [ ] Upload to website

### Linux
- [ ] Build DEB: `./gradlew :composeApp:packageDeb` (on Linux)
- [ ] Build RPM (optional)
- [ ] Test installation
- [ ] Upload to website or package manager

### Distribution
- [ ] Create download page
- [ ] Add installation instructions
- [ ] Configure auto-update (optional)
- [ ] Test on target operating systems

---

## 📊 Monitoring & Analytics Setup

### Server Monitoring
- [ ] Set up health checks
- [ ] Configure metrics collection (Prometheus, etc.)
- [ ] Set up alerting (PagerDuty, Opsgenie)
- [ ] Configure log aggregation (ELK, Splunk)
- [ ] Set up error tracking (Sentry)
- [ ] Monitor API response times
- [ ] Monitor database performance
- [ ] Monitor server resources (CPU, memory, disk)

### Application Analytics
- [ ] Set up analytics (Google Analytics, Mixpanel, Amplitude)
- [ ] Track key events:
  - [ ] User registration
  - [ ] User login
  - [ ] Profile completion
  - [ ] Match interactions (like/pass)
  - [ ] Messages sent
  - [ ] App opens
  - [ ] Screen views
- [ ] Set up conversion tracking
- [ ] Set up retention analysis
- [ ] Set up crash reporting (Firebase Crashlytics)

---

## 🔐 Security Checklist

### Before Going Live
- [ ] **All secrets in environment variables** (not in code)
- [ ] **HTTPS enabled** everywhere
- [ ] **CORS configured** correctly
- [ ] **Rate limiting** implemented
- [ ] **Input validation** on all endpoints
- [ ] **SQL injection** prevention verified
- [ ] **XSS protection** enabled
- [ ] **CSRF protection** enabled
- [ ] **Authentication** required on protected endpoints
- [ ] **Authorization** checks on all operations
- [ ] **Passwords hashed** (bcrypt/argon2)
- [ ] **JWT tokens** expire correctly
- [ ] **Refresh tokens** rotate
- [ ] **Database backups** configured
- [ ] **Security headers** configured
- [ ] **Dependencies** up to date
- [ ] **Vulnerability scan** completed

---

## 📝 Documentation Tasks

### User Documentation
- [ ] Create user guide
- [ ] Create FAQ
- [ ] Create troubleshooting guide
- [ ] Create video tutorials (optional)
- [ ] Privacy policy (REQUIRED!)
- [ ] Terms of service (REQUIRED!)
- [ ] Community guidelines

### Developer Documentation
- [ ] API documentation
- [ ] Architecture documentation
- [ ] Setup guide
- [ ] Contribution guide
- [ ] Code of conduct

### Legal Documents (REQUIRED!)
- [ ] **Privacy Policy** (CRITICAL - required by app stores)
- [ ] **Terms of Service** (CRITICAL - required by app stores)
- [ ] Cookie policy (if applicable)
- [ ] GDPR compliance (if EU users)
- [ ] CCPA compliance (if California users)
- [ ] Age verification process (if required)

---

## 🎯 Priority Order

### Phase 1: Critical (Do First)
1. [ ] Generate production secrets
2. [ ] Set up production server
3. [ ] Configure production database
4. [ ] Create privacy policy & terms
5. [ ] Enable HTTPS

### Phase 2: Distribution Setup
1. [ ] Android keystore & Play Console
2. [ ] iOS Apple Developer & App Store Connect
3. [ ] Web hosting setup
4. [ ] Desktop build and distribution

### Phase 3: Monitoring
1. [ ] Server monitoring
2. [ ] Error tracking
3. [ ] Analytics
4. [ ] Crash reporting

### Phase 4: Polish
1. [ ] User documentation
2. [ ] Marketing materials
3. [ ] Support channels
4. [ ] Social media presence

---

## 📞 Resources & Links

### Required Accounts
- [ ] Apple Developer ($99/year): https://developer.apple.com
- [ ] Google Play Developer ($25 one-time): https://play.google.com/console
- [ ] Domain registrar (for custom domain)
- [ ] Hosting provider
- [ ] PocketBase/Database hosting

### Useful Services
- SSL Certificates: Let's Encrypt (free), Cloudflare
- Monitoring: Datadog, New Relic, AppDynamics
- Error Tracking: Sentry, Bugsnag, Rollbar
- Analytics: Google Analytics, Mixpanel, Amplitude
- Crash Reporting: Firebase Crashlytics

### Tools
- Fastlane (automate iOS/Android deployment)
- GitHub Actions (CI/CD)
- Docker (containerization)
- Kubernetes (orchestration)

---

## ✅ Ready to Deploy When...

- [ ] All critical tasks complete
- [ ] All tests passing
- [ ] Security review done
- [ ] Legal documents ready
- [ ] Monitoring configured
- [ ] Backups configured
- [ ] Support plan in place
- [ ] Rollback plan prepared
- [ ] Team trained

---

**Use this TODO to track your deployment preparation progress!**

**See BUILD_AND_DEPLOY_GUIDE.md for detailed instructions**
