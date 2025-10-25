plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.androidLibrary)
    `maven-publish`
    signing
}

group = project.findProperty("GROUP") as String? ?: "io.pocketbase"
version = project.findProperty("VERSION_NAME") as String? ?: "0.1.0-SNAPSHOT"

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        }
    }
    
    jvm()
    
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    
    js(IR) {
        browser()
        nodejs()
    }
    
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.kotlinx.coroutinesCore)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kotlinx.datetime)
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.content.negotiation)
                implementation(libs.ktor.serialization.kotlinx.json)
                implementation(libs.ktor.client.logging)
            }
        }
        
        val commonTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.kotlinx.coroutinesCore)
            }
        }
        
        val androidMain by getting {
            dependencies {
                implementation(libs.ktor.client.cio)
            }
        }
        
        val jvmMain by getting {
            dependencies {
                implementation(libs.ktor.client.cio)
            }
        }
        
        val iosX64Main by getting
        val iosArm64Main by getting
        val iosSimulatorArm64Main by getting
        
        val jsMain by getting {
            dependencies {
                implementation(libs.ktor.client.js)
            }
        }
    }
}

android {
    namespace = "io.pocketbase.sdk"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

publishing {
    publications {
        withType<MavenPublication> {
            groupId = project.group.toString()
            artifactId = project.findProperty("POM_ARTIFACT_ID") as String? ?: "pocketbase-kt-sdk"
            version = project.version.toString()
            
            pom {
                name.set(project.findProperty("POM_NAME") as String? ?: "PocketBase Kotlin Multiplatform SDK")
                description.set(project.findProperty("POM_DESCRIPTION") as String? ?: "A pure Kotlin Multiplatform SDK for PocketBase")
                url.set(project.findProperty("POM_URL") as String? ?: "https://github.com/yourusername/pocketbase-kt-sdk")
                
                licenses {
                    license {
                        name.set(project.findProperty("POM_LICENCE_NAME") as String? ?: "MIT License")
                        url.set(project.findProperty("POM_LICENCE_URL") as String? ?: "https://opensource.org/licenses/MIT")
                        distribution.set(project.findProperty("POM_LICENCE_DIST") as String? ?: "repo")
                    }
                }
                
                developers {
                    developer {
                        id.set(project.findProperty("POM_DEVELOPER_ID") as String? ?: "developer")
                        name.set(project.findProperty("POM_DEVELOPER_NAME") as String? ?: "Developer")
                        url.set(project.findProperty("POM_DEVELOPER_URL") as String? ?: "")
                    }
                }
                
                scm {
                    url.set(project.findProperty("POM_SCM_URL") as String? ?: "")
                    connection.set(project.findProperty("POM_SCM_CONNECTION") as String? ?: "")
                    developerConnection.set(project.findProperty("POM_SCM_DEV_CONNECTION") as String? ?: "")
                }
            }
        }
    }
    
    repositories {
        maven {
            name = "LocalRepo"
            url = uri(layout.buildDirectory.dir("repo"))
        }
        
        // Uncomment when ready to publish to Maven Central
        // maven {
        //     name = "MavenCentral"
        //     val releasesRepoUrl = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
        //     val snapshotsRepoUrl = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
        //     url = if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl
        //     credentials {
        //         username = project.findProperty("ossrhUsername") as String? ?: System.getenv("OSSRH_USERNAME")
        //         password = project.findProperty("ossrhPassword") as String? ?: System.getenv("OSSRH_PASSWORD")
        //     }
        // }
    }
}

signing {
    // Only sign if publishing to Maven Central
    // Requires GPG key setup
    // sign(publishing.publications)
    
    // Use in-memory keys (recommended for CI/CD)
    // val signingKey = project.findProperty("signingKey") as String? ?: System.getenv("SIGNING_KEY")
    // val signingPassword = project.findProperty("signingPassword") as String? ?: System.getenv("SIGNING_PASSWORD")
    // useInMemoryPgpKeys(signingKey, signingPassword)
}
