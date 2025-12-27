package ru.alekseev.myapplication.feature.main.domain.usecase

import ru.alekseev.myapplication.feature.main.domain.entity.ChatMessageState
import ru.alekseev.myapplication.feature.main.domain.repository.ChatConnectionRepository
import kotlinx.coroutines.flow.Flow

class ObserveMessagesUseCase(
    private val chatConnectionRepository: ChatConnectionRepository
) {
    operator fun invoke(): Flow<Result<ChatMessageState>> {
        return chatConnectionRepository.observeMessages()
    }
}