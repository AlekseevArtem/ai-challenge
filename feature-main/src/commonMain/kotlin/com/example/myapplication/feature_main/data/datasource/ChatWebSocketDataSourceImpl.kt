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
import ru.alekseev.myapplication.data.dto.ChatRequestDto
import ru.alekseev.myapplication.data.dto.ChatResponseDto
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class ChatWebSocketDataSourceImpl(
    private val json: Json,
) : ChatWebSocketDataSource {

    private val serverUrl: String = SERVER_WS_URL

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
        if (session?.isActive == true) {
            return
        }

        try {
            session = client.webSocketSession(serverUrl)
        } catch (e: Exception) {
            session = null
        }
    }

    override suspend fun disconnect() {
        try {
            session?.close()
        } catch (e: Exception) {
        } finally {
            session = null
        }
    }

    override suspend fun sendMessage(message: String) {
        val request = ChatRequestDto(message = message)
        val jsonString = try {
            val result = json.encodeToString(request)
            result
        } catch (e: Exception) {
            val manual = """{"message":"${message.replace("\"", "\\\"")}"}"""
            manual
        }
        try {
            session?.send(Frame.Text(jsonString))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun observeMessages(): Flow<ChatResponseDto> = callbackFlow {
        val webSocketSession = try {
            client.webSocketSession(serverUrl)
        } catch (e: Exception) {
            trySend(
                ChatResponseDto.Error(
                    error = "Failed to connect: ${e.message}",
                )
            )
            close(e)
            return@callbackFlow
        }

        session = webSocketSession

        try {
            webSocketSession.incoming
                .consumeAsFlow()
                .filterIsInstance<Frame.Text>()
                .mapNotNull { frame ->
                    val text = frame.readText()
                    try {
                        val decoded = json.decodeFromString(
                            ChatResponseDto.serializer(),
                            text
                        )
                        decoded
                    } catch (e: Exception) {
                        ChatResponseDto.Error(
                            error = "Failed to parse response: ${e.message}",
                        )
                    }
                }
                .collect { response ->
                    trySend(response)
                }
        } catch (e: Exception) {
            trySend(
                ChatResponseDto.Error(
                    error = "WebSocket error: ${e.message}",
                )
            )
        } finally {
            webSocketSession.close()
        }

        awaitClose {
            session = null
        }
    }

    override fun isConnected(): Boolean = session?.isActive == true
}