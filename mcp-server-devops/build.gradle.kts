plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinSerialization)
    application
}

group = "ru.alekseev.mcp"
version = "1.0.0"

application {
    mainClass.set("ru.alekseev.mcp.devops.MainKt")
}

dependencies {
    implementation(projects.coreCommon)

    // Serialization
    implementation(libs.serialization.json)

    // Coroutines
    implementation(libs.kotlinx.coroutines)

    // Ktor Server for HTTP mode
    implementation(libs.ktor.serverCore)
    implementation(libs.ktor.serverNetty)
    implementation(libs.ktor.server.contentNegotiation)
    implementation(libs.ktor.serialization.json)

    // Logging
    implementation(libs.logback)
}

tasks.withType<Jar> {
    manifest {
        attributes["Main-Class"] = "ru.alekseev.mcp.devops.MainKt"
    }
    // Create fat JAR
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
}
