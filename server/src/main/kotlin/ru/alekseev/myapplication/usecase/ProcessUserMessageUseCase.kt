package ru.alekseev.myapplication.usecase

import kotlinx.serialization.json.Json
import ru.alekseev.myapplication.data.dto.*
import ru.alekseev.myapplication.domain.entity.RagMode
import ru.alekseev.myapplication.domain.model.*
import ru.alekseev.myapplication.domain.rag.SimilarityThresholdFilter
import ru.alekseev.myapplication.mapper.createMessageInfo
import ru.alekseev.myapplication.repository.ChatRepository
import ru.alekseev.myapplication.service.ClaudeApiService
import ru.alekseev.myapplication.service.DocumentRAGService
import java.util.UUID
import kotlin.system.measureTimeMillis

/**
 * Result of processing a user message, containing domain message and metadata.
 */
data class ProcessMessageResult(
    val message: Message,
    val messageInfo: MessageInfoDto,
    val usedRag: Boolean
)

/**
 * Use case for processing a user message through the Claude API.
 * Handles building context from summaries and uncompressed messages,
 * calling Claude API, and saving the result.
 *
 * Works with domain entities, not DTOs or database entities.
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
     * 4. Returning domain Message entity
     *
     * @param userMessageText The message text from the user
     * @param userId The user identifier (domain value object)
     * @param ragMode The RAG mode to use (Disabled, Enabled, or EnabledWithFiltering)
     * @return ProcessMessageResult containing domain Message and metadata
     */
    suspend operator fun invoke(
        userMessageText: String,
        userId: UserId = UserId.DEFAULT,
        ragMode: RagMode = RagMode.Disabled
    ): ProcessMessageResult {
        // Build message history for Claude API
        val messagesForApi = buildMessageHistory(userId, userMessageText, ragMode)

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
        val messageId = MessageId(UUID.randomUUID().toString())
        val currentTimestamp = Timestamp(System.currentTimeMillis())
        val responseTimeValue = ResponseTimeMs(responseTime)

        chatRepository.saveMessage(
            id = messageId,
            userMessage = userMessageText,
            assistantMessage = responseText,
            claudeResponseJson = json.encodeToString(claudeResponse),
            timestamp = currentTimestamp,
            responseTimeMs = responseTimeValue,
            userId = userId
        )

        // Create domain Message entity
        val domainMessage = Message(
            id = messageId,
            userMessage = userMessageText,
            assistantMessage = responseText,
            claudeResponseJson = json.encodeToString(claudeResponse),
            timestamp = currentTimestamp,
            responseTimeMs = responseTimeValue,
            isCompressed = false,
            userId = userId
        )

        // Create message info
        val messageInfo = createMessageInfo(claudeResponse, responseTime)

        return ProcessMessageResult(
            message = domainMessage,
            messageInfo = messageInfo,
            usedRag = ragMode !is RagMode.Disabled
        )
    }

    /**
     * Builds the message history for Claude API from summaries and uncompressed messages.
     * Summaries are added as context at the beginning of the conversation.
     * Also includes RAG context from relevant documents based on the RAG mode.
     *
     * Now works with domain entities (Message, Summary) instead of database entities.
     */
    private suspend fun buildMessageHistory(userId: UserId, currentMessage: String, ragMode: RagMode): List<ClaudeMessage> {
        val summaries = chatRepository.getAllSummaries(userId)
        val uncompressedMessages = chatRepository.getUncompressedMessages(userId)

        val messagesForApi = mutableListOf<ClaudeMessage>()

        // Add summaries as system context
        if (summaries.isNotEmpty()) {
            val summaryContext = summaries.joinToString("\n\n") {
                "Previous conversation summary: ${it.summaryText}"
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
                ClaudeMessage(role = "user", content = ClaudeMessageContent.Text(msg.userMessage))
            )
            messagesForApi.add(
                ClaudeMessage(role = "assistant", content = ClaudeMessageContent.Text(msg.assistantMessage))
            )
        }

        // Add RAG context from document search based on mode
        when (ragMode) {
            is RagMode.Disabled -> {
                // No RAG context
            }
            is RagMode.Enabled -> {
                // RAG without filtering
                if (documentRAGService.isReady()) {
                    println("[ProcessUserMessageUseCase] RAG enabled without filtering")
                    val ragContext = documentRAGService.getContextForQuery(currentMessage, topK = 3, filter = null)
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
            }
            is RagMode.EnabledWithFiltering -> {
                // RAG with similarity threshold filtering
                if (documentRAGService.isReady()) {
                    println("[ProcessUserMessageUseCase] RAG enabled with filtering (threshold: ${ragMode.threshold})")
                    val filter = SimilarityThresholdFilter(ragMode.threshold)
                    val ragContext = documentRAGService.getContextForQuery(currentMessage, topK = 3, filter = filter)
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
                                content = ClaudeMessageContent.Text("I understand. I'll use this filtered context from the project codebase to answer your question.")
                            )
                        )
                    } else {
                        println("[ProcessUserMessageUseCase] No chunks passed the similarity threshold, proceeding without RAG context")
                    }
                }
            }
        }

        // Add current user message
        messagesForApi.add(
            ClaudeMessage(role = "user", content = ClaudeMessageContent.Text(currentMessage))
        )

        return messagesForApi
    }
}
