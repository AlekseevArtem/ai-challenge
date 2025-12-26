package ru.alekseev.myapplication.feature.main.domain.usecase

import ru.alekseev.myapplication.feature.main.domain.entity.ChatMessageState
import ru.alekseev.myapplication.feature.main.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow

class ObserveMessagesUseCase(
    private val chatRepository: ChatRepository
) {
    operator fun invoke(): Flow<Result<ChatMessageState>> {
        return chatRepository.observeMessages()
    }
}