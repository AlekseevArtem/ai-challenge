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
        compilerOptions {
            jvmTarget.set(JvmTarget.fromTarget(libs.versions.jvmTarget.get()))
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
                // Serialization
                implementation(libs.serialization.json)
            }
        }
        val androidMain by getting {
            dependencies {
                // Ktor Client Engine
                implementation(libs.ktor.client.android)
            }
        }

        val jvmMain by getting {
            dependencies {
                // Ktor Client Engine
                implementation(libs.ktor.client.cio)
            }
        }

        val iosMain by getting {
            dependencies {
                // Ktor Client Engine
                implementation(libs.ktor.client.darwin)
            }
        }

        val jsMain by getting {
            dependencies {
                // Ktor Client Engine
                implementation(libs.ktor.client.js)
            }
        }

        val wasmJsMain by getting {
            dependencies {
                // Ktor Client Engine
                implementation(libs.ktor.client.js)
            }
        }
    }
}

android {
    namespace = "ru.alekseev.myapplication.core.network"
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