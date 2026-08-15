import org.gradle.api.initialization.resolve.RepositoriesMode

rootProject.name = "gym-come-true"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

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
    // Kotlin/JS provisions Node.js through a project-scoped Ivy repository.
    // Prefer centrally declared dependency repositories while allowing
    // toolchain-owned distribution repositories required by Kotlin JS/Wasm.
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
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

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

include(":shared")
include(":androidApp")
include(":webApp")
