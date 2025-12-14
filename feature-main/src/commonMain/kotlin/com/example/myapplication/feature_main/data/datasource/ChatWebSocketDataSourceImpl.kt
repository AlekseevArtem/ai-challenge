package com.example.myapplication.feature_main.data.datasource

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.pingInterval
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.serialization.kotlinx.json.json
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.isActive
import kotlinx.serialization.json.Json
import ru.alekseev.myapplication.SERVER_WS_URL
import ru.alekseev.myapplication.dto.ChatRequestDto
import ru.alekseev.myapplication.dto.ChatResponseDto
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class ChatWebSocketDataSourceImpl(
    private val serverUrl: String = SERVER_WS_URL,
) : ChatWebSocketDataSource {

    private val json = Json {
        classDiscriminator = "type"
        ignoreUnknownKeys = true
        prettyPrint = true
        isLenient = true
        encodeDefaults = true
    }

    private val client = HttpClient {
        install(WebSockets) {
            pingInterval = 20_000.toDuration(DurationUnit.MILLISECONDS)
        }
        install(ContentNegotiation) {
            json(json)
        }
    }

    private var session: WebSocketSession? = null

    override suspend fun connect() {
        println("ChatWebSocketDataSource: connect() called")
        if (session?.isActive == true) {
            println("ChatWebSocketDataSource: session already active")
            return
        }

        try {
            println("ChatWebSocketDataSource: connecting to $serverUrl")
            session = client.webSocketSession(serverUrl)
            println("ChatWebSocketDataSource: connected successfully")
        } catch (e: Exception) {
            println("ChatWebSocketDataSource: connection failed - ${e.message}")
            session = null
        }
    }

    override suspend fun disconnect() {
        println("ChatWebSocketDataSource: disconnect() called")
        try {
            session?.close()
            println("ChatWebSocketDataSource: disconnected successfully")
        } catch (e: Exception) {
            println("ChatWebSocketDataSource: disconnect error - ${e.message}")
        } finally {
            session = null
        }
    }

    override suspend fun sendMessage(message: String) {
        println("ChatWebSocketDataSource: sendMessage() called with message: '$message'")
        println("ChatWebSocketDataSource: session active = ${session?.isActive}")
        val request = ChatRequestDto(message = message)
        println("ChatWebSocketDataSource: created request object: $request")
        val jsonString = try {
            // Try proper serialization first
            val result = json.encodeToString(request)
            println("ChatWebSocketDataSource: serialized JSON: $result")
            result
        } catch (e: Exception) {
            println("ChatWebSocketDataSource: serialization failed, using manual JSON - ${e.message}")
            e.printStackTrace()
            // Fallback to manual JSON creation
            val manual = """{"message":"${message.replace("\"", "\\\"")}"}"""
            println("ChatWebSocketDataSource: manually created JSON: $manual")
            manual
        }
        println("ChatWebSocketDataSource: sending JSON: $jsonString")
        try {
            session?.send(Frame.Text(jsonString))
            println("ChatWebSocketDataSource: message sent successfully")
        } catch (e: Exception) {
            println("ChatWebSocketDataSource: send error - ${e.message}")
            e.printStackTrace()
        }
    }

    override fun observeMessages(): Flow<ChatResponseDto> = callbackFlow {
        println("ChatWebSocketDataSource: observeMessages() called")
        val webSocketSession = try {
            println("ChatWebSocketDataSource: creating websocket session for observing")
            client.webSocketSession(serverUrl)
        } catch (e: Exception) {
            println("ChatWebSocketDataSource: observeMessages connection failed - ${e.message}")
            trySend(
                ChatResponseDto.Error(
                    error = "Failed to connect: ${e.message}",
                )
            )
            close(e)
            return@callbackFlow
        }

        session = webSocketSession
        println("ChatWebSocketDataSource: observeMessages session created successfully")

        try {
            webSocketSession.incoming
                .consumeAsFlow()
                .filterIsInstance<Frame.Text>()
                .mapNotNull { frame ->
                    val text = frame.readText()
                    println("ChatWebSocketDataSource: received frame: $text")
                    try {
                        val decoded = json.decodeFromString(
                            ChatResponseDto.serializer(),
                            text
                        )
                        println("ChatWebSocketDataSource: decoded response: $decoded")
                        decoded
                    } catch (e: Exception) {
                        println("ChatWebSocketDataSource: failed to parse response - ${e.message}")
                        ChatResponseDto.Error(
                            error = "Failed to parse response: ${e.message}",
                        )
                    }
                }
                .collect { response ->
                    println("ChatWebSocketDataSource: sending response to flow: $response")
                    trySend(response)
                }
        } catch (e: Exception) {
            println("ChatWebSocketDataSource: observeMessages error - ${e.message}")
            trySend(
                ChatResponseDto.Error(
                    error = "WebSocket error: ${e.message}",
                )
            )
        } finally {
            println("ChatWebSocketDataSource: observeMessages closing session")
            webSocketSession.close()
        }

        awaitClose {
            println("ChatWebSocketDataSource: observeMessages awaitClose called")
            session = null
        }
    }

    override fun isConnected(): Boolean = session?.isActive == true
}