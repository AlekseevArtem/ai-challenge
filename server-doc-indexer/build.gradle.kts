plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinSerialization)
    application
    `java-library`
}

group = "ru.alekseev.indexer"
version = "1.0.0"

application {
    mainClass.set("ru.alekseev.indexer.MainKt")
    applicationDefaultJvmArgs = listOf(
        "-DprojectRoot=${rootProject.projectDir.absolutePath}"
    )
}

tasks.named<JavaExec>("run") {
    // Pass project root as working directory for file discovery
    workingDir = rootProject.projectDir
}

kotlin {
    jvmToolchain(17)
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    // Serialization
    implementation(libs.serialization.json)

    // Coroutines
    implementation(libs.kotlinx.coroutines)

    // Ktor Client for HTTP requests (MCP + Ollama)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.contentNegotiation)
    implementation(libs.ktor.serialization.json)

    // Logging
    implementation(libs.logback)
}

// Create a separate fat JAR task for standalone execution
tasks.register<Jar>("fatJar") {
    group = "build"
    description = "Create a fat JAR with all dependencies"
    archiveClassifier.set("all")
    manifest {
        attributes["Main-Class"] = "ru.alekseev.indexer.MainKt"
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(sourceSets.main.get().output)
    dependsOn(configurations.runtimeClasspath)
    from({
        configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }.map { zipTree(it) }
    })
}

// Task to run the search example
tasks.register<JavaExec>("runSearch") {
    group = "application"
    description = "Search the indexed documents"
    mainClass.set("ru.alekseev.indexer.SearchExampleKt")
    classpath = sourceSets["main"].runtimeClasspath
    workingDir = rootProject.projectDir
}
