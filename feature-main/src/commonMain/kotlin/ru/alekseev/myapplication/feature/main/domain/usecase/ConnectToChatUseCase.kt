package ru.alekseev.myapplication.feature.main.domain.usecase

import ru.alekseev.myapplication.feature.main.domain.repository.ChatConnectionRepository

class ConnectToChatUseCase(
    private val chatConnectionRepository: ChatConnectionRepository
) {
    suspend operator fun invoke() {
        chatConnectionRepository.connect()
    }
}