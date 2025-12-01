plugins {
    kotlin("js") version "1.9.20"
    id("org.jetbrains.compose") version "1.6.0"
}

kotlin {
    js(IR) {
        browser {
            commonWebpackConfig {
                outputFileName = "app.js"
            }
        }
        binaries.executable()
    }
}

compose.experimental {
    web.application {}
}

repositories {
    mavenCentral()
    google()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}