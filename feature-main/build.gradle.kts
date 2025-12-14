@file:Suppress("DSL_SCOPE_VIOLATION")

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    applyDefaultHierarchyTemplate()

    androidTarget {
        compilations.all {
            compilerOptions {
                jvmTarget.set(JvmTarget.JVM_17)
            }
        }
    }

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

                // Ktor Client
                implementation("io.ktor:ktor-client-core:3.3.3")
                implementation("io.ktor:ktor-client-websockets:3.3.3")
                implementation("io.ktor:ktor-client-content-negotiation:3.3.3")
                implementation("io.ktor:ktor-serialization-kotlinx-json:3.3.3")
            }
        }

        val androidMain by getting {
            dependencies {
                // Ktor Client Engine
                implementation("io.ktor:ktor-client-android:3.3.3")
            }
        }

        val jvmMain by getting {
            dependencies {
                // Ktor Client Engine
                implementation("io.ktor:ktor-client-cio:3.3.3")
            }
        }

        val iosMain by getting {
            dependencies {
                // Ktor Client Engine
                implementation("io.ktor:ktor-client-darwin:3.3.3")
            }
        }

        val jsMain by getting {
            dependencies {
                // Ktor Client Engine
                implementation("io.ktor:ktor-client-js:3.3.3")
            }
        }

        val wasmJsMain by getting {
            dependencies {
                // Ktor Client Engine
                implementation("io.ktor:ktor-client-js:3.3.3")
            }
        }
    }
}

android {
    namespace = "com.example.myapplication.feature.main"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }

    compileOptions {
        val jvmTargetVersion = libs.versions.jvmTarget.get().toInt()

        sourceCompatibility = JavaVersion.toVersion(jvmTargetVersion)
        targetCompatibility = JavaVersion.toVersion(jvmTargetVersion)
    }
}