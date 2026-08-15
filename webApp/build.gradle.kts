import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    js {
        binaries.executable()
        browser {
            commonWebpackConfig {
                outputFileName = "gym-come-true.js"
            }
        }
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        binaries.executable()
        browser {
            commonWebpackConfig {
                outputFileName = "gym-come-true.js"
            }
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":shared"))
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
        }
    }
}
