package ru.alekseev.myapplication.mapper

import kotlinx.serialization.json.Json
import ru.alekseev.myapplication.core.common.ChatConstants
import ru.alekseev.myapplication.core.common.ClaudeModels
import ru.alekseev.myapplication.core.common.ClaudePricing
import ru.alekseev.myapplication.data.dto.ChatMessageDto
import ru.alekseev.myapplication.data.dto.ClaudeResponse
import ru.alekseev.myapplication.data.dto.MessageInfoDto
import ru.alekseev.myapplication.data.dto.MessageSender
import ru.alekseev.myapplication.domain.model.Message as DomainMessage

/**
 * Converts a domain Message to a user ChatMessageDto.
 * The user message is sent before the assistant message, so it gets a slightly earlier timestamp.
 */
fun DomainMessage.toUserMessageDto(): ChatMessageDto {
    return ChatMessageDto(
        id = "${this.id.value}${ChatConstants.USER_MESSAGE_ID_SUFFIX}",
        content = this.userMessage,
        sender = MessageSender.USER,
        timestamp = this.timestamp.value - ChatConstants.USER_MESSAGE_TIMESTAMP_OFFSET,
        messageInfo = null
    )
}

/**
 * Converts a domain Message to an assistant ChatMessageDto with full message info.
 * Requires a Json instance to parse the stored Claude response.
 */
fun DomainMessage.toAssistantMessageDto(json: Json): ChatMessageDto {
    val claudeResponse = json.decodeFromString<ClaudeResponse>(this.claudeResponseJson)

    val cost = calculateCost(
        model = claudeResponse.model ?: ClaudeModels.DEFAULT_MODEL,
        inputTokens = claudeResponse.usage?.inputTokens ?: 0,
        outputTokens = claudeResponse.usage?.outputTokens ?: 0
    )

    return ChatMessageDto(
        id = this.id.value,
        content = this.assistantMessage,
        sender = MessageSender.ASSISTANT,
        timestamp = this.timestamp.value,
        messageInfo = MessageInfoDto(
            inputTokens = claudeResponse.usage?.inputTokens ?: 0,
            outputTokens = claudeResponse.usage?.outputTokens ?: 0,
            responseTimeMs = this.responseTimeMs.value,
            model = claudeResponse.model ?: "unknown",
            cost = cost
        )
    )
}

/**
 * Converts a domain Message to both user and assistant ChatMessageDto objects.
 * This is useful for loading chat history where we need both messages.
 */
fun DomainMessage.toBothMessageDtos(json: Json): Pair<ChatMessageDto, ChatMessageDto> {
    return Pair(
        toUserMessageDto(),
        toAssistantMessageDto(json)
    )
}

/**
 * Calculates the cost of a Claude API call based on model and token usage.
 * Returns the total cost in dollars.
 */
fun calculateCost(model: String, inputTokens: Int, outputTokens: Int): Double {
    val pricing = when {
        model.contains("sonnet-4") -> ClaudePricing.SONNET_4_PRICING
        model.contains("haiku-4") -> ClaudePricing.HAIKU_4_PRICING
        else -> ClaudePricing.DEFAULT_PRICING
    }

    val inputCost = (inputTokens / ClaudePricing.TOKENS_PER_MILLION) * pricing.first
    val outputCost = (outputTokens / ClaudePricing.TOKENS_PER_MILLION) * pricing.second

    return inputCost + outputCost
}

/**
 * Creates a MessageInfoDto from Claude response and timing information.
 */
fun createMessageInfo(
    claudeResponse: ClaudeResponse,
    responseTimeMs: Long
): MessageInfoDto {
    val cost = calculateCost(
        model = claudeResponse.model ?: ClaudeModels.DEFAULT_MODEL,
        inputTokens = claudeResponse.usage?.inputTokens ?: 0,
        outputTokens = claudeResponse.usage?.outputTokens ?: 0
    )

    return MessageInfoDto(
        inputTokens = claudeResponse.usage?.inputTokens ?: 0,
        outputTokens = claudeResponse.usage?.outputTokens ?: 0,
        responseTimeMs = responseTimeMs,
        model = claudeResponse.model ?: "unknown",
        cost = cost
    )
}
