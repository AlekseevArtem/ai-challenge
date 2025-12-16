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
import ru.alekseev.myapplication.usecase.HandleSummarizationUseCase
import ru.alekseev.myapplication.usecase.LoadChatHistoryUseCase
import ru.alekseev.myapplication.usecase.ProcessUserMessageUseCase

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

    webSocket("/chat") {
        val userId = ChatConstants.DEFAULT_USER_ID

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

                        // Process user message and get response
                        val result = processUserMessageUseCase(request.message, userId)

                        // Send user message
                        send(
                            Frame.Text(
                                json.encodeToString(
                                    ChatResponseDto.serializer(),
                                    ChatResponseDto.Data(message = result.userMessage)
                                )
                            )
                        )

                        // Send assistant response
                        send(
                            Frame.Text(
                                json.encodeToString(
                                    ChatResponseDto.serializer(),
                                    ChatResponseDto.Data(message = result.assistantMessage)
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
        }
    }
}
