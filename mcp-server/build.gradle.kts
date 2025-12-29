plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinSerialization)
    application
}

group = "ru.alekseev.mcp"
version = "1.0.0"

application {
    mainClass.set("ru.alekseev.mcp.MainKt")
}

dependencies {
    implementation(projects.coreCommon)

    // Serialization
    implementation(libs.serialization.json)

    // Coroutines
    implementation(libs.kotlinx.coroutines)

    // Google API Client
    implementation(libs.google.api.client)
    implementation(libs.google.oauth.client)
    implementation(libs.google.api.calendar)

    // Ktor Client for HTTP
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)

    // SQLite for Reminders
    implementation(libs.sqlite.jdbc)

    // Logging
    implementation(libs.logback)
}

tasks.withType<Jar> {
    manifest {
        attributes["Main-Class"] = "ru.alekseev.mcp.MainKt"
    }
    // Create fat JAR
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
}
