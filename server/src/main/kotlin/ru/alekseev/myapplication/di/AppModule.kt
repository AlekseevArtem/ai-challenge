package ru.alekseev.myapplication.di

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import org.koin.dsl.module
import ru.alekseev.myapplication.core.common.JsonFactory
import ru.alekseev.myapplication.db.ChatDatabase
import ru.alekseev.myapplication.repository.ChatRepository
import ru.alekseev.myapplication.repository.ChatRepositoryImpl
import ru.alekseev.myapplication.service.ClaudeApiService
import ru.alekseev.myapplication.service.DocumentRAGService
import ru.alekseev.myapplication.service.MCPClient
import ru.alekseev.myapplication.service.MCPManager
import ru.alekseev.myapplication.service.SummarizationService
import ru.alekseev.myapplication.service.ReminderSchedulerService
import ru.alekseev.myapplication.service.WebSocketManager
import ru.alekseev.myapplication.usecase.HandleSummarizationUseCase
import ru.alekseev.myapplication.usecase.LoadChatHistoryUseCase
import ru.alekseev.myapplication.usecase.ProcessUserMessageUseCase
import java.io.File
import java.util.Properties

val jsonModule = module {
    single {
        JsonFactory.create()
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
    single { ReminderSchedulerService(get()) }
    single { WebSocketManager(get(), get()) }
    single { DocumentRAGService() }

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

        // Configure GitHub MCP Server (Docker-based)
        val githubToken = System.getenv("GITHUB_PERSONAL_ACCESS_TOKEN")
        if (githubToken != null && githubToken.isNotEmpty()) {
            println("Found GitHub token, registering GitHub MCP client")

            val githubClient = MCPClient(
                name = "github",
                command = listOf(
                    "docker", "run", "-i", "--rm",
                    "-e", "GITHUB_PERSONAL_ACCESS_TOKEN=$githubToken",
                    "ghcr.io/github/github-mcp-server"
                ),
                environment = mapOf(),
                workingDirectory = projectRoot
            )

            mcpManager.registerClient("github", githubClient)
            println("Registered GitHub MCP client")
        } else {
            println("WARNING: GITHUB_PERSONAL_ACCESS_TOKEN not found in environment")
            println("GitHub MCP tools will not be available")
            println("Please set GITHUB_PERSONAL_ACCESS_TOKEN environment variable")
        }

        // Configure DevOps MCP Server (HTTP-based, running on host)
        // This connects to the DevOps MCP server running on the host machine (Mac)
        // The server should be started with: ./start-devops-mcp.sh (or ./start-devops-mcp-background.sh)
        val devopsServerUrl = "http://host.docker.internal:8082"
        println("Registering DevOps MCP HTTP client at $devopsServerUrl")

        val devopsClient = ru.alekseev.myapplication.service.MCPHttpClient(
            name = "devops",
            baseUrl = devopsServerUrl
        )

        mcpManager.registerHttpClient("devops", devopsClient)
        println("Registered DevOps MCP HTTP client (will connect when server starts)")
        println("To start DevOps MCP server on host: ./start-devops-mcp.sh (foreground) or ./start-devops-mcp-background.sh (daemon)")

        mcpManager
    }
}

val repositoryModule = module {
    single<ChatRepository> { ChatRepositoryImpl(get()) }
}

val useCaseModule = module {
    factory { LoadChatHistoryUseCase(get(), get()) }
    factory { HandleSummarizationUseCase(get(), get()) }
    factory { ProcessUserMessageUseCase(get(), get(), get(), get()) }
}

val appModules = listOf(jsonModule, databaseModule, serviceModule, repositoryModule, useCaseModule)
