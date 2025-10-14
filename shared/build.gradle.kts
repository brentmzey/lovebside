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
    
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

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

    // WasmJS target (experimental) - Disabled temporarily due to Koin compatibility
    // @OptIn(ExperimentalWasmDsl::class)
    // wasmJs {
    //     browser()
    // }

    sourceSets {
        // Common source set - shared across all platforms
        commonMain.dependencies {
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
            
            // Compose dependencies for UI components
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.materialIconsExtended)
        }
        
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }

        // Android source set
        androidMain.dependencies {
            implementation(libs.ktor.client.cio)
            implementation(libs.koin.core)
            implementation(libs.koin.android)
            implementation(libs.koin.compose)
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
        }
    }
}

android {
    namespace = "love.bside.app.shared"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
}
