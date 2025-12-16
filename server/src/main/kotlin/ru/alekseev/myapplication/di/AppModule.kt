package ru.alekseev.myapplication.di

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlinx.serialization.json.Json
import org.koin.dsl.module
import ru.alekseev.myapplication.db.ChatDatabase
import ru.alekseev.myapplication.repository.ChatRepository
import ru.alekseev.myapplication.repository.ChatRepositoryImpl
import ru.alekseev.myapplication.service.ClaudeApiService
import ru.alekseev.myapplication.service.MCPClient
import ru.alekseev.myapplication.service.MCPManager
import ru.alekseev.myapplication.service.SummarizationService
import java.io.File
import java.util.Properties

val jsonModule = module {
    single {
        Json {
            classDiscriminator = "type"
            ignoreUnknownKeys = true
            prettyPrint = true
            isLenient = true
            encodeDefaults = true
        }
    }
}

val databaseModule = module {
    single<SqlDriver> {
        // Use system temp directory or user home for database file
        val dbPath = System.getProperty("user.home") + "/chat.db"

        val driver = JdbcSqliteDriver(
            url = "jdbc:sqlite:$dbPath",
            properties = Properties().apply {
                put("foreign_keys", "true")
            }
        )

        // Create tables if they don't exist
        try {
            ChatDatabase.Schema.create(driver)
        } catch (e: Exception) {

        }

        driver
    }

    single {
        ChatDatabase(driver = get())
    }
}

val serviceModule = module {
    single { ClaudeApiService(get(), get()) }
    single { SummarizationService(get()) }

    single {
        val mcpManager = MCPManager()

        // Configure Google Calendar MCP Server
        val projectRoot = System.getProperty("user.dir")
        println("Current working directory: $projectRoot")

        // Try multiple possible locations for credentials
        val possibleCredentialsPaths = listOf(
            "$projectRoot/server/google-calendar-credentials.json",
            "${System.getProperty("user.home")}/google-calendar-credentials.json",
            "./server/google-calendar-credentials.json",
            "./google-calendar-credentials.json"
        )

        val credentialsPath = possibleCredentialsPaths.firstOrNull { File(it).exists() }
        val tokensPath = "$projectRoot/server/tokens"

        if (credentialsPath != null) {
            println("Found Google Calendar credentials at: $credentialsPath")

            // Try multiple possible locations for MCP server JAR
            val possibleJarPaths = listOf(
                "$projectRoot/mcp-server/build/libs/mcp-server-1.0.0.jar",
                "./mcp-server/build/libs/mcp-server-1.0.0.jar",
                "../mcp-server/build/libs/mcp-server-1.0.0.jar"
            )

            val mcpServerJar = possibleJarPaths.firstOrNull { File(it).exists() }

            if (mcpServerJar != null) {
                println("Found MCP server JAR at: $mcpServerJar")

                // Register Google Calendar MCP client
                val calendarClient = MCPClient(
                    name = "google-calendar",
                    command = listOf(
                        "java",
                        "-jar",
                        File(mcpServerJar).absolutePath
                    ),
                    environment = mapOf(
                        "GOOGLE_CALENDAR_CREDENTIALS_PATH" to File(credentialsPath).absolutePath,
                        "GOOGLE_CALENDAR_TOKENS_PATH" to File(tokensPath).absolutePath
                    ),
                    workingDirectory = projectRoot
                )

                mcpManager.registerClient("google-calendar", calendarClient)
                println("Registered Google Calendar MCP client")
            } else {
                println("WARNING: MCP server JAR not found. Tried:")
                possibleJarPaths.forEach { println("  - $it") }
                println("Please build mcp-server: ./gradlew :mcp-server:jar")
            }
        } else {
            println("WARNING: Google Calendar credentials not found. Tried:")
            possibleCredentialsPaths.forEach { println("  - $it") }
            println("MCP Calendar tools will not be available")
            println("Please place google-calendar-credentials.json in one of the above locations")
        }

        mcpManager
    }
}

val repositoryModule = module {
    single<ChatRepository> { ChatRepositoryImpl(get()) }
}

val appModules = listOf(jsonModule, databaseModule, serviceModule, repositoryModule)
