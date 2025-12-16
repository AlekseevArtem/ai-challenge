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

    // Logging
    implementation(libs.logback.mcp)
}

tasks.withType<Jar> {
    manifest {
        attributes["Main-Class"] = "ru.alekseev.mcp.MainKt"
    }
    // Create fat JAR
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
}
