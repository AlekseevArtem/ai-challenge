@file:OptIn(ExperimentalUuidApi::class)

package ru.alekseev.myapplication.routing

import io.ktor.server.routing.Route
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.channels.consumeEach
import kotlinx.serialization.json.Json
import org.koin.java.KoinJavaComponent.inject
import ru.alekseev.myapplication.core.common.ChatConstants
import ru.alekseev.myapplication.data.dto.*
import ru.alekseev.myapplication.domain.entity.RagMode
import ru.alekseev.myapplication.domain.model.UserId
import ru.alekseev.myapplication.mapper.toMessagePairDtos
import ru.alekseev.myapplication.usecase.HandleSummarizationUseCase
import ru.alekseev.myapplication.usecase.LoadChatHistoryUseCase
import ru.alekseev.myapplication.usecase.ProcessUserMessageUseCase
import ru.alekseev.myapplication.service.WebSocketManager
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * WebSocket route for chat functionality.
 * Handles:
 * - Loading and sending chat history on connection
 * - Receiving user messages
 * - Automatic summarization when threshold is reached
 * - Processing messages through Claude API
 * - Sending responses back to client
 */
fun Route.chatRouting() {
    // Inject dependencies
    val json: Json by inject(Json::class.java)
    val loadChatHistoryUseCase: LoadChatHistoryUseCase by inject(LoadChatHistoryUseCase::class.java)
    val handleSummarizationUseCase: HandleSummarizationUseCase by inject(HandleSummarizationUseCase::class.java)
    val processUserMessageUseCase: ProcessUserMessageUseCase by inject(ProcessUserMessageUseCase::class.java)
    val webSocketManager: WebSocketManager by inject(WebSocketManager::class.java)

    webSocket("/chat") {
        val userId = UserId(ChatConstants.DEFAULT_USER_ID)
        val connectionId = Uuid.random().toString()

        // Register this connection
        webSocketManager.registerConnection(connectionId, this)

        try {
            // Load and send chat history on connection
            val historyMessages = loadChatHistoryUseCase(userId)
            if (historyMessages.isNotEmpty()) {
                send(
                    Frame.Text(
                        json.encodeToString(
                            ChatResponseDto.serializer(),
                            ChatResponseDto.History(historyMessages)
                        )
                    )
                )
            }

            // Process incoming messages
            incoming.consumeEach { frame ->
                if (frame is Frame.Text) {
                    val text = frame.readText()

                    try {
                        val request = json.decodeFromString<ChatRequestDto>(text)

                        // Send loading state
                        send(
                            Frame.Text(
                                json.encodeToString(
                                    ChatResponseDto.serializer(),
                                    ChatResponseDto.Loading
                                )
                            )
                        )

                        // Handle summarization if needed
                        handleSummarizationUseCase(userId)

                        // Convert RagModeDto to domain RagMode
                        val ragMode = when (val dto = request.ragMode) {
                            is RagModeDto.Disabled -> RagMode.Disabled
                            is RagModeDto.Enabled -> RagMode.Enabled
                            is RagModeDto.EnabledWithFiltering -> RagMode.EnabledWithFiltering(dto.threshold)
                        }

                        // Process user message and get response (returns domain Message)
                        val result = processUserMessageUseCase(request.message, userId, ragMode)

                        // Convert domain Message to DTOs for sending to client
                        val (userMessageDto, assistantMessageDto) = result.message.toMessagePairDtos(
                            messageInfo = result.messageInfo,
                            usedRag = result.usedRag
                        )

                        // Send user message
                        send(
                            Frame.Text(
                                json.encodeToString(
                                    ChatResponseDto.serializer(),
                                    ChatResponseDto.Data(message = userMessageDto)
                                )
                            )
                        )

                        // Send assistant response
                        send(
                            Frame.Text(
                                json.encodeToString(
                                    ChatResponseDto.serializer(),
                                    ChatResponseDto.Data(message = assistantMessageDto)
                                )
                            )
                        )

                    } catch (e: Exception) {
                        e.printStackTrace()
                        send(
                            Frame.Text(
                                json.encodeToString(
                                    ChatResponseDto.serializer(),
                                    ChatResponseDto.Error(error = e.message ?: "Unknown error")
                                )
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            // Unregister connection when WebSocket closes
            webSocketManager.unregisterConnection(connectionId)
        }
    }
}
