package com.example.myapplication.feature_main.domain.usecase

import com.example.myapplication.feature_main.domain.entity.ChatMessageState
import com.example.myapplication.feature_main.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow

class ObserveMessagesUseCase(
    private val chatRepository: ChatRepository
) {
    operator fun invoke(): Flow<Result<ChatMessageState>> {
        return chatRepository.observeMessages()
    }
}