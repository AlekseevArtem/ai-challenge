package ru.alekseev.myapplication.feature.main.domain.usecase

import ru.alekseev.myapplication.feature.main.domain.repository.ChatConnectionRepository
import ru.alekseev.myapplication.domain.entity.RagMode

class SendMessageUseCase(
    private val chatConnectionRepository: ChatConnectionRepository
) {
    suspend operator fun invoke(message: String, ragMode: RagMode = RagMode.Disabled) {
        if (message.isBlank()) {
            throw IllegalArgumentException("Message cannot be empty")
        }
        chatConnectionRepository.sendMessage(message, ragMode)
    }
}