# 🚀 B-Side Distribution Guide

This guide details how to build release-ready artifacts for Android (Play Store) and iOS (App Store).

---

## 🤖 Android (Google Play)

### 1. Generate Upload Keystore

If you haven't already, generate a signing key:

```bash
keytool -genkey -v -keystore bside-release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias bside-key
```

* **Keep this file safe!** If you lose it, you cannot update your app on the Play Store.
* Move `bside-release.jks` to the `composeApp/` directory (but **do not commit it**).

### 2. Configure Signing

Create/Edit using `local.properties` in your root project to avoid committing secrets:

```properties
storeFile=../composeApp/bside-release.jks
storePassword=YOUR_STORE_PASSWORD
keyAlias=bside-key
keyPassword=YOUR_KEY_PASSWORD
```

**Note:** You will need to update `composeApp/build.gradle.kts` to read these values if you want fully automated signing. Currently, the project is set up for debug builds.

### 3. Build Release Artifacts

Run the following command to build the App Bundle (.aab) for the Play Store:

```bash
./gradlew :composeApp:bundleRelease
```

* **Output:** `composeApp/build/outputs/bundle/release/composeApp-release.aab`
* **Testing:** To build a standard APK for side-loading -> `./gradlew :composeApp:assembleRelease`

---

## 🍎 iOS (App Store / TestFlight)

### 1. Prerequisites

* An Apple Developer Account ($99/year).
* A registered App ID in Apple Developer Portal (`love.bside.app`).
* Signing Certificate and Provisioning Profile installed.

### 2. Prepare Xcode

1. Open `iosApp/iosApp.xcodeproj` in Xcode.
2. Select the **iosApp** target.
3. Go to **Signing & Capabilities**.
4. Select your **Team**.
5. Ensure **Bundle Identifier** matches your App ID.

### 3. Archive & Distribute

1. Select **Any iOS Device (arm64)** as the build target.
2. Menu: **Product** > **Archive**.
3. Once finished, the **Organizer** window opens.
4. Click **Distribute App** -> **App Store Connect** -> **Upload**.
5. Follow the wizard to upload to TestFlight.

---

## 🖥️ Desktop (MacOS, Windows, Linux)

### Build Native Installers

```bash
./gradlew :composeApp:packageReleaseDistributionForCurrentOS
```

* **Output:** `composeApp/build/compose/binaries/main/release`
* Contains `.dmg`, `.msi`, or `.deb` depending on your OS.
