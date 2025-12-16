plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.sqldelight)
    application
}

group = "ru.alekseev.myapplication"
version = "1.0.0"
application {
    mainClass.set("ru.alekseev.myapplication.ApplicationKt")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

dependencies {
    implementation(projects.shared)
    implementation(libs.logback)

    // Ktor Server
    implementation(libs.ktor.serverCore)
    implementation(libs.ktor.serverNetty)
    implementation("io.ktor:ktor-server-websockets-jvm:3.3.3")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:3.3.3")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:3.3.3")
    implementation("io.ktor:ktor-server-cors-jvm:3.3.3")
    implementation("io.ktor:ktor-server-call-logging-jvm:3.3.3")
    implementation("io.ktor:ktor-server-double-receive-jvm:3.3.3")

    // Ktor Client
    implementation("io.ktor:ktor-client-core-jvm:3.3.3")
    implementation("io.ktor:ktor-client-cio-jvm:3.3.3")
    implementation("io.ktor:ktor-client-content-negotiation-jvm:3.3.3")
    implementation("io.ktor:ktor-client-logging-jvm:3.3.3")

    // Serialization
    implementation(libs.serialization.json)

    // Coroutines
    implementation(libs.kotlinx.coroutines)

    // SQLDelight
    implementation(libs.sqldelight.driver.sqlite)
    implementation(libs.sqldelight.coroutinesExtensions)

    // Koin
    implementation(libs.koin.core)
    implementation(libs.koin.ktor)

    // Google Calendar API
    implementation("com.google.api-client:google-api-client:2.7.0")
    implementation("com.google.oauth-client:google-oauth-client-jetty:1.36.0")
    implementation("com.google.apis:google-api-services-calendar:v3-rev20220715-2.0.0")

    // Tests
    testImplementation(libs.ktor.serverTestHost)
    testImplementation(libs.kotlin.testJunit)
}

sqldelight {
    databases {
        create("ChatDatabase") {
            packageName.set("ru.alekseev.myapplication.db")
            dialect("app.cash.sqldelight:sqlite-3-38-dialect:2.0.2")
        }
    }
}