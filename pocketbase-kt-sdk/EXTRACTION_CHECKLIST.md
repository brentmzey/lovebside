# Extraction Checklist - Making the SDK Standalone

This checklist ensures the SDK can be easily extracted and used as a standalone library.

## ✅ Files Ready for Extraction

### Core SDK Files

- [x] `build.gradle.kts` - With publishing configuration
- [x] `gradle.properties` - With library metadata
- [x] `LICENSE` - MIT License
- [x] `.gitignore` - Configured for standalone repo
- [x] `README.md` - Complete documentation
- [x] `EXAMPLES.md` - Usage examples
- [x] `MIGRATION_GUIDE.md` - Migration documentation
- [x] `PUBLISHING.md` - Publishing guide
- [x] `EXTRACTION_CHECKLIST.md` - This file

### Source Code

- [x] `src/commonMain/kotlin/io/pocketbase/PocketBase.kt`
- [x] `src/commonMain/kotlin/io/pocketbase/models/RecordModels.kt`
- [x] `src/commonMain/kotlin/io/pocketbase/models/RealtimeModels.kt`
- [x] `src/commonMain/kotlin/io/pocketbase/models/ClientResponseError.kt`
- [x] `src/commonMain/kotlin/io/pocketbase/services/BaseService.kt`
- [x] `src/commonMain/kotlin/io/pocketbase/services/RecordService.kt`
- [x] `src/commonMain/kotlin/io/pocketbase/services/RealtimeService.kt`
- [x] `src/commonMain/kotlin/io/pocketbase/stores/AuthStore.kt`
- [x] `src/commonMain/kotlin/io/pocketbase/tools/SSEClient.kt`

## 📋 Extraction Steps

### 1. Create Standalone Directory Structure

```bash
mkdir pocketbase-kt-sdk-standalone
cd pocketbase-kt-sdk-standalone
```

### 2. Copy SDK Module

```bash
# Copy the entire SDK module
cp -r /path/to/bside/pocketbase-kt-sdk/* .

# Copy Gradle wrapper
cp -r /path/to/bside/gradle .
cp /path/to/bside/gradlew .
cp /path/to/bside/gradlew.bat .

# Copy version catalog
mkdir -p gradle
cp /path/to/bside/gradle/libs.versions.toml gradle/
```

### 3. Create Root Build Files

Create `build.gradle.kts` in root:

```kotlin
plugins {
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.androidLibrary) apply false
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}
```

Create `settings.gradle.kts` in root:

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

### 4. Update File Paths

The SDK module should be at root level, so update:

- Move `pocketbase-kt-sdk/build.gradle.kts` to root `build.gradle.kts`
- Move `pocketbase-kt-sdk/src/` to root `src/`
- Remove nested module structure

### 5. Customize Metadata

Update `gradle.properties`:

```properties
# Update these with your information
GROUP=io.pocketbase
VERSION_NAME=0.1.0-SNAPSHOT

POM_URL=https://github.com/your-username/pocketbase-kt-sdk
POM_SCM_URL=https://github.com/your-username/pocketbase-kt-sdk
POM_SCM_CONNECTION=scm:git:git://github.com/your-username/pocketbase-kt-sdk.git
POM_SCM_DEV_CONNECTION=scm:git:ssh://git@github.com/your-username/pocketbase-kt-sdk.git

POM_DEVELOPER_ID=your-username
POM_DEVELOPER_NAME=Your Name
POM_DEVELOPER_URL=https://github.com/your-username
```

### 6. Initialize Git

```bash
git init
git add .
git commit -m "Initial commit: PocketBase Kotlin Multiplatform SDK"
```

### 7. Create GitHub Repository

```bash
# Create repo on GitHub, then:
git remote add origin https://github.com/your-username/pocketbase-kt-sdk.git
git branch -M main
git push -u origin main
```

### 8. Test Local Build

```bash
# Test compilation
./gradlew build

# Test publishing to local Maven
./gradlew publishToMavenLocal

# Verify in ~/.m2/repository/io/pocketbase/pocketbase-kt-sdk/
ls -la ~/.m2/repository/io/pocketbase/pocketbase-kt-sdk/
```

### 9. Test in Another Project

Create a test project:

```kotlin
// build.gradle.kts
repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation("io.pocketbase:pocketbase-kt-sdk:0.1.0-SNAPSHOT")
}
```

```kotlin
// Test.kt
import io.pocketbase.PocketBase

fun main() {
    val pb = PocketBase("https://example.pockethost.io")
    println("PocketBase SDK loaded successfully!")
}
```

## 🔧 Configuration Checklist

### Publishing Configuration

- [x] `maven-publish` plugin added
- [x] `signing` plugin added
- [x] Group ID configured
- [x] Version configured
- [x] POM metadata complete
- [x] License information included
- [x] Developer information included
- [x] SCM information included

### Repository Structure

- [x] Source code in `src/commonMain/kotlin/`
- [x] Tests in `src/commonTest/kotlin/` (when added)
- [x] Platform-specific code properly organized
- [x] No hardcoded paths
- [x] No dependencies on parent project

### Documentation

- [x] README.md with installation instructions
- [x] EXAMPLES.md with usage examples
- [x] PUBLISHING.md with publishing guide
- [x] API documentation (KDoc comments)
- [x] LICENSE file
- [x] CHANGELOG.md (create when releasing)

## 📦 Ready to Use As Dependency

### In Current Project (Before Extraction)

Already configured in `settings.gradle.kts`:

```kotlin
include(":pocketbase-kt-sdk")
```

Use it:

```kotlin
// In shared/build.gradle.kts or any module
dependencies {
    implementation(project(":pocketbase-kt-sdk"))
}
```

### After Extraction (As External Library)

#### Option 1: Maven Local (Testing)

```bash
# In SDK repo
./gradlew publishToMavenLocal

# In consuming project
dependencies {
    implementation("io.pocketbase:pocketbase-kt-sdk:0.1.0-SNAPSHOT")
}
```

#### Option 2: GitHub Packages

```kotlin
repositories {
    maven {
        url = uri("https://maven.pkg.github.com/your-username/pocketbase-kt-sdk")
        credentials {
            username = findProperty("gpr.user") as String? ?: System.getenv("GITHUB_ACTOR")
            password = findProperty("gpr.token") as String? ?: System.getenv("GITHUB_TOKEN")
        }
    }
}

dependencies {
    implementation("io.pocketbase:pocketbase-kt-sdk:0.1.0")
}
```

#### Option 3: Maven Central (Production)

```kotlin
repositories {
    mavenCentral()
}

dependencies {
    implementation("io.pocketbase:pocketbase-kt-sdk:0.1.0")
}
```

## 🚀 Quick Extraction Script

Save this as `extract-sdk.sh`:

```bash
#!/bin/bash

# Configuration
SOURCE_DIR="$(pwd)/pocketbase-kt-sdk"
TARGET_DIR="$HOME/projects/pocketbase-kt-sdk-standalone"

echo "Extracting PocketBase Kotlin SDK..."

# Create target directory
mkdir -p "$TARGET_DIR"
cd "$TARGET_DIR"

# Copy SDK files
cp -r "$SOURCE_DIR"/* .

# Copy Gradle wrapper
cp -r "$(dirname "$SOURCE_DIR")/gradle" .
cp "$(dirname "$SOURCE_DIR")/gradlew" .
cp "$(dirname "$SOURCE_DIR")/gradlew.bat" .

# Initialize Git
git init
git add .
git commit -m "Initial commit: PocketBase Kotlin Multiplatform SDK"

echo "✅ SDK extracted to: $TARGET_DIR"
echo ""
echo "Next steps:"
echo "1. cd $TARGET_DIR"
echo "2. Update gradle.properties with your information"
echo "3. Create GitHub repository"
echo "4. git remote add origin YOUR_REPO_URL"
echo "5. git push -u origin main"
echo "6. ./gradlew publishToMavenLocal (to test locally)"
```

Make it executable:

```bash
chmod +x extract-sdk.sh
```

## ✅ Final Verification

Before considering the SDK "extraction-ready":

- [ ] Builds successfully standalone
- [ ] Publishes to Maven Local successfully
- [ ] Can be imported as dependency in test project
- [ ] All documentation is accurate
- [ ] No references to parent project
- [ ] All dependencies are declared
- [ ] Version catalog works correctly
- [ ] License is appropriate
- [ ] README has correct badge and links

## 📝 Post-Extraction Tasks

After extracting:

1. **Create GitHub Repository**
   - Add description
   - Add topics/tags
   - Enable issues
   - Add README badges

2. **Set Up CI/CD**
   - GitHub Actions for testing
   - Automated publishing workflow
   - Code coverage reporting

3. **Create Release**
   - Tag version (e.g., v0.1.0)
   - Write release notes
   - Publish to Maven Central

4. **Community**
   - Add CONTRIBUTING.md
   - Add CODE_OF_CONDUCT.md
   - Set up issue templates
   - Add discussion forum

5. **Documentation Site**
   - GitHub Pages
   - Dokka API docs
   - Migration guides
   - Video tutorials

## 🎯 Success Criteria

The SDK is successfully extractable and reusable if:

✅ It builds without errors in isolation  
✅ It can be published to a Maven repository  
✅ Another project can import and use it  
✅ All features work identically in both contexts  
✅ Documentation is complete and accurate  
✅ No hardcoded paths or parent dependencies  

---

**Current Status**: ✅ Ready for extraction and use as dependency!

The SDK is fully configured to be:
- Used as a project module (current setup)
- Extracted as a standalone library
- Published to Maven Local, GitHub Packages, or Maven Central
- Imported as a dependency in any Kotlin Multiplatform project
