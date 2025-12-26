package ru.alekseev.myapplication.feature.main.domain.entity

data class ChatMessage(
    val id: String,
    val content: String,
    val isFromUser: Boolean,
    val timestamp: Long,
    val messageInfo: MessageInfo? = null,
    val usedRag: Boolean? = null
)

sealed interface ChatMessageState {

    data object Loading : ChatMessageState

    data class Data(val message: ChatMessage) : ChatMessageState

    data class History(val messages: List<ChatMessage>) : ChatMessageState

    data class Alert(val alert: AlertEntity) : ChatMessageState
}