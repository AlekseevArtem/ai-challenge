@file:OptIn(ExperimentalTime::class)

package ru.alekseev.myapplication.core.network.dto

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * DTO for communication between frontend and backend via WebSocket
 * This DTO is used both on client and server side
 */
@Serializable
data class ChatMessageDto(
    val id: String,
    val content: String,
    val sender: MessageSender,
    val timestamp: Long = Clock.System.now().toEpochMilliseconds(),
    val messageInfo: MessageInfoDto? = null
)

@Serializable
enum class MessageSender {
    USER,
    ASSISTANT
}

@Serializable
data class ChatRequestDto(
    val message: String
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("type")
sealed interface ChatResponseDto {
    @Serializable
    @SerialName("loading")
    data object Loading : ChatResponseDto

    @Serializable
    @SerialName("data")
    data class Data(val message: ChatMessageDto) : ChatResponseDto

    @Serializable
    @SerialName("error")
    data class Error(val error: String) : ChatResponseDto

    @Serializable
    @SerialName("history")
    data class History(val messages: List<ChatMessageDto>) : ChatResponseDto
}

@Serializable
data class MessageInfoDto(
    val inputTokens: Int,
    val outputTokens: Int,
    val responseTimeMs: Long,
    val model: String,
    val cost: Double
)