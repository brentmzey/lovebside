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
    val sharedCatalog = file("../gradle/libs.versions.toml")
    val localCatalog = file("gradle/libs.versions.toml")
    val catalogFile = when {
        sharedCatalog.exists() -> sharedCatalog
        localCatalog.exists() -> localCatalog
        else -> throw GradleException("Missing libs.versions.toml for pocketbase-kt-sdk build")
    }

    versionCatalogs {
        create("libs") {
            from(files(catalogFile))
        }
    }

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
