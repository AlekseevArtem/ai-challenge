package ru.alekseev.myapplication.mapper

import kotlinx.serialization.json.Json
import ru.alekseev.myapplication.core.common.ChatConstants
import ru.alekseev.myapplication.core.common.ClaudeModels
import ru.alekseev.myapplication.core.common.ClaudePricing
import ru.alekseev.myapplication.data.dto.ChatMessageDto
import ru.alekseev.myapplication.data.dto.ClaudeResponse
import ru.alekseev.myapplication.data.dto.MessageInfoDto
import ru.alekseev.myapplication.data.dto.MessageSender
import ru.alekseev.myapplication.db.Message

/**
 * Converts a database Message to a user ChatMessageDto.
 * The user message is sent before the assistant message, so it gets a slightly earlier timestamp.
 */
fun Message.toUserMessageDto(): ChatMessageDto {
    return ChatMessageDto(
        id = "${this.id}${ChatConstants.USER_MESSAGE_ID_SUFFIX}",
        content = this.user_message,
        sender = MessageSender.USER,
        timestamp = this.timestamp - ChatConstants.USER_MESSAGE_TIMESTAMP_OFFSET,
        messageInfo = null
    )
}

/**
 * Converts a database Message to an assistant ChatMessageDto with full message info.
 * Requires a Json instance to parse the stored Claude response.
 */
fun Message.toAssistantMessageDto(json: Json): ChatMessageDto {
    val claudeResponse = json.decodeFromString<ClaudeResponse>(this.claude_response_json)

    val cost = calculateCost(
        model = claudeResponse.model ?: ClaudeModels.DEFAULT_MODEL,
        inputTokens = claudeResponse.usage?.inputTokens ?: 0,
        outputTokens = claudeResponse.usage?.outputTokens ?: 0
    )

    return ChatMessageDto(
        id = this.id,
        content = this.assistant_message,
        sender = MessageSender.ASSISTANT,
        timestamp = this.timestamp,
        messageInfo = MessageInfoDto(
            inputTokens = claudeResponse.usage?.inputTokens ?: 0,
            outputTokens = claudeResponse.usage?.outputTokens ?: 0,
            responseTimeMs = this.response_time_ms,
            model = claudeResponse.model ?: "unknown",
            cost = cost
        )
    )
}

/**
 * Converts a database Message to both user and assistant ChatMessageDto objects.
 * This is useful for loading chat history where we need both messages.
 */
fun Message.toBothMessageDtos(json: Json): Pair<ChatMessageDto, ChatMessageDto> {
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
