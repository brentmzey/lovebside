# Kotlin Multiplatform Hierarchy Template - Best Practices

## Overview

The B-Side project now uses the **Default Kotlin Hierarchy Template** which automatically configures source set hierarchies following Kotlin Multiplatform best practices.

## What Changed

### Before (Manual Configuration)
```kotlin
val iosArm64Main by getting
val iosSimulatorArm64Main by getting
val iosMain by creating {
    dependsOn(commonMain)
    iosArm64Main.dependsOn(this)
    iosSimulatorArm64Main.dependsOn(this)
}
```

### After (Default Hierarchy Template)
```kotlin
applyDefaultHierarchyTemplate()

// Source sets are automatically created
iosMain.dependencies {
    // Dependencies here
}
```

## Source Set Hierarchy

The default hierarchy template creates this structure:

```
                    commonMain
                        │
        ┌───────────────┼───────────────┬───────────┬─────────┐
        │               │               │           │         │
    androidMain     iosMain         jvmMain     jsMain   wasmJsMain
                        │
            ┌───────────┴───────────┐
            │                       │
        iosArm64Main        iosSimulatorArm64Main
```

### Intermediate Source Sets

The template automatically creates these intermediate source sets:

- **iosMain**: Shared between all iOS targets
- **appleMain**: Shared between all Apple platforms (iOS, macOS, tvOS, watchOS)
- **nativeMain**: Shared between all native platforms
- **jsMain**: Shared between JS and WasmJS (if configured)

## Benefits

### 1. Automatic Hierarchy
The template automatically creates and connects source sets based on your declared targets.

### 2. Better Code Sharing
Intermediate source sets like `iosMain` allow sharing iOS-specific code without duplication.

### 3. Consistency
Follows Kotlin's official recommendations and conventions.

### 4. Less Boilerplate
No need to manually create and connect source sets with `dependsOn()`.

### 5. Future-Proof
When new platforms are added, the hierarchy adjusts automatically.

## Platform-Specific Code

### Expect/Actual Mechanism

Use expect/actual for platform-specific implementations:

**commonMain**:
```kotlin
// In commonMain
expect fun getPlatformName(): String
```

**iosMain** (shared across all iOS targets):
```kotlin
// In iosMain - shared by iosArm64 and iosSimulatorArm64
actual fun getPlatformName(): String = "iOS"
```

**androidMain**:
```kotlin
// In androidMain
actual fun getPlatformName(): String = "Android"
```

### Platform Checks

Use compiler flags for conditional compilation:

```kotlin
// In commonMain
fun logPlatform() {
    val platform = when {
        // Automatically available from hierarchy
        isIos() -> "iOS"
        isAndroid() -> "Android"
        isJvm() -> "JVM/Desktop"
        isJs() -> "JavaScript"
        isWasmJs() -> "WebAssembly"
        else -> "Unknown"
    }
    println("Running on: $platform")
}
```

## Source Set Configuration Best Practices

### ✅ DO: Use the Default Template

```kotlin
kotlin {
    applyDefaultHierarchyTemplate()
    
    // Declare targets
    androidTarget()
    iosArm64()
    iosSimulatorArm64()
    iosX64()
    jvm()
    
    sourceSets {
        // Use generated source sets
        commonMain.dependencies { }
        iosMain.dependencies { }
    }
}
```

### ❌ DON'T: Manually Create Source Sets

```kotlin
// DON'T DO THIS
val iosMain by creating {
    dependsOn(commonMain)
}
```

### ✅ DO: Use Kotlin DSL Accessors

```kotlin
sourceSets {
    commonMain.dependencies {
        implementation(libs.kotlinx.coroutines)
    }
}
```

### ❌ DON'T: Use `by getting`

```kotlin
// DON'T DO THIS
val commonMain by getting {
    dependencies { }
}
```

## Dependency Management

### Common Dependencies (All Platforms)

Place in `commonMain`:
```kotlin
commonMain.dependencies {
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.datetime)
    implementation(libs.ktor.client.core)
}
```

### Platform-Specific Dependencies

Place in platform source sets:
```kotlin
androidMain.dependencies {
    implementation(libs.koin.android)
}

iosMain.dependencies {
    implementation(libs.ktor.client.darwin)
}

jvmMain.dependencies {
    implementation(libs.ktor.client.cio)
}
```

### Engine Selection by Platform

| Platform | Recommended Ktor Engine |
|----------|------------------------|
| Android | `ktor-client-cio` or `ktor-client-okhttp` |
| iOS | `ktor-client-darwin` |
| JVM/Desktop | `ktor-client-cio` or `ktor-client-java` |
| JS/WasmJS | `ktor-client-js` |

## Testing Configuration

### Common Tests

```kotlin
commonTest.dependencies {
    implementation(libs.kotlin.test)
    implementation(libs.kotlinx.coroutines.test)
}
```

### Platform-Specific Tests

```kotlin
androidUnitTest.dependencies {
    implementation(libs.mockk)
    implementation(libs.robolectric)
}

jvmTest.dependencies {
    implementation(libs.mockk)
}

iosTest.dependencies {
    // iOS-specific test dependencies
}
```

## Gradle Properties

### Recommended Settings

Add to `gradle.properties`:

```properties
# Kotlin Multiplatform
kotlin.code.style=official
kotlin.mpp.stability.nowarn=true

# Enable hierarchy template (default in modern KMP)
kotlin.mpp.applyDefaultHierarchyTemplate=true

# Gradle optimization
org.gradle.jvmargs=-Xmx4096M
org.gradle.configuration-cache=true
org.gradle.caching=true
org.gradle.parallel=true

# Android
android.useAndroidX=true
android.nonTransitiveRClass=true

# Compose Multiplatform
org.jetbrains.compose.experimental.jscanvas.enabled=true
org.jetbrains.compose.experimental.wasm.enabled=true
```

## Directory Structure

Your project should follow this structure:

```
shared/
├── build.gradle.kts
└── src/
    ├── commonMain/
    │   └── kotlin/
    │       └── love/bside/app/
    ├── commonTest/
    │   └── kotlin/
    ├── androidMain/
    │   └── kotlin/
    │       └── love/bside/app/
    ├── iosMain/                    # Auto-created by template
    │   └── kotlin/
    │       └── love/bside/app/
    ├── iosArm64Main/               # Optional: iOS ARM64-specific
    ├── iosSimulatorArm64Main/      # Optional: Simulator-specific
    ├── jvmMain/
    │   └── kotlin/
    │       └── love/bside/app/
    ├── jsMain/
    │   └── kotlin/
    │       └── love/bside/app/
    └── wasmJsMain/
        └── kotlin/
            └── love/bside/app/
```

## Migration Guide

If you have existing code in `iosArm64Main` or `iosSimulatorArm64Main`, move it to `iosMain`:

1. **Create iosMain directory**:
   ```bash
   mkdir -p shared/src/iosMain/kotlin/love/bside/app
   ```

2. **Move shared iOS code**:
   ```bash
   # Move files that are the same in both iOS variants
   mv shared/src/iosArm64Main/kotlin/love/bside/app/PlatformFile.kt \
      shared/src/iosMain/kotlin/love/bside/app/
   ```

3. **Keep target-specific code**:
   Only leave code in `iosArm64Main` if it's truly ARM64-specific (rare).

## Common Patterns

### Dependency Injection with Koin

**commonMain**:
```kotlin
val commonModule = module {
    single { ApiClient.create() }
    single { PocketBaseClient(get()) }
}
```

**androidMain**:
```kotlin
val androidModule = module {
    single { AndroidSpecificService(androidContext()) }
}

fun initKoin(appDeclaration: KoinAppDeclaration) {
    startKoin {
        appDeclaration()
        modules(commonModule, androidModule)
    }
}
```

**iosMain**:
```kotlin
val iosModule = module {
    single { IosSpecificService() }
}

fun initKoin() {
    startKoin {
        modules(commonModule, iosModule)
    }
}
```

### HTTP Client Configuration

**commonMain**:
```kotlin
expect fun createHttpClient(): HttpClient

class ApiClient(private val client: HttpClient) {
    companion object {
        fun create() = ApiClient(createHttpClient())
    }
}
```

**androidMain**:
```kotlin
actual fun createHttpClient() = HttpClient(CIO) {
    install(ContentNegotiation) {
        json()
    }
}
```

**iosMain**:
```kotlin
actual fun createHttpClient() = HttpClient(Darwin) {
    install(ContentNegotiation) {
        json()
    }
}
```

## Troubleshooting

### Warning: "Default Kotlin Hierarchy Template Not Applied"

**Cause**: Manual `dependsOn()` calls conflict with the template.

**Solution**: Remove all manual `dependsOn()` calls and use `applyDefaultHierarchyTemplate()`.

### Build Error: "Source set not found"

**Cause**: Trying to access a source set that doesn't match your declared targets.

**Solution**: Only reference source sets for targets you've declared:
```kotlin
// If you declared iosArm64()
iosArm64Main // ✅ OK

// If you didn't declare iosX64()
iosX64Main // ❌ Error
```

### Dependency Resolution Issues

**Cause**: Platform-specific dependency in wrong source set.

**Solution**: Check dependency compatibility:
```kotlin
// ✅ Correct
iosMain.dependencies {
    implementation(libs.ktor.client.darwin) // iOS only
}

// ❌ Wrong
commonMain.dependencies {
    implementation(libs.ktor.client.darwin) // Not available on all platforms
}
```

## Resources

- [Kotlin Multiplatform Hierarchy Template](https://kotlinlang.org/docs/multiplatform-hierarchy.html)
- [Multiplatform Project Structure](https://kotlinlang.org/docs/multiplatform-discover-project.html)
- [Expect and Actual Declarations](https://kotlinlang.org/docs/multiplatform-expect-actual.html)
- [Ktor Client for KMP](https://ktor.io/docs/http-client-multiplatform.html)

## Summary

The Default Hierarchy Template:
- ✅ Reduces boilerplate
- ✅ Follows Kotlin conventions
- ✅ Enables better code sharing
- ✅ Makes the build configuration cleaner
- ✅ Is the recommended approach for all KMP projects

**Always use `applyDefaultHierarchyTemplate()` in modern KMP projects!**

---

**Last Updated**: October 2024  
**Kotlin Version**: 2.0+  
**Status**: ✅ Production Ready
