import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JvmVendorSpec
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.compile.JavaCompile

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.kotlin.serialization)
    application
}

group = "love.bside.app"
version = "1.0.0"
application {
    mainClass.set("love.bside.app.ApplicationKt")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
        vendor.set(JvmVendorSpec.ADOPTIUM)
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
        vendor.set(JvmVendorSpec.ADOPTIUM)
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(21)
}

tasks.withType<KotlinJvmCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

// Handle duplicate dependencies in distribution
tasks.withType<Tar> {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.withType<Zip> {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.named<Sync>("installDist") {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    doFirst {
        destinationDir.deleteRecursively()
    }
}

dependencies {
    implementation(projects.shared)
    implementation(libs.logback)
    
    // Ktor server core
    implementation(libs.ktor.serverCore)
    implementation(libs.ktor.serverNetty)
    
    // Ktor server plugins
    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.server.auth.jwt)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.server.call.logging)
    implementation(libs.ktor.server.cors)
    implementation(libs.ktor.server.status.pages)
    
    // Ktor client (for PocketBase)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.client.logging)
    
    // Serialization
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.datetime)
    
    // Dependency Injection
    implementation(libs.koin.core)
    implementation(libs.koin.ktor)
    
    // CLI for migrations
    implementation("org.jetbrains.kotlinx:kotlinx-cli:0.3.6")
    
    // Testing
    testImplementation(libs.ktor.serverTestHost)
    testImplementation(libs.kotlin.testJunit)
    testImplementation(libs.ktor.client.mock)
    testImplementation(libs.kotlinx.coroutinesCore)
}

// Task to run migrations
tasks.register<JavaExec>("runMigrations") {
    group = "database"
    description = "Run database migrations"
    mainClass.set("love.bside.server.migrations.MigrationCliKt")
    classpath = sourceSets["main"].runtimeClasspath
    args = if (project.hasProperty("args")) {
        (project.property("args") as String).split(" ")
    } else {
        listOf("run")
    }
}

// Task to validate schema
tasks.register<JavaExec>("runSchemaValidator") {
    group = "database"
    description = "Validate database schema"
    mainClass.set("love.bside.server.schema.SchemaValidatorKt")
    classpath = sourceSets["main"].runtimeClasspath
    args = if (project.hasProperty("args")) {
        (project.property("args") as String).split(" ")
    } else {
        listOf("validate")
    }
}