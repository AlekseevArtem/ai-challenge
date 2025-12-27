package ru.alekseev.myapplication.di

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import org.koin.dsl.module
import ru.alekseev.myapplication.core.common.JsonFactory
import ru.alekseev.myapplication.db.ChatDatabase
import ru.alekseev.myapplication.domain.gateway.ClaudeGateway
import ru.alekseev.myapplication.domain.gateway.DocumentRetriever
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

val observabilityModule = module {
    // Metrics collectors
    single<ru.alekseev.myapplication.domain.observability.RAGMetricsCollector> {
        ru.alekseev.myapplication.infrastructure.observability.PrintlnRAGMetrics()
    }
    single<ru.alekseev.myapplication.domain.observability.ConversationMetricsCollector> {
        ru.alekseev.myapplication.infrastructure.observability.PrintlnConversationMetrics()
    }
}

val serviceModule = module {
    // Conversation orchestration (extracted from ClaudeApiService)
    single { ru.alekseev.myapplication.service.ConversationOrchestrator(get(), get()) }

    // Domain gateway implementations
    single<ClaudeGateway> { ClaudeApiService(get(), get(), get()) }
    single<DocumentRetriever> { DocumentRAGService(config = get()) }

    // Other services
    single { SummarizationService(get()) }
    single { ReminderSchedulerService(get()) }
    single { WebSocketManager(get(), get()) }

    // RAG configuration
    single {
        ru.alekseev.myapplication.config.RAGConfig.load().also { config ->
            println("[AppModule] RAG Config loaded:")
            println("  - Index path: ${config.indexPath}")
            println("  - Ollama URL: ${config.ollamaUrl}")
            println("  - Embedding model: ${config.embeddingModel}")
            println("  - TopK: ${config.topK}")
            println("  - Chunk size: ${config.chunkSize}")
            println("  - Overlap: ${config.overlapSize}")
        }
    }

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
        // TEMPORARILY DISABLED due to bug in search_code tool (v0.26.3)
        // Issue: ToolDependencies not found in context panic
        // TODO: Re-enable when GitHub fixes the bug
        /*
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
        */
        println("GitHub MCP client disabled (temporary workaround for search_code bug)")

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
    // RAG formatting
    factory { ru.alekseev.myapplication.usecase.FormatRAGContextUseCase() }

    // Context sources for message history building
    factory<ru.alekseev.myapplication.domain.context.ContextSource>(qualifier = org.koin.core.qualifier.named("summary")) {
        ru.alekseev.myapplication.domain.context.SummaryContextSource(get())
    }
    factory<ru.alekseev.myapplication.domain.context.ContextSource>(qualifier = org.koin.core.qualifier.named("uncompressed")) {
        ru.alekseev.myapplication.domain.context.UncompressedMessagesContextSource(get())
    }
    factory<ru.alekseev.myapplication.domain.context.ContextSource>(qualifier = org.koin.core.qualifier.named("rag")) {
        ru.alekseev.myapplication.domain.context.RAGContextSource(get(), get(), get(), topK = 3)
    }

    // Message history builder with context sources in priority order
    factory {
        ru.alekseev.myapplication.usecase.MessageHistoryBuilder(
            contextSources = listOf(
                get(org.koin.core.qualifier.named("summary")),
                get(org.koin.core.qualifier.named("uncompressed")),
                get(org.koin.core.qualifier.named("rag"))
                // When adding memory: add MemoryContextSource here
            )
        )
    }

    // Use cases
    factory { LoadChatHistoryUseCase(get(), get()) }
    factory { HandleSummarizationUseCase(get(), get()) }
    factory { ProcessUserMessageUseCase(get(), get(), get(), get(), get()) }
}

val appModules = listOf(jsonModule, databaseModule, observabilityModule, serviceModule, repositoryModule, useCaseModule)
