import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}

dependencies {
    implementation(project(":shared"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.mlkit.text.recognition.chinese)
    implementation(libs.mlkit.barcode.scanning)
    // ponytail: pinned directly rather than via gradle/libs.versions.toml because
    // that file is outside this lane's path lease (see newDependencyRequests in
    // the delivery report) — integrator should promote this into the catalog.
    // Reviewed 2026-08-18 (Issue #53): bumped alpha07 -> 1.1.0 stable, confirmed
    // present on Google's Maven (dl.google.com) with no changes since its rc03
    // and no API changes to the surfaces this adapter calls (HealthPermission.
    // getReadPermission, WeightRecord, ExerciseSessionRecord,
    // PermissionController.createRequestPermissionResultContract,
    // HealthConnectClient.getSdkStatus/SDK_AVAILABLE/
    // SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED). See docs/android/health-connect.md.
    implementation("androidx.health.connect:connect-client:1.1.0")
    debugImplementation(libs.compose.ui.tooling)
    testImplementation(libs.kotlin.test)
}

android {
    namespace = "dev.ed3c.gymcometrue"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "dev.ed3c.gymcometrue"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "0.1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildTypes {
        release {
            isDebuggable = false
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}
