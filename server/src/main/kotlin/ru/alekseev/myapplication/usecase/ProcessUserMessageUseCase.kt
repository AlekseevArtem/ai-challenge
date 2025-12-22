package ru.alekseev.myapplication.usecase

import kotlinx.serialization.json.Json
import ru.alekseev.myapplication.core.common.ChatConstants
import ru.alekseev.myapplication.data.dto.*
import ru.alekseev.myapplication.mapper.createMessageInfo
import ru.alekseev.myapplication.repository.ChatRepository
import ru.alekseev.myapplication.service.ClaudeApiService
import ru.alekseev.myapplication.service.DocumentRAGService
import java.util.UUID
import kotlin.system.measureTimeMillis

/**
 * Result of processing a user message, containing both user and assistant messages to send to client
 */
data class ProcessMessageResult(
    val userMessage: ChatMessageDto,
    val assistantMessage: ChatMessageDto
)

/**
 * Use case for processing a user message through the Claude API.
 * Handles building context from summaries and uncompressed messages,
 * calling Claude API, and saving the result.
 */
class ProcessUserMessageUseCase(
    private val chatRepository: ChatRepository,
    private val claudeApiService: ClaudeApiService,
    private val documentRAGService: DocumentRAGService,
    private val json: Json
) {
    /**
     * Processes a user message by:
     * 1. Building message history from summaries and uncompressed messages
     * 2. Calling Claude API with the full context
     * 3. Saving the conversation to database
     * 4. Creating DTOs for the response
     *
     * @param userMessageText The message text from the user
     * @param userId The user identifier
     * @return ProcessMessageResult containing both user and assistant message DTOs
     */
    suspend operator fun invoke(
        userMessageText: String,
        userId: String = ChatConstants.DEFAULT_USER_ID
    ): ProcessMessageResult {
        // Build message history for Claude API
        val messagesForApi = buildMessageHistory(userId, userMessageText)

        // Create Claude API request
        val claudeRequest = ClaudeRequest(messages = messagesForApi)

        // Call Claude API and measure time
        val claudeResponse: ClaudeResponse
        val responseTime = measureTimeMillis {
            claudeResponse = claudeApiService.sendMessage(claudeRequest)
        }

        // Extract text from response
        val responseText = claudeResponse.content
            ?.firstOrNull { it.type == "text" }
            ?.text ?: "No response"

        // Save to database
        val messageId = UUID.randomUUID().toString()
        val currentTimestamp = System.currentTimeMillis()
        chatRepository.saveMessage(
            id = messageId,
            userMessage = userMessageText,
            assistantMessage = responseText,
            claudeResponseJson = json.encodeToString(claudeResponse),
            timestamp = currentTimestamp,
            responseTimeMs = responseTime,
            userId = userId
        )

        // Create DTOs for response
        val userMessage = ChatMessageDto(
            id = "${messageId}${ChatConstants.USER_MESSAGE_ID_SUFFIX}",
            content = userMessageText,
            sender = MessageSender.USER,
            timestamp = currentTimestamp - ChatConstants.USER_MESSAGE_TIMESTAMP_OFFSET,
            messageInfo = null
        )

        val assistantMessage = ChatMessageDto(
            id = messageId,
            content = responseText,
            sender = MessageSender.ASSISTANT,
            timestamp = currentTimestamp,
            messageInfo = createMessageInfo(claudeResponse, responseTime)
        )

        return ProcessMessageResult(userMessage, assistantMessage)
    }

    /**
     * Builds the message history for Claude API from summaries and uncompressed messages.
     * Summaries are added as context at the beginning of the conversation.
     * Also includes RAG context from relevant documents.
     */
    private suspend fun buildMessageHistory(userId: String, currentMessage: String): List<ClaudeMessage> {
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
                    content = ClaudeMessageContent.Text(summaryContext)
                )
            )
            messagesForApi.add(
                ClaudeMessage(
                    role = "assistant",
                    content = ClaudeMessageContent.Text("I understand the context from previous conversations.")
                )
            )
        }

        // Add uncompressed messages
        uncompressedMessages.forEach { msg ->
            messagesForApi.add(
                ClaudeMessage(role = "user", content = ClaudeMessageContent.Text(msg.user_message))
            )
            messagesForApi.add(
                ClaudeMessage(role = "assistant", content = ClaudeMessageContent.Text(msg.assistant_message))
            )
        }

        // Add RAG context from document search
        if (documentRAGService.isReady()) {
            val ragContext = documentRAGService.getContextForQuery(currentMessage, topK = 3)
            if (ragContext.isNotBlank()) {
                messagesForApi.add(
                    ClaudeMessage(
                        role = "user",
                        content = ClaudeMessageContent.Text(ragContext)
                    )
                )
                messagesForApi.add(
                    ClaudeMessage(
                        role = "assistant",
                        content = ClaudeMessageContent.Text("I understand. I'll use this context from the project codebase to answer your question.")
                    )
                )
            }
        }

        // Add current user message
        messagesForApi.add(
            ClaudeMessage(role = "user", content = ClaudeMessageContent.Text(currentMessage))
        )

        return messagesForApi
    }
}
