package com.example.myapplication.feature_main.domain.usecase

import com.example.myapplication.feature_main.domain.repository.ChatRepository

class SendMessageUseCase(
    private val chatRepository: ChatRepository
) {
    suspend operator fun invoke(message: String, useRag: Boolean = false) {
        if (message.isBlank()) {
            throw IllegalArgumentException("Message cannot be empty")
        }
        chatRepository.sendMessage(message, useRag)
    }
}