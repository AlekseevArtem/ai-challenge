package com.example.myapplication.feature_main.domain.entity

import ru.alekseev.myapplication.dto.MessageInfoDto

data class ChatMessage(
    val id: String,
    val content: String,
    val isFromUser: Boolean,
    val timestamp: Long,
    val messageInfo: MessageInfoDto? = null
)

sealed interface ChatMessageState {

    data object Loading : ChatMessageState

    data class Data(val message: ChatMessage) : ChatMessageState

    data class History(val messages: List<ChatMessage>) : ChatMessageState
}