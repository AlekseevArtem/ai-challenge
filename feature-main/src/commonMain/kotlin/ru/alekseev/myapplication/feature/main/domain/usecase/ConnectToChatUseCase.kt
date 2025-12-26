package ru.alekseev.myapplication.feature.main.domain.usecase

import ru.alekseev.myapplication.feature.main.domain.repository.ChatRepository

class ConnectToChatUseCase(
    private val chatRepository: ChatRepository
) {
    suspend operator fun invoke() {
        chatRepository.connect()
    }
}