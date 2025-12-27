package ru.alekseev.myapplication.usecase

import kotlinx.serialization.json.Json
import ru.alekseev.myapplication.core.common.ClaudePricing
import ru.alekseev.myapplication.data.dto.*
import ru.alekseev.myapplication.domain.entity.RagMode
import ru.alekseev.myapplication.domain.gateway.ClaudeGateway
import ru.alekseev.myapplication.domain.model.*
import ru.alekseev.myapplication.domain.observability.ConversationMetricsCollector
import ru.alekseev.myapplication.mapper.createMessageInfo
import ru.alekseev.myapplication.repository.ChatRepository
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
 * Orchestrates the high-level flow:
 * 1. Build message history (delegated to MessageHistoryBuilder)
 * 2. Call Claude API
 * 3. Save conversation to database
 * 4. Return domain result
 *
 * Works with domain entities and depends on domain interfaces (ports),
 * not concrete service implementations.
 *
 * Integrated with ConversationMetricsCollector for observability.
 */
class ProcessUserMessageUseCase(
    private val chatRepository: ChatRepository,
    private val claudeGateway: ClaudeGateway,
    private val messageHistoryBuilder: MessageHistoryBuilder,
    private val json: Json,
    private val conversationMetrics: ConversationMetricsCollector
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
        // Build message history for Claude API (delegated to MessageHistoryBuilder)
        val messagesForApi = messageHistoryBuilder.buildMessageHistory(userId, userMessageText, ragMode)

        // Create Claude API request
        val claudeRequest = ClaudeRequest(messages = messagesForApi)

        // Call Claude API and measure time
        val claudeResponse: ClaudeResponse
        val responseTime = measureTimeMillis {
            claudeResponse = claudeGateway.sendMessage(claudeRequest)
        }

        // Extract text from response
        val responseText = claudeResponse.content
            ?.firstOrNull { it.type == "text" }
            ?.text ?: "No response"

        // Record token usage metrics
        val inputTokens = claudeResponse.usage?.inputTokens ?: 0
        val outputTokens = claudeResponse.usage?.outputTokens ?: 0
        val model = claudeResponse.model ?: "unknown"
        val cost = calculateCost(inputTokens, outputTokens, model)

        conversationMetrics.recordTokenUsage(inputTokens, outputTokens, model, cost)

        // Count tool calls in the conversation
        val toolCallsCount = claudeResponse.content?.count { it.type == "tool_use" } ?: 0

        // Record overall message processing
        conversationMetrics.recordMessageProcessed(
            userId = userId.value,
            totalDurationMs = responseTime,
            ragUsed = ragMode !is RagMode.Disabled,
            toolCallsCount = toolCallsCount
        )

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
     * Calculate cost based on token usage and model pricing.
     */
    private fun calculateCost(inputTokens: Int, outputTokens: Int, model: String): Double {
        val (inputCostPer1M, outputCostPer1M) = when {
            model.contains("sonnet") -> ClaudePricing.SONNET_4_PRICING
            model.contains("haiku") -> ClaudePricing.HAIKU_4_PRICING
            else -> Pair(0.0, 0.0) // Unknown model
        }

        val inputCost = (inputTokens / 1_000_000.0) * inputCostPer1M
        val outputCost = (outputTokens / 1_000_000.0) * outputCostPer1M

        return inputCost + outputCost
    }
}
