package com.example.myapplication.feature_main.data.datasource

import kotlinx.coroutines.flow.Flow
import ru.alekseev.myapplication.data.dto.ChatResponseDto

interface ChatWebSocketDataSource {

    suspend fun connect()

    suspend fun disconnect()

    suspend fun sendMessage(message: String)

    fun observeMessages(): Flow<ChatResponseDto>

    fun isConnected(): Boolean
}