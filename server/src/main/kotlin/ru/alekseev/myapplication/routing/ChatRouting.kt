package ru.alekseev.myapplication.routing

import io.ktor.server.routing.Route
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.channels.consumeEach
import kotlinx.serialization.json.Json
import org.koin.java.KoinJavaComponent.inject
import ru.alekseev.myapplication.data.dto.ChatMessageDto
import ru.alekseev.myapplication.data.dto.ChatRequestDto
import ru.alekseev.myapplication.data.dto.ChatResponseDto
import ru.alekseev.myapplication.data.dto.ClaudeMessage
import ru.alekseev.myapplication.data.dto.ClaudeRequest
import ru.alekseev.myapplication.data.dto.ClaudeResponse
import ru.alekseev.myapplication.data.dto.MessageInfoDto
import ru.alekseev.myapplication.data.dto.MessageSender
import ru.alekseev.myapplication.repository.ChatRepository
import ru.alekseev.myapplication.service.ClaudeApiService
import ru.alekseev.myapplication.service.SummarizationService
import java.util.UUID
import kotlin.system.measureTimeMillis

fun Route.chatRouting() {
    val chatRepository: ChatRepository by inject(ChatRepository::class.java)
    val claudeApiService: ClaudeApiService by inject(ClaudeApiService::class.java)
    val summarizationService: SummarizationService by inject(SummarizationService::class.java)
    val json: Json by inject(Json::class.java)

    webSocket("/chat") {
        val userId = "default_user"

        try {
            // Load and send chat history on connection
            val allMessages = chatRepository.getAllMessages(userId)
            val historyMessages = allMessages.flatMap { msg ->
                val claudeResponseJson = msg.claude_response_json
                val claudeResponse = json.decodeFromString<ClaudeResponse>(claudeResponseJson)

                // Parse cost based on model and tokens
                val cost = calculateCost(
                    model = claudeResponse.model ?: "claude-sonnet-4-5-20250929",
                    inputTokens = claudeResponse.usage?.inputTokens ?: 0,
                    outputTokens = claudeResponse.usage?.outputTokens ?: 0
                )

                // Return both user and assistant messages
                listOf(
                    // User message
                    ChatMessageDto(
                        id = "${msg.id}_user",
                        content = msg.user_message,
                        sender = MessageSender.USER,
                        timestamp = msg.timestamp - 1, // Slightly earlier timestamp for ordering
                        messageInfo = null
                    ),
                    // Assistant message
                    ChatMessageDto(
                        id = msg.id,
                        content = msg.assistant_message,
                        sender = MessageSender.ASSISTANT,
                        timestamp = msg.timestamp,
                        messageInfo = MessageInfoDto(
                            inputTokens = claudeResponse.usage?.inputTokens ?: 0,
                            outputTokens = claudeResponse.usage?.outputTokens ?: 0,
                            responseTimeMs = msg.response_time_ms,
                            model = claudeResponse.model ?: "unknown",
                            cost = cost
                        )
                    )
                )
            }

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

                        // Check if we need to perform summarization
                        val uncompressedCount = chatRepository.getUncompressedMessagesCount(userId)

                        if (uncompressedCount >= 5) {
                            val uncompressedMessages = chatRepository.getUncompressedMessages(userId)

                            // Create summary uncompressed messages
                            val messagePairs = uncompressedMessages.map { msg ->
                                msg.user_message to msg.assistant_message
                            }

                            try {
                                val summaryText = summarizationService.createSummary(messagePairs)

                                // Save summary
                                val summaries = chatRepository.getAllSummaries(userId)
                                chatRepository.saveSummary(
                                    id = UUID.randomUUID().toString(),
                                    summaryText = summaryText,
                                    messagesCount = uncompressedCount.toInt(),
                                    timestamp = System.currentTimeMillis(),
                                    position = summaries.size,
                                    userId = userId
                                )

                                // Mark messages as compressed
                                chatRepository.markMessagesAsCompressed(
                                    uncompressedMessages.map { it.id }
                                )
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }

                        // Build message history for Claude API
                        val summaries = chatRepository.getAllSummaries(userId)
                        val uncompressedMessages = chatRepository.getUncompressedMessages(userId)

                        val messagesForApi = mutableListOf<ClaudeMessage>()

                        // Add summaries as system context
                        if (summaries.isNotEmpty()) {
                            val summaryContext = summaries.joinToString("\n\n") {
                                "Previous conversation summary: ${it.summary_text}"
                            }
                            messagesForApi.add(
                                ClaudeMessage(
                                    role = "user",
                                    content = summaryContext
                                )
                            )
                            messagesForApi.add(
                                ClaudeMessage(
                                    role = "assistant",
                                    content = "I understand the context from previous conversations."
                                )
                            )
                        }

                        // Add uncompressed messages
                        uncompressedMessages.forEach { msg ->
                            messagesForApi.add(
                                ClaudeMessage(role = "user", content = msg.user_message)
                            )
                            messagesForApi.add(
                                ClaudeMessage(role = "assistant", content = msg.assistant_message)
                            )
                        }

                        // Add current user message
                        messagesForApi.add(
                            ClaudeMessage(role = "user", content = request.message)
                        )

                        // Create Claude API request
                        val claudeRequest = ClaudeRequest(messages = messagesForApi)

                        // Call Claude API and measure time
                        var claudeResponse: ClaudeResponse
                        val responseTime = measureTimeMillis {
                            claudeResponse = claudeApiService.sendMessage(claudeRequest)
                        }

                        // Extract text from response
                        val responseText = claudeResponse.content
                            ?.firstOrNull { it.type == "text" }
                            ?.text ?: "No response"

                        // Calculate cost
                        val cost = calculateCost(
                            model = claudeResponse.model ?: "claude-sonnet-4-5-20250929",
                            inputTokens = claudeResponse.usage?.inputTokens ?: 0,
                            outputTokens = claudeResponse.usage?.outputTokens ?: 0
                        )

                        // Save message to database
                        val messageId = UUID.randomUUID().toString()
                        val currentTimestamp = System.currentTimeMillis()
                        chatRepository.saveMessage(
                            id = messageId,
                            userMessage = request.message,
                            assistantMessage = responseText,
                            claudeResponseJson = json.encodeToString(claudeResponse),
                            timestamp = currentTimestamp,
                            responseTimeMs = responseTime,
                            userId = userId
                        )

                        // Send user message first
                        val userMessage = ChatMessageDto(
                            id = "${messageId}_user",
                            content = request.message,
                            sender = MessageSender.USER,
                            timestamp = currentTimestamp - 1,
                            messageInfo = null
                        )

                        send(
                            Frame.Text(
                                json.encodeToString(
                                    ChatResponseDto.serializer(),
                                    ChatResponseDto.Data(message = userMessage)
                                )
                            )
                        )

                        // Create assistant message with info
                        val assistantMessage = ChatMessageDto(
                            id = messageId,
                            content = responseText,
                            sender = MessageSender.ASSISTANT,
                            timestamp = currentTimestamp,
                            messageInfo = MessageInfoDto(
                                inputTokens = claudeResponse.usage?.inputTokens ?: 0,
                                outputTokens = claudeResponse.usage?.outputTokens ?: 0,
                                responseTimeMs = responseTime,
                                model = claudeResponse.model ?: "unknown",
                                cost = cost
                            )
                        )

                        // Send assistant response
                        send(
                            Frame.Text(
                                json.encodeToString(
                                    ChatResponseDto.serializer(),
                                    ChatResponseDto.Data(message = assistantMessage)
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

private fun calculateCost(model: String, inputTokens: Int, outputTokens: Int): Double {
    // Prices per 1M tokens (as of 2025)
    val pricing = when {
        model.contains("sonnet-4") -> Pair(3.0, 15.0)  // Claude Sonnet 4.5: $3/$15 per 1M tokens
        model.contains("haiku-4") -> Pair(0.25, 1.25)  // Claude Haiku 4: $0.25/$1.25 per 1M tokens
        else -> Pair(3.0, 15.0)  // Default to Sonnet pricing
    }

    val inputCost = (inputTokens / 1_000_000.0) * pricing.first
    val outputCost = (outputTokens / 1_000_000.0) * pricing.second

    return inputCost + outputCost
}