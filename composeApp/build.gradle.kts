import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JvmVendorSpec
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import java.util.Properties
import java.io.FileInputStream
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
}

kotlin {
    androidTarget()

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
            export(libs.koin.core)
        }
    }

    jvm()

    js {
        browser()
        binaries.executable()
    }

    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
        vendor.set(JvmVendorSpec.ADOPTIUM)
    }


    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.materialIconsExtended)
                implementation(compose.ui)
                implementation(compose.components.resources)
                implementation(compose.components.uiToolingPreview)
                implementation(libs.androidx.lifecycle.viewmodelCompose)
                implementation(libs.androidx.lifecycle.runtimeCompose)
                implementation(libs.decompose)
                implementation(libs.decompose.extensions.compose)
                implementation(libs.kotlinx.coroutinesCore)
                implementation(libs.kotlinx.datetime)
                implementation(libs.kotlinx.serialization.json)
                implementation(projects.shared)
                implementation(libs.koin.compose.multiplatform)
                implementation(libs.koin.compose.viewmodel)
            }
        }

        val nonWasmCommonMain by creating {
            dependsOn(commonMain)
            dependencies {
                implementation(projects.shared)
                implementation(libs.pocketbase.sdk)
                api(libs.koin.core)
                implementation(libs.multiplatform.settings)
            }
        }

        val androidMain by getting {
            dependsOn(nonWasmCommonMain)
            dependencies {
                implementation(compose.preview)
                implementation(libs.androidx.activity.compose)
                implementation(libs.koin.android)
                implementation(libs.koin.compose)
                implementation(libs.multiplatform.settings)
                implementation(libs.androidx.biometric)
            }
        }

        val appleMain by creating {
            dependsOn(nonWasmCommonMain)
            dependencies {
                implementation(libs.koin.core)
                implementation(libs.multiplatform.settings)
            }
        }

        val iosArm64Main by getting { dependsOn(appleMain) }
        val iosSimulatorArm64Main by getting { dependsOn(appleMain) }

        val jvmMain by getting {
            dependsOn(nonWasmCommonMain)
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(libs.kotlinx.coroutinesSwing)
                implementation(libs.koin.core)
                implementation(libs.multiplatform.settings)
            }
        }

        val jsMain by getting {
            dependsOn(nonWasmCommonMain)
            dependencies {
                implementation(libs.koin.core)
                implementation(libs.multiplatform.settings)
            }
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

android {
    namespace = "love.bside.app"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "love.bside.app"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"

        val localProperties = Properties()
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            localProperties.load(FileInputStream(localPropertiesFile))
        }
        val mapsApiKey = localProperties.getProperty("MAPS_API_KEY") ?: ""
        manifestPlaceholders["MAPS_API_KEY"] = mapsApiKey
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        val java21 = JavaVersion.toVersion("21")
        sourceCompatibility = java21
        targetCompatibility = java21
    }
}

dependencies {
    implementation(project(":shared"))
    debugImplementation(compose.uiTooling)
}

compose.desktop {
    application {
        mainClass = "love.bside.app.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "love.bside.app"
            packageVersion = "1.0.0"

            val iconDir = project.file("src/jvmMain/resources/icons")
            macOS {
                iconFile.set(iconDir.resolve("bside_logo.icns"))
            }
            windows {
                iconFile.set(iconDir.resolve("bside_logo.ico"))
            }
            linux {
                iconFile.set(iconDir.resolve("bside_logo_512.png"))
            }
        }
    }
}
