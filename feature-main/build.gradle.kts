@file:Suppress("DSL_SCOPE_VIOLATION")

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    applyDefaultHierarchyTemplate()

    jvm()

    js {
        browser()
        binaries.library()
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        binaries.library()
    }

    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        val commonMain by getting {
            dependencies {
                // Core
                api(project(":shared"))
                api(project(":core-common"))

                // Decompose
                api(libs.decompose.decompose)
                api(libs.essenty.lifecycle)

                // FlowMVI
                implementation(libs.flowmvi.core)

                // Koin
                implementation(libs.koin.core)

                // Coroutines
                implementation(libs.kotlinx.coroutines)

                // Serialization
                implementation(libs.serialization.json)

                //DateTime
                implementation(libs.kotlinx.datetime)
            }
        }
    }
}
