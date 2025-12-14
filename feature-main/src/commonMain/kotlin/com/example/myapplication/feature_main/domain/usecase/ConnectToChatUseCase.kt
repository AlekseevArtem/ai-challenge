package com.example.myapplication.feature_main.domain.usecase

import com.example.myapplication.feature_main.domain.repository.ChatRepository

class ConnectToChatUseCase(
    private val chatRepository: ChatRepository
) {
    suspend operator fun invoke() {
        chatRepository.connect()
    }
}