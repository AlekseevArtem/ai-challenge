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
    implementation("com.google.api-client:google-api-client:2.7.0")
    implementation("com.google.oauth-client:google-oauth-client-jetty:1.36.0")
    implementation("com.google.apis:google-api-services-calendar:v3-rev20220715-2.0.0")

    // Ktor Client for HTTP
    implementation("io.ktor:ktor-client-core-jvm:3.3.3")
    implementation("io.ktor:ktor-client-cio-jvm:3.3.3")

    // Logging
    implementation("ch.qos.logback:logback-classic:1.5.17")
}

tasks.withType<Jar> {
    manifest {
        attributes["Main-Class"] = "ru.alekseev.mcp.MainKt"
    }
    // Create fat JAR
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
}
