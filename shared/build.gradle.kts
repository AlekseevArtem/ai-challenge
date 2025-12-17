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

    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    )
        .forEach {
            it.binaries.framework {
                baseName = "ComposeApp"

                export(libs.decompose.decompose)
                export(libs.essenty.lifecycle)
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
    
    sourceSets {
        commonMain.dependencies {
            // Core Common
            implementation(projects.coreCommon)

            // Decompose
            api(libs.decompose.decompose)
            api(libs.essenty.lifecycle)

            // Serialization for navigation
            implementation(libs.serialization.json)

            //DateTime
            implementation(libs.kotlinx.datetime)

            // Ktor Client
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.websockets)
            implementation(libs.ktor.client.contentNegotiation)
            implementation(libs.ktor.serialization.json)

            //Koin
            implementation(libs.koin.core)
        }

        val androidMain by getting {
            dependencies {
                // Ktor Client Engine
                implementation(libs.ktor.client.okhttp)
                implementation(libs.androidx.core.ktx)

                //Koin
                implementation(libs.koin.android)
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

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

android {
    namespace = "ru.alekseev.myapplication.shared"
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
