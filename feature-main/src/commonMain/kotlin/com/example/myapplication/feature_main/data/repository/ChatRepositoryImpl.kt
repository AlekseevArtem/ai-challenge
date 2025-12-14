package com.example.myapplication.feature_main.data.repository

import com.example.myapplication.feature_main.data.datasource.ChatWebSocketDataSource
import com.example.myapplication.feature_main.data.mapper.toEntity
import com.example.myapplication.feature_main.domain.entity.ChatMessageState
import com.example.myapplication.feature_main.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.alekseev.myapplication.dto.ChatResponseDto

class ChatRepositoryImpl(
    private val webSocketDataSource: ChatWebSocketDataSource
) : ChatRepository {

    override suspend fun sendMessage(message: String) {
        webSocketDataSource.sendMessage(message)
    }

    override fun observeMessages(): Flow<Result<ChatMessageState>> {
        return webSocketDataSource.observeMessages().map { response ->
            when (response) {
                is ChatResponseDto.Error -> Result.failure(Exception(response.error))
                is ChatResponseDto.Data -> Result.success(
                    ChatMessageState.Data(response.message.toEntity())
                )
                is ChatResponseDto.Loading -> Result.success(ChatMessageState.Loading)
                is ChatResponseDto.History -> Result.success(
                    ChatMessageState.History(response.messages.map { it.toEntity() })
                )
            }
        }
    }

    override suspend fun connect() {
        webSocketDataSource.connect()
    }

    override suspend fun disconnect() {
        webSocketDataSource.disconnect()
    }
}