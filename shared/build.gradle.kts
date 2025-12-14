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
            jvmTarget.set(JvmTarget.JVM_11)
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

            // Decompose
            api(libs.decompose.decompose)
            api(libs.essenty.lifecycle)

            // Serialization for navigation
            implementation(libs.serialization.json)

            //DateTime
            implementation(libs.kotlinx.datetime)
        }

        val iosArm64Main by getting {
            dependencies {
                api(libs.decompose.decompose)
                api(libs.essenty.lifecycle)
            }
        }

        val iosSimulatorArm64Main by getting {
            dependencies {
                api(libs.decompose.decompose)
                api(libs.essenty.lifecycle)
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
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
}
