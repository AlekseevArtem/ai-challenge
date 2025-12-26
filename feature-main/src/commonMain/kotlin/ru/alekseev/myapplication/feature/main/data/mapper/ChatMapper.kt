package ru.alekseev.myapplication.feature.main.data.mapper

import ru.alekseev.myapplication.feature.main.domain.entity.AlertEntity
import ru.alekseev.myapplication.feature.main.domain.entity.AlertSeverity
import ru.alekseev.myapplication.feature.main.domain.entity.ChatMessage
import ru.alekseev.myapplication.feature.main.domain.entity.MessageInfo
import ru.alekseev.myapplication.data.dto.AlertSeverityDto
import ru.alekseev.myapplication.data.dto.ChatMessageDto
import ru.alekseev.myapplication.data.dto.MessageInfoDto
import ru.alekseev.myapplication.data.dto.MessageSender
import ru.alekseev.myapplication.data.dto.UserAlertDto

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
        messageInfo = messageInfo?.toDomain(),
        usedRag = usedRag
    )
}

fun ChatMessage.toDto(): ChatMessageDto {
    return ChatMessageDto(
        id = id,
        content = content,
        sender = if (isFromUser) MessageSender.USER else MessageSender.ASSISTANT,
        timestamp = timestamp,
        messageInfo = messageInfo?.toDto(),
        usedRag = usedRag
    )
}

fun UserAlertDto.toDomain(): AlertEntity {
    return AlertEntity(
        id = id,
        title = title,
        message = message,
        severity = when (severity) {
            AlertSeverityDto.INFO -> AlertSeverity.INFO
            AlertSeverityDto.WARNING -> AlertSeverity.WARNING
            AlertSeverityDto.ERROR -> AlertSeverity.ERROR
            AlertSeverityDto.SUCCESS -> AlertSeverity.SUCCESS
        },
        timestamp = timestamp,
        category = category,
        actionLabel = actionLabel,
        actionData = actionData
    )
}

fun AlertEntity.toDto(): UserAlertDto {
    return UserAlertDto(
        id = id,
        title = title,
        message = message,
        severity = when (severity) {
            AlertSeverity.INFO -> AlertSeverityDto.INFO
            AlertSeverity.WARNING -> AlertSeverityDto.WARNING
            AlertSeverity.ERROR -> AlertSeverityDto.ERROR
            AlertSeverity.SUCCESS -> AlertSeverityDto.SUCCESS
        },
        timestamp = timestamp,
        category = category,
        actionLabel = actionLabel,
        actionData = actionData
    )
}