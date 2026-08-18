@file:OptIn(org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi::class)

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    androidLibrary {
        namespace = "dev.ed3c.gymcometrue.shared"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        compilerOptions.jvmTarget = JvmTarget.JVM_11
        androidResources.enable = true
        withHostTest {
            isIncludeAndroidResources = true
        }
    }

    jvm()

    js {
        browser()
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { target ->
        target.binaries.framework {
            baseName = "GymComeTrueShared"
            isStatic = true
        }
    }

    applyDefaultHierarchyTemplate()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.ui.tooling.preview)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.serialization.json)
            // ponytail: pinned directly rather than via gradle/libs.versions.toml
            // because that file is outside this lane's path lease (Issue #53).
            // org.kotlincrypto.hash:sha2:0.8.0 confirmed on Maven Central
            // (2025-09-19) with a Gradle module publishing jvm/js/wasmJs/native
            // (incl. iosArm64, iosSimulatorArm64) variants matching every target
            // this module builds for. Used by health/EvidenceHandoff.kt to
            // recompute the OCR text digest instead of trusting its shape.
            // Fallback if 0.8.0 fails to resolve: 0.7.1, the previous published
            // stable minor with the same `SHA256().digest(ByteArray)` API.
            implementation("org.kotlincrypto.hash:sha2:0.8.0")
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }

    jvmToolchain(21)
}

dependencies {
    androidRuntimeClasspath(libs.compose.ui.tooling)
}
