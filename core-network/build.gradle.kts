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
            jvmTarget.set(JvmTarget.JVM_17)
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