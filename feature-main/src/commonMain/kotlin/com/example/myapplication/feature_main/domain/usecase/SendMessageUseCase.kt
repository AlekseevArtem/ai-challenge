package com.example.myapplication.feature_main.domain.usecase

import com.example.myapplication.feature_main.domain.repository.ChatRepository
import ru.alekseev.myapplication.domain.entity.RagMode

class SendMessageUseCase(
    private val chatRepository: ChatRepository
) {
    suspend operator fun invoke(message: String, ragMode: RagMode = RagMode.Disabled) {
        if (message.isBlank()) {
            throw IllegalArgumentException("Message cannot be empty")
        }
        chatRepository.sendMessage(message, ragMode)
    }
}