package com.example.myapplication.feature_main.data.repository

import com.example.myapplication.feature_main.data.mapper.toDomain
import com.example.myapplication.feature_main.domain.entity.ChatMessageState
import com.example.myapplication.feature_main.domain.repository.ChatRepository
import com.example.myapplication.feature_main.domain.usecase.DispatchAlertUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.alekseev.myapplication.data.datasource.ChatWebSocketDataSource
import ru.alekseev.myapplication.data.dto.ChatResponseDto

class ChatRepositoryImpl(
    private val webSocketDataSource: ChatWebSocketDataSource,
    private val dispatchAlertUseCase: DispatchAlertUseCase,
) : ChatRepository {

    override suspend fun sendMessage(message: String) {
        webSocketDataSource.sendMessage(message)
    }

    override fun observeMessages(): Flow<Result<ChatMessageState>> {
        return webSocketDataSource.observeMessages().map { response ->
            when (response) {
                is ChatResponseDto.Error -> Result.failure(Exception(response.error))
                is ChatResponseDto.Data -> Result.success(
                    ChatMessageState.Data(response.message.toDomain())
                )

                is ChatResponseDto.Loading -> Result.success(ChatMessageState.Loading)
                is ChatResponseDto.History -> Result.success(
                    ChatMessageState.History(response.messages.map { it.toDomain() })
                )

                is ChatResponseDto.Alert -> {
                    val domainAlert = response.alert.toDomain()
                    dispatchAlertUseCase(domainAlert)
                    Result.success(ChatMessageState.Alert(domainAlert))
                }
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