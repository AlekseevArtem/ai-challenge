package ru.alekseev.myapplication

import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.doublereceive.DoubleReceive
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import io.ktor.server.request.receiveText
import io.ktor.server.response.ApplicationSendPipeline
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.head
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.pingPeriod
import io.ktor.server.websocket.timeout
import kotlinx.serialization.json.Json
import org.koin.core.context.startKoin
import org.koin.java.KoinJavaComponent.inject
import ru.alekseev.myapplication.core.common.SERVER_PORT
import ru.alekseev.myapplication.di.appModules
import ru.alekseev.myapplication.routing.chatRouting
import ru.alekseev.myapplication.service.DocumentRAGService
import ru.alekseev.myapplication.service.ReminderSchedulerService
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

fun main() {
    startKoin {
        modules(appModules)
    }

    embeddedServer(
        factory = Netty,
        port = SERVER_PORT,
        host = "0.0.0.0",
        module = Application::module,
    )
        .start(wait = true)
}

fun Application.module() {
    val json: Json by inject(Json::class.java)

    install(DoubleReceive)

    install(CallLogging) {
        format { call ->
            val status = call.response.status()
            val httpMethod = call.request.httpMethod.value
            val userAgent = call.request.headers["User-Agent"]
            val path = call.request.path()
            "$httpMethod $path -> $status (User-Agent: $userAgent)"
        }
    }

    intercept(ApplicationCallPipeline.Monitoring) {
        try {
            val requestBody = call.receiveText()
            if (requestBody.isNotEmpty()) {
                application.log.info("Request body: $requestBody")
            }
        } catch (_: Exception) {

        }
    }

    sendPipeline.intercept(ApplicationSendPipeline.After) { message ->
        if (message is String) {
            application.log.info("Response body: $message")
        }
    }

    install(WebSockets) {
        pingPeriod = 15.seconds
        timeout = 15.seconds
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }

    install(ContentNegotiation) {
        json(json)
    }

    install(CORS) {
        anyHost()
        allowHeader("Content-Type")
    }

    // Start reminder scheduler
    val reminderScheduler: ReminderSchedulerService by inject(ReminderSchedulerService::class.java)
    reminderScheduler.start()
    log.info("Reminder scheduler started")

    // Initialize Document RAG Service
    val documentRAGService: DocumentRAGService by inject(DocumentRAGService::class.java)
    launch {
        try {
            log.info("Initializing Document RAG Service...")
            documentRAGService.initialize()
            val stats = documentRAGService.getStats()
            if (stats.isLoaded) {
                log.info("Document RAG Service initialized successfully")
                log.info("  - Total vectors: ${stats.totalVectors}")
                log.info("  - Total files: ${stats.totalFiles}")
                log.info("  - Indexed at: ${stats.indexedAt}")
            } else {
                log.info("Document index not found. RAG will be unavailable.")
                log.info("To create the index, run: ./gradlew :server-doc-indexer:run")
            }
        } catch (e: Exception) {
            log.error("Failed to initialize Document RAG Service: ${e.message}")
        }
    }

    routing {
        get("/") {
            call.respondText("Ktor: ${Greeting().greet()}")
        }

        head("/") {
            call.respondText("")
        }

        chatRouting()
    }
}