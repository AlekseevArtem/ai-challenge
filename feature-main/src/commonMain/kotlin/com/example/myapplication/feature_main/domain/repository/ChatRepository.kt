package com.example.myapplication.feature_main.domain.repository

import com.example.myapplication.feature_main.domain.entity.ChatMessageState
import ru.alekseev.myapplication.domain.entity.RagMode
import kotlinx.coroutines.flow.Flow

interface ChatRepository {

    suspend fun sendMessage(message: String, ragMode: RagMode = RagMode.Disabled)

    fun observeMessages(): Flow<Result<ChatMessageState>>

    suspend fun connect()

    suspend fun disconnect()
}