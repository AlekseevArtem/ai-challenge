package ru.alekseev.myapplication.data.datasource

import kotlinx.coroutines.flow.Flow
import ru.alekseev.myapplication.data.dto.ChatResponseDto

interface ChatWebSocketDataSource {

    suspend fun connect()

    suspend fun disconnect()

    suspend fun sendMessage(message: String, useRag: Boolean = false)

    fun observeMessages(): Flow<ChatResponseDto>

    fun isConnected(): Boolean
}
