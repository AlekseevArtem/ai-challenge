package com.example.myapplication.feature_main.data.mapper

import com.example.myapplication.feature_main.domain.entity.ChatMessage
import com.example.myapplication.feature_main.domain.entity.MessageInfo
import ru.alekseev.myapplication.data.dto.ChatMessageDto
import ru.alekseev.myapplication.data.dto.MessageInfoDto
import ru.alekseev.myapplication.data.dto.MessageSender

// MessageInfo mappers
fun MessageInfoDto.toDomain(): MessageInfo {
    return MessageInfo(
        inputTokens = inputTokens,
        outputTokens = outputTokens,
        responseTimeMs = responseTimeMs,
        model = model,
        cost = cost
    )
}

fun MessageInfo.toDto(): MessageInfoDto {
    return MessageInfoDto(
        inputTokens = inputTokens,
        outputTokens = outputTokens,
        responseTimeMs = responseTimeMs,
        model = model,
        cost = cost
    )
}

// ChatMessage mappers
fun ChatMessageDto.toDomain(): ChatMessage {
    return ChatMessage(
        id = id,
        content = content,
        isFromUser = sender == MessageSender.USER,
        timestamp = timestamp,
        messageInfo = messageInfo?.toDomain()
    )
}

fun ChatMessage.toDto(): ChatMessageDto {
    return ChatMessageDto(
        id = id,
        content = content,
        sender = if (isFromUser) MessageSender.USER else MessageSender.ASSISTANT,
        timestamp = timestamp,
        messageInfo = messageInfo?.toDto()
    )
}