package ru.alekseev.myapplication.mapper

import ru.alekseev.myapplication.core.common.ChatConstants
import ru.alekseev.myapplication.data.dto.ChatMessageDto
import ru.alekseev.myapplication.data.dto.MessageInfoDto
import ru.alekseev.myapplication.data.dto.MessageSender
import ru.alekseev.myapplication.domain.model.Message

/**
 * Mappers for converting domain models to DTOs.
 * Domain models are internal server representations.
 * DTOs are wire format for client-server communication.
 */

/**
 * Convert domain Message to user ChatMessageDto.
 * User messages are sent by the user to the assistant.
 */
fun Message.toUserDto(): ChatMessageDto {
    return ChatMessageDto(
        id = "${this.id.value}${ChatConstants.USER_MESSAGE_ID_SUFFIX}",
        content = this.userMessage,
        sender = MessageSender.USER,
        timestamp = this.timestamp.value - ChatConstants.USER_MESSAGE_TIMESTAMP_OFFSET,
        messageInfo = null
    )
}

/**
 * Convert domain Message to assistant ChatMessageDto.
 * Assistant messages are responses from Claude.
 */
fun Message.toAssistantDto(
    messageInfo: MessageInfoDto?,
    usedRag: Boolean
): ChatMessageDto {
    return ChatMessageDto(
        id = this.id.value,
        content = this.assistantMessage,
        sender = MessageSender.ASSISTANT,
        timestamp = this.timestamp.value,
        messageInfo = messageInfo,
        usedRag = usedRag
    )
}

/**
 * Convert domain Message to both user and assistant ChatMessageDtos.
 * Returns a pair: (userMessage, assistantMessage).
 *
 * This overload is used when you have messageInfo and usedRag already.
 */
fun Message.toMessagePairDtos(
    messageInfo: MessageInfoDto?,
    usedRag: Boolean
): Pair<ChatMessageDto, ChatMessageDto> {
    return Pair(
        this.toUserDto(),
        this.toAssistantDto(messageInfo, usedRag)
    )
}

/**
 * Convert domain Message to both user and assistant ChatMessageDtos.
 * Returns a pair: (userMessage, assistantMessage).
 *
 * This overload parses the Claude response from the message itself.
 */
fun Message.toMessagePairDtos(json: kotlinx.serialization.json.Json): Pair<ChatMessageDto, ChatMessageDto> {
    val claudeResponse = json.decodeFromString<ru.alekseev.myapplication.data.dto.ClaudeResponse>(this.claudeResponseJson)

    val messageInfo = ru.alekseev.myapplication.mapper.createMessageInfo(
        claudeResponse = claudeResponse,
        responseTimeMs = this.responseTimeMs.value
    )

    return this.toMessagePairDtos(messageInfo, usedRag = false)
}
