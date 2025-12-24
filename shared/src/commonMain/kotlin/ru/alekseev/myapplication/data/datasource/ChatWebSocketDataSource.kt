package ru.alekseev.myapplication.data.datasource

import ru.alekseev.myapplication.domain.entity.RagMode
import kotlinx.coroutines.flow.Flow
import ru.alekseev.myapplication.data.dto.ChatResponseDto

interface ChatWebSocketDataSource {

    suspend fun connect()

    suspend fun disconnect()

    suspend fun sendMessage(message: String, ragMode: RagMode = RagMode.Disabled)

    fun observeMessages(): Flow<ChatResponseDto>

    fun isConnected(): Boolean
}
