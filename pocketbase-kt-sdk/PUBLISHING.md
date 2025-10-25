# Publishing Guide for PocketBase Kotlin SDK

This guide explains how to extract the SDK as a standalone library and publish it for others to use.

## Table of Contents

1. [Extracting as Standalone Repository](#extracting-as-standalone-repository)
2. [Publishing to Maven Local](#publishing-to-maven-local)
3. [Publishing to Maven Central](#publishing-to-maven-central)
4. [Publishing to GitHub Packages](#publishing-to-github-packages)
5. [Using the Published Library](#using-the-published-library)

---

## Extracting as Standalone Repository

### Step 1: Copy the SDK to a New Directory

```bash
# Create a new directory for the standalone SDK
mkdir pocketbase-kt-sdk-standalone
cd pocketbase-kt-sdk-standalone

# Copy the SDK files
cp -r ../bside/pocketbase-kt-sdk/* .

# Copy necessary Gradle wrapper files
cp -r ../bside/gradle .
cp ../bside/gradlew .
cp ../bside/gradlew.bat .
```

### Step 2: Create Root build.gradle.kts

Create a root `build.gradle.kts` in the standalone directory:

```kotlin
plugins {
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.androidLibrary) apply false
}
```

### Step 3: Create Root settings.gradle.kts

Create `settings.gradle.kts`:

```kotlin
rootProject.name = "pocketbase-kt-sdk"

pluginManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}
```

### Step 4: Copy gradle/libs.versions.toml

Copy the version catalog:

```bash
cp ../bside/gradle/libs.versions.toml gradle/
```

### Step 5: Initialize Git Repository

```bash
git init
git add .
git commit -m "Initial commit: PocketBase Kotlin Multiplatform SDK"

# Create repository on GitHub/GitLab
git remote add origin https://github.com/yourusername/pocketbase-kt-sdk.git
git push -u origin main
```

---

## Publishing to Maven Local

Perfect for testing the library locally before publishing:

```bash
# Build and publish to local Maven repository (~/.m2/repository)
./gradlew publishToMavenLocal

# Or publish to a custom local directory
./gradlew publish -PmavenRepo=file:///path/to/local/repo
```

### Using from Maven Local

In the consuming project's `build.gradle.kts`:

```kotlin
repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation("io.pocketbase:pocketbase-kt-sdk:0.1.0-SNAPSHOT")
}
```

---

## Publishing to Maven Central

Maven Central is the recommended way to distribute public libraries.

### Prerequisites

1. **Create Sonatype Account**
   - Sign up at https://issues.sonatype.org/
   - Create a JIRA ticket to claim your groupId (e.g., `io.pocketbase`)
   - Follow: https://central.sonatype.org/register/central-portal/

2. **Generate GPG Key**

```bash
# Generate GPG key pair
gpg --gen-key

# List keys and note the key ID
gpg --list-keys

# Export public key
gpg --keyserver keyserver.ubuntu.com --send-keys YOUR_KEY_ID

# Export private key for signing (keep this secure!)
gpg --export-secret-keys YOUR_KEY_ID > secring.gpg
```

3. **Configure Credentials**

Create `~/.gradle/gradle.properties`:

```properties
ossrhUsername=your-sonatype-username
ossrhPassword=your-sonatype-password

signing.keyId=YOUR_KEY_ID
signing.password=your-gpg-password
signing.secretKeyRingFile=/path/to/secring.gpg

# OR use in-memory keys (preferred for CI/CD)
signingKey=paste-your-base64-encoded-key-here
signingPassword=your-gpg-password
```

### Publishing Steps

1. **Update version in gradle.properties**

```properties
VERSION_NAME=0.1.0  # Remove -SNAPSHOT for releases
```

2. **Enable signing in build.gradle.kts**

Uncomment the signing configuration:

```kotlin
signing {
    sign(publishing.publications)
}
```

3. **Enable Maven Central repository**

Uncomment the Maven Central repository in `build.gradle.kts`.

4. **Publish**

```bash
# Build and publish
./gradlew clean build
./gradlew publish

# If using Sonatype Nexus (older method)
./gradlew publishToSonatype closeAndReleaseSonatypeStagingRepository
```

5. **Wait for sync**

After release, it takes 15-30 minutes to sync to Maven Central.

---

## Publishing to GitHub Packages

Easier alternative to Maven Central for GitHub-hosted projects.

### Step 1: Configure GitHub Token

Create a GitHub Personal Access Token with `write:packages` permission.

### Step 2: Add GitHub Packages Repository

In `build.gradle.kts`, add:

```kotlin
publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/yourusername/pocketbase-kt-sdk")
            credentials {
                username = project.findProperty("gpr.user") as String? ?: System.getenv("GITHUB_ACTOR")
                password = project.findProperty("gpr.token") as String? ?: System.getenv("GITHUB_TOKEN")
            }
        }
    }
}
```

### Step 3: Publish

```bash
# Set credentials
export GITHUB_ACTOR=your-username
export GITHUB_TOKEN=your-token

# Publish
./gradlew publish
```

### Using from GitHub Packages

In the consuming project:

```kotlin
repositories {
    maven {
        url = uri("https://maven.pkg.github.com/yourusername/pocketbase-kt-sdk")
        credentials {
            username = project.findProperty("gpr.user") as String? ?: System.getenv("GITHUB_ACTOR")
            password = project.findProperty("gpr.token") as String? ?: System.getenv("GITHUB_TOKEN")
        }
    }
}

dependencies {
    implementation("io.pocketbase:pocketbase-kt-sdk:0.1.0")
}
```

---

## Using the Published Library

Once published, consumers can use the library like this:

### Gradle Kotlin DSL

```kotlin
// In build.gradle.kts
dependencies {
    implementation("io.pocketbase:pocketbase-kt-sdk:0.1.0")
}
```

### Gradle Groovy DSL

```groovy
// In build.gradle
dependencies {
    implementation 'io.pocketbase:pocketbase-kt-sdk:0.1.0'
}
```

### Version Catalog (libs.versions.toml)

```toml
[versions]
pocketbase-sdk = "0.1.0"

[libraries]
pocketbase-sdk = { module = "io.pocketbase:pocketbase-kt-sdk", version.ref = "pocketbase-sdk" }
```

Then use:

```kotlin
dependencies {
    implementation(libs.pocketbase.sdk)
}
```

---

## CI/CD Setup

### GitHub Actions Example

Create `.github/workflows/publish.yml`:

```yaml
name: Publish to Maven Central

on:
  release:
    types: [created]

jobs:
  publish:
    runs-on: macos-latest
    
    steps:
      - uses: actions/checkout@v3
      
      - name: Set up JDK 11
        uses: actions/setup-java@v3
        with:
          java-version: '11'
          distribution: 'temurin'
      
      - name: Setup Gradle
        uses: gradle/gradle-build-action@v2
      
      - name: Publish to Maven Central
        env:
          OSSRH_USERNAME: ${{ secrets.OSSRH_USERNAME }}
          OSSRH_PASSWORD: ${{ secrets.OSSRH_PASSWORD }}
          SIGNING_KEY: ${{ secrets.SIGNING_KEY }}
          SIGNING_PASSWORD: ${{ secrets.SIGNING_PASSWORD }}
        run: |
          ./gradlew publish --no-daemon
```

---

## Versioning Strategy

Follow semantic versioning (SemVer):

- **0.1.0-SNAPSHOT** - Development versions
- **0.1.0** - Initial public release
- **0.2.0** - Minor updates, new features (backward compatible)
- **1.0.0** - Stable API, production-ready
- **1.1.0** - New features (backward compatible)
- **2.0.0** - Breaking changes

### Updating Version

1. Update `VERSION_NAME` in `gradle.properties`
2. Commit and tag:
   ```bash
   git commit -am "Release version 0.1.0"
   git tag v0.1.0
   git push origin main --tags
   ```

---

## Checklist Before Publishing

- [ ] All tests pass
- [ ] Documentation is complete and accurate
- [ ] Version number is updated
- [ ] CHANGELOG.md is updated
- [ ] README.md has correct installation instructions
- [ ] Examples work with the new version
- [ ] Breaking changes are documented
- [ ] GPG keys are configured
- [ ] Sonatype account is set up (for Maven Central)
- [ ] License file is present
- [ ] POM metadata is correct

---

## Troubleshooting

### "Publication not found" Error

Ensure all Kotlin targets are properly configured and built.

### Signing Failures

Check that:
- GPG key is properly exported
- Key password is correct
- Key hasn't expired

### Maven Central Sync Delays

- Initial sync can take 30+ minutes
- Check status at https://s01.oss.sonatype.org/

### GitHub Packages Authentication

Ensure your token has `write:packages` permission and isn't expired.

---

## Resources

- [Maven Central Publishing Guide](https://central.sonatype.org/publish/publish-guide/)
- [Kotlin Multiplatform Publishing](https://kotlinlang.org/docs/multiplatform-publish-lib.html)
- [Gradle Maven Publish Plugin](https://docs.gradle.org/current/userguide/publishing_maven.html)
- [Semantic Versioning](https://semver.org/)
