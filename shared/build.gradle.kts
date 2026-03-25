import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JvmVendorSpec
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    // Apply default hierarchy template for proper source set configuration
    applyDefaultHierarchyTemplate()
    
    androidTarget()

    // iOS targets
    iosArm64()
    iosSimulatorArm64()
    iosX64()

    // JVM target for desktop
    jvm()

    // JS target for web
    js {
        browser()
    }

    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
        vendor.set(JvmVendorSpec.ADOPTIUM)
    }

    targets.all {
        compilations.all {
            compilerOptions.configure {
                freeCompilerArgs.add("-Xexpect-actual-classes")
            }
        }
    }

    // WasmJS target (experimental) - Disabled temporarily due to Koin compatibility
    // @OptIn(ExperimentalWasmDsl::class)
    // wasmJs {
    //     browser()
    // }

    sourceSets {
        // Common source set - shared across all platforms
        commonMain.dependencies {
            implementation(projects.bsideApi)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.client.auth)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.ktor.client.logging)
            implementation(libs.kotlinx.coroutinesCore)
            implementation(libs.multiplatform.settings)
            implementation(libs.decompose)
            implementation(libs.decompose.extensions.compose)
            implementation(libs.essenty.lifecycle)
            implementation(libs.essenty.lifecycle.coroutines)
            implementation(libs.koin.core)
            implementation(libs.koin.compose.multiplatform)
            implementation(libs.pocketbase.sdk)
            implementation(libs.arrow.core)
            implementation(libs.arrow.fx.coroutines)
            implementation(libs.uuid)
            
            // Compose dependencies for UI components
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.materialIconsExtended)
            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor)
        }
        
        val commonTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.kotlinx.coroutinesCore)
                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.ktor.client.mock)
                implementation(libs.kotest.property)
            }
        }

        // Android source set
        androidMain.dependencies {
            implementation(libs.ktor.client.cio)
            implementation(libs.koin.core)
            implementation(libs.koin.android)
            implementation(libs.koin.compose)
            implementation(libs.androidx.security.crypto)
            implementation(libs.maps.compose)
            implementation(libs.google.play.services.maps)
            implementation(libs.google.play.services.location)
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.10.2")
            implementation(libs.brotli4j)
        }
        
        // iOS source set - automatically created by hierarchy template
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
            implementation(libs.koin.core)
        }

        // JVM/Desktop source set
        jvmMain.dependencies {
            implementation(libs.ktor.client.cio)
            implementation(libs.koin.core)
            implementation(libs.brotli4j)
            implementation(libs.brotli4j.native.osx.aarch64)
            implementation(libs.brotli4j.native.linux.x64)
            
            // AWS SDK for S3 and CloudFront (JVM only - not available for iOS/JS yet)
            implementation(libs.aws.s3)
            implementation(libs.aws.cloudfront)
            implementation(libs.aws.core)
            implementation(libs.aws.config)
        }

        // JavaScript source set
        jsMain.dependencies {
            implementation(libs.ktor.client.js)
            implementation(libs.koin.core)
        }

        // WebAssembly source set - Disabled temporarily
        // wasmJsMain.dependencies {
        //     implementation(libs.ktor.client.js)
        //     // Note: Koin doesn't support WasmJS yet - using manual DI
        // }
        
        // Test source sets
        androidUnitTest.dependencies {
            implementation(libs.mockk)
        }
        
        jvmTest.dependencies {
            implementation(libs.mockk)
            implementation(libs.ktor.client.cio)
            implementation(libs.brotli4j)
            implementation(libs.brotli4j.native.osx.aarch64)
            implementation(libs.brotli4j.native.linux.x64)
        }
    }
}

android {
    namespace = "love.bside.app.shared"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
}

tasks.withType<AbstractTestTask> {
    testLogging {
        showStandardStreams = true
        events("passed", "failed", "skipped", "standardOut", "standardError")
    }
    // Force tests to run if we want to see fresh logs
    outputs.upToDateWhen { false }
}

// Ensure standard Test tasks also log
tasks.withType<Test> {
    testLogging {
        showStandardStreams = true
        events("passed", "failed", "skipped", "standardOut", "standardError")
    }
}
