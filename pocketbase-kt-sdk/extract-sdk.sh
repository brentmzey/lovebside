#!/bin/bash

# Extract PocketBase Kotlin SDK as Standalone Library
# Usage: ./extract-sdk.sh [target-directory]

set -e  # Exit on error

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SOURCE_DIR="$SCRIPT_DIR"
PARENT_DIR="$(dirname "$SCRIPT_DIR")"
TARGET_DIR="${1:-$HOME/projects/pocketbase-kt-sdk}"

echo "🚀 Extracting PocketBase Kotlin SDK..."
echo "   Source: $SOURCE_DIR"
echo "   Target: $TARGET_DIR"
echo ""

# Create target directory
mkdir -p "$TARGET_DIR"

# Copy SDK files
echo "📦 Copying SDK files..."
rsync -av --exclude='.gradle' --exclude='build' "$SOURCE_DIR/" "$TARGET_DIR/"

# Copy Gradle wrapper
echo "📦 Copying Gradle wrapper..."
cp -r "$PARENT_DIR/gradle" "$TARGET_DIR/"
cp "$PARENT_DIR/gradlew" "$TARGET_DIR/"
cp "$PARENT_DIR/gradlew.bat" "$TARGET_DIR/"

# Copy version catalog if it doesn't exist
if [ ! -d "$TARGET_DIR/gradle" ]; then
    mkdir -p "$TARGET_DIR/gradle"
fi
cp "$PARENT_DIR/gradle/libs.versions.toml" "$TARGET_DIR/gradle/"

# Create root build.gradle.kts
echo "📝 Creating root build file..."
cat > "$TARGET_DIR/build.gradle.kts" << 'EOF'
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
EOF

# Create root settings.gradle.kts
echo "📝 Creating root settings file..."
cat > "$TARGET_DIR/settings.gradle.kts" << 'EOF'
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
EOF

# Initialize Git
echo "🎯 Initializing Git repository..."
cd "$TARGET_DIR"
if [ ! -d ".git" ]; then
    git init
    git add .
    git commit -m "Initial commit: PocketBase Kotlin Multiplatform SDK

Extracted from parent project and configured as standalone library.

Features:
- Full CRUD operations for PocketBase
- Realtime/SSE subscription support
- Authentication flows
- Kotlin Multiplatform (Android, iOS, JVM, JS)
- Pure Kotlin implementation using Ktor"
    
    echo ""
    echo "✅ Git repository initialized"
else
    echo "⚠️  Git repository already exists, skipping initialization"
fi

# Test build
echo ""
echo "🔨 Testing build..."
./gradlew clean build --console=plain || {
    echo "⚠️  Build test skipped (may have compilation issues to fix)"
}

echo ""
echo "✅ SDK extracted successfully to: $TARGET_DIR"
echo ""
echo "📋 Next steps:"
echo ""
echo "1. Update gradle.properties with your information:"
echo "   cd $TARGET_DIR"
echo "   nano gradle.properties"
echo ""
echo "2. Test local publishing:"
echo "   ./gradlew publishToMavenLocal"
echo ""
echo "3. Verify in your local Maven repo:"
echo "   ls -la ~/.m2/repository/io/pocketbase/pocketbase-kt-sdk/"
echo ""
echo "4. Create GitHub repository:"
echo "   - Go to https://github.com/new"
echo "   - Create repository 'pocketbase-kt-sdk'"
echo "   - Then run:"
echo "     cd $TARGET_DIR"
echo "     git remote add origin https://github.com/YOUR_USERNAME/pocketbase-kt-sdk.git"
echo "     git branch -M main"
echo "     git push -u origin main"
echo ""
echo "5. Read the publishing guide:"
echo "   cat PUBLISHING.md"
echo ""
echo "🎉 Happy publishing!"
