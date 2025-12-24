package ru.alekseev.myapplication.data.datasource

import io.ktor.client.plugins.websocket.webSocketSession
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
import ru.alekseev.myapplication.core.common.SERVER_WS_URL
import ru.alekseev.myapplication.data.dto.ChatRequestDto
import ru.alekseev.myapplication.data.dto.ChatResponseDto
import ru.alekseev.myapplication.data.dto.RagModeDto
import ru.alekseev.myapplication.domain.entity.RagMode

class ChatWebSocketDataSourceImpl(
    private val json: Json,
) : ChatWebSocketDataSource {

    private val serverUrl: String = SERVER_WS_URL

    private val client = createHttpClient(json)

    private var session: WebSocketSession? = null

    override suspend fun connect() {
        if (session != null && session?.isActive == true) {
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

    override suspend fun sendMessage(message: String, ragMode: RagMode) {
        val ragModeDto = when (ragMode) {
            is RagMode.Disabled -> RagModeDto.Disabled
            is RagMode.Enabled -> RagModeDto.Enabled
            is RagMode.EnabledWithFiltering -> RagModeDto.EnabledWithFiltering(ragMode.threshold)
        }

        val request = ChatRequestDto(message = message, ragMode = ragModeDto)
        val jsonString = try {
            json.encodeToString(ChatRequestDto.serializer(), request)
        } catch (e: Exception) {
            println("[ChatWebSocket] Failed to encode request: ${e.message}")
            e.printStackTrace()
            return
        }
        try {
            session?.send(Frame.Text(jsonString))
        } catch (e: Exception) {
            println("[ChatWebSocket] Failed to send message: ${e.message}")
            e.printStackTrace()
        }
    }

    override fun observeMessages(): Flow<ChatResponseDto> = callbackFlow {
        val webSocketSession = session
            ?: run {
                trySend(
                    ChatResponseDto.Error(
                        error = "Failed to connect",
                    )
                )
                close(IllegalStateException("WebSocket not connected"))
                return@callbackFlow
            }

        session = webSocketSession

        try {
            webSocketSession.incoming
                .consumeAsFlow()
                .filterIsInstance<Frame.Text>()
                .mapNotNull { frame ->
                    val text = frame.readText()
                    println("[ChatWebSocket] Received text: '$text'")

                    // Skip empty messages
                    if (text.isBlank()) {
                        println("[ChatWebSocket] Skipping empty message")
                        return@mapNotNull null
                    }

                    try {
                        val decoded = json.decodeFromString(
                            ChatResponseDto.serializer(),
                            text
                        )
                        println("[ChatWebSocket] Successfully decoded: $decoded")
                        decoded
                    } catch (e: Exception) {
                        println("[ChatWebSocket] Parse error: ${e.message}")
                        ChatResponseDto.Error(
                            error = "Failed to parse response: ${e.message}\nJSON input: $text",
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
