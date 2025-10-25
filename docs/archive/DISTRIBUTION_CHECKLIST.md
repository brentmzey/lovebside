# ✅ Distribution Checklist - Release Process

**Use this checklist for every release to ensure quality and completeness**

---

## 📅 Pre-Release (1-2 weeks before)

### Code Quality
- [ ] All features complete for this release
- [ ] All tests passing (`./gradlew test`)
- [ ] Code review completed
- [ ] No critical bugs
- [ ] Performance tested
- [ ] Security audit completed
- [ ] Database migrations tested and documented

### Documentation
- [ ] CHANGELOG.md updated with new features
- [ ] API documentation updated
- [ ] User documentation updated
- [ ] README.md reflects current state
- [ ] Known issues documented

### Version Management
- [ ] Version number decided (semantic versioning)
- [ ] Version updated in all places:
  - [ ] `composeApp/build.gradle.kts` (versionCode, versionName)
  - [ ] `iosApp/iosApp/Info.plist` (CFBundleVersion, CFBundleShortVersionString)
  - [ ] `server/build.gradle.kts` (version)
  - [ ] `gradle.properties` (project.version)

### Assets & Content
- [ ] App icons updated (all sizes)
- [ ] Splash screens updated
- [ ] Marketing screenshots prepared
- [ ] App Store / Play Store descriptions written
- [ ] Privacy policy updated
- [ ] Terms of service updated

---

## 🏗️ Build Phase

### Server Build
- [ ] Clean build: `./gradlew clean :server:shadowJar`
- [ ] JAR created: `server/build/libs/server-all.jar`
- [ ] JAR tested locally
- [ ] Environment variables documented
- [ ] Database migrations ready

### Android Build
- [ ] Keystore configured and secured
- [ ] Release build: `./gradlew :composeApp:assembleRelease`
- [ ] AAB created: `composeApp/build/outputs/bundle/release/composeApp-release.aab`
- [ ] APK created (if needed): `composeApp/build/outputs/apk/release/composeApp-release.apk`
- [ ] Version code incremented
- [ ] ProGuard tested
- [ ] Tested on multiple Android versions (min SDK to latest)
- [ ] Tested on different screen sizes

### iOS Build
- [ ] Xcode project opens without errors
- [ ] Signing configured correctly
- [ ] Archive created: Product → Archive
- [ ] Tested on iOS simulator
- [ ] Tested on physical device
- [ ] Version/Build number incremented
- [ ] Crash reporting configured

### Web Build
- [ ] Production build: `./gradlew :composeApp:jsBrowserDistribution`
- [ ] Bundle size checked and optimized
- [ ] Tested on Chrome
- [ ] Tested on Firefox
- [ ] Tested on Safari
- [ ] Mobile responsive tested
- [ ] PWA manifest configured
- [ ] Service worker configured (if applicable)

### Desktop Build
- [ ] macOS DMG: `./gradlew :composeApp:packageDmg`
- [ ] Windows MSI: `./gradlew :composeApp:packageMsi` (on Windows)
- [ ] Linux DEB: `./gradlew :composeApp:packageDeb` (on Linux)
- [ ] Tested on target OS
- [ ] Installer tested
- [ ] Auto-update tested (if configured)

---

## 🧪 Testing Phase

### Functional Testing
- [ ] Authentication works (login, register, logout)
- [ ] Profile creation and editing works
- [ ] Values selection works
- [ ] Proust questionnaire works
- [ ] Match discovery works
- [ ] Like/Pass functionality works
- [ ] All API endpoints tested
- [ ] Error handling works correctly

### Platform-Specific Testing

**Android**:
- [ ] Tested on Android 7+ (API 24+)
- [ ] Tested on different manufacturers (Samsung, Google, etc.)
- [ ] Tested on tablets
- [ ] Tested offline mode
- [ ] Tested deep links
- [ ] Tested push notifications

**iOS**:
- [ ] Tested on iOS 14+
- [ ] Tested on iPhone (various sizes)
- [ ] Tested on iPad
- [ ] Tested dark mode
- [ ] Tested offline mode
- [ ] Tested push notifications

**Web**:
- [ ] Tested on desktop browsers (Chrome, Firefox, Safari, Edge)
- [ ] Tested on mobile browsers
- [ ] Tested different screen sizes
- [ ] Tested slow network (3G)
- [ ] Tested offline (if PWA)

**Desktop**:
- [ ] Tested on macOS
- [ ] Tested on Windows
- [ ] Tested on Linux
- [ ] Tested window resizing
- [ ] Tested menu items
- [ ] Tested keyboard shortcuts

### Performance Testing
- [ ] App launches in < 3 seconds
- [ ] API responses < 200ms (p95)
- [ ] No memory leaks
- [ ] Battery usage acceptable
- [ ] Network usage reasonable
- [ ] Bundle size acceptable

### Security Testing
- [ ] All API endpoints require authentication
- [ ] Tokens expire correctly
- [ ] Refresh tokens work
- [ ] Passwords hashed securely
- [ ] Input validation working
- [ ] XSS protection verified
- [ ] CSRF protection verified
- [ ] SQL injection not possible
- [ ] Secrets not in code
- [ ] HTTPS enforced in production

---

## 🚀 Deployment Phase

### Server Deployment

**Staging Environment**:
- [ ] Deploy to staging
- [ ] Run database migrations
- [ ] Verify health endpoint
- [ ] Test API endpoints
- [ ] Check logs for errors
- [ ] Monitor for 24 hours

**Production Environment**:
- [ ] Backup database
- [ ] Deploy server
- [ ] Run migrations
- [ ] Verify health endpoint
- [ ] Test critical flows
- [ ] Monitor error rates
- [ ] Check performance metrics

### Android Distribution

**Internal Testing**:
- [ ] Upload AAB to Play Console (Internal testing track)
- [ ] Invite internal testers
- [ ] Wait for feedback
- [ ] Fix critical issues

**Beta Testing** (Optional):
- [ ] Upload to Beta track
- [ ] Invite beta testers
- [ ] Collect feedback
- [ ] Fix issues

**Production**:
- [ ] Upload final AAB to Production track
- [ ] Fill out/update store listing:
  - [ ] Title
  - [ ] Short description
  - [ ] Full description
  - [ ] Screenshots (phone, tablet)
  - [ ] Feature graphic
  - [ ] App icon
  - [ ] Privacy policy link
  - [ ] Target age group
  - [ ] Content rating
  - [ ] Categories
- [ ] Set pricing (free)
- [ ] Select countries
- [ ] Submit for review
- [ ] Wait for approval (typically 1-3 days)
- [ ] Release when approved

### iOS Distribution

**TestFlight**:
- [ ] Upload build to TestFlight
- [ ] Fill out "What to Test" notes
- [ ] Invite internal testers
- [ ] Wait for feedback
- [ ] Fix critical issues
- [ ] Invite external testers (optional)
- [ ] Collect feedback

**App Store**:
- [ ] Upload final build
- [ ] Fill out/update App Store listing:
  - [ ] Name
  - [ ] Subtitle
  - [ ] Description
  - [ ] Keywords
  - [ ] Screenshots (6.5", 5.5", iPad)
  - [ ] App icon
  - [ ] Privacy policy URL
  - [ ] Support URL
  - [ ] Marketing URL
  - [ ] Age rating
  - [ ] Categories (Primary, Secondary)
- [ ] Set pricing (free)
- [ ] Select territories
- [ ] Submit for review
- [ ] Wait for review (typically 24-48 hours)
- [ ] Release when approved

### Web Deployment
- [ ] Build production bundle
- [ ] Upload to hosting (Netlify/Vercel/Firebase)
- [ ] Configure custom domain
- [ ] Enable HTTPS
- [ ] Configure CORS
- [ ] Test in production
- [ ] Configure CDN (if applicable)
- [ ] Setup analytics

### Desktop Distribution
- [ ] Upload installers to website
- [ ] Create download page
- [ ] Add installation instructions
- [ ] Test download and install
- [ ] Configure auto-update server (if applicable)
- [ ] Announce release

---

## 📢 Post-Release

### Monitoring (First 24 hours)
- [ ] Monitor error rates (target: < 1%)
- [ ] Monitor crash rates (target: < 0.1%)
- [ ] Monitor API response times (target: < 200ms p95)
- [ ] Monitor server CPU/memory usage
- [ ] Monitor user signups
- [ ] Monitor user retention
- [ ] Check user reviews/feedback

### Communication
- [ ] Announce on social media
- [ ] Send email to existing users
- [ ] Update website
- [ ] Post to Product Hunt (if major release)
- [ ] Update press kit
- [ ] Notify stakeholders

### Documentation
- [ ] Update user guides
- [ ] Create release notes
- [ ] Update API documentation
- [ ] Update screenshots/videos
- [ ] Tag release in Git
- [ ] Create GitHub release

### Support
- [ ] Monitor support channels
- [ ] Respond to user feedback
- [ ] Track reported bugs
- [ ] Prepare hotfix if needed
- [ ] Update FAQ

---

## 🐛 Hotfix Process

If critical bug found after release:

1. **Assess Severity**:
   - [ ] Critical (affects all users, data loss, security)
   - [ ] High (affects many users, major feature broken)
   - [ ] Medium (affects some users, workaround exists)
   - [ ] Low (minor issue, can wait for next release)

2. **For Critical/High**:
   - [ ] Create hotfix branch from release tag
   - [ ] Fix bug
   - [ ] Test thoroughly
   - [ ] Increment patch version (e.g., 1.0.0 → 1.0.1)
   - [ ] Build all platforms
   - [ ] Deploy server immediately
   - [ ] Submit mobile updates (expedited review)
   - [ ] Deploy web immediately
   - [ ] Notify users

3. **For Medium/Low**:
   - [ ] Add to backlog
   - [ ] Fix in next regular release

---

## 📊 Release Metrics

Track these metrics for each release:

### Technical Metrics
- [ ] Build time
- [ ] Bundle sizes (Android APK, iOS IPA, Web bundle)
- [ ] Test coverage percentage
- [ ] Number of bugs found in testing
- [ ] Number of bugs found in production

### User Metrics
- [ ] Day 1 downloads/installs
- [ ] Week 1 downloads/installs
- [ ] Crash rate (first 7 days)
- [ ] 1-day retention rate
- [ ] 7-day retention rate
- [ ] Average session length
- [ ] User reviews/ratings

### Business Metrics
- [ ] New user signups
- [ ] Active users (DAU/MAU)
- [ ] Match rate
- [ ] User engagement
- [ ] Conversion rate (if applicable)

---

## 📋 Quick Checklist Summary

**Pre-Release**:
- [ ] Code complete and tested
- [ ] Documentation updated
- [ ] Versions bumped
- [ ] Assets prepared

**Build**:
- [ ] Server JAR built
- [ ] Android AAB/APK built
- [ ] iOS archive created
- [ ] Web bundle built
- [ ] Desktop installers built

**Test**:
- [ ] All platforms tested
- [ ] Performance verified
- [ ] Security verified
- [ ] User flows tested

**Deploy**:
- [ ] Staging deployed and tested
- [ ] Production deployed
- [ ] Mobile apps submitted
- [ ] Web deployed
- [ ] Desktop distributed

**Post-Release**:
- [ ] Monitoring active
- [ ] Users notified
- [ ] Documentation updated
- [ ] Support ready

---

## 🎯 Release Types

### Patch Release (1.0.X)
- Bug fixes only
- No new features
- Fast approval expected
- Can be released anytime

### Minor Release (1.X.0)
- New features
- Bug fixes
- Backward compatible
- Release every 2-4 weeks

### Major Release (X.0.0)
- Breaking changes
- Major new features
- Significant UI changes
- Release every 3-6 months
- Requires migration guide

---

## 📞 Emergency Contacts

Keep these handy during release:

- **Server Issues**: DevOps team / Hosting provider support
- **App Store Issues**: Apple Developer Support
- **Play Store Issues**: Google Play Console support
- **Database Issues**: Database admin / PocketBase support
- **Security Issues**: Security team lead
- **PR Issues**: Marketing team lead

---

## 🎉 Release Celebration

After successful release:
- [ ] Team celebration
- [ ] Retrospective meeting
- [ ] Document lessons learned
- [ ] Plan next release
- [ ] Take a break! 🍾

---

**Use this checklist for every release to ensure quality and smooth distribution!**

**Next**: See `BUILD_AND_DEPLOY_GUIDE.md` for technical details
