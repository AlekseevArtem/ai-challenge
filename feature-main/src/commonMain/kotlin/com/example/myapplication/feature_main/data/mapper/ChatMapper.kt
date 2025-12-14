package com.example.myapplication.feature_main.data.mapper

import com.example.myapplication.feature_main.domain.entity.ChatMessage
import ru.alekseev.myapplication.dto.ChatMessageDto
import ru.alekseev.myapplication.dto.MessageSender

fun ChatMessageDto.toEntity(): ChatMessage {
    return ChatMessage(
        id = id,
        content = content,
        isFromUser = sender == MessageSender.USER,
        timestamp = timestamp,
        messageInfo = messageInfo
    )
}

fun ChatMessage.toDto(): ChatMessageDto {
    return ChatMessageDto(
        id = id,
        content = content,
        sender = if (isFromUser) MessageSender.USER else MessageSender.ASSISTANT,
        timestamp = timestamp,
        messageInfo = messageInfo
    )
}