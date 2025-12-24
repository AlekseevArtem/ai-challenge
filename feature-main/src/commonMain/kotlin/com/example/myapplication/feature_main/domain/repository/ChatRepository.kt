package com.example.myapplication.feature_main.domain.repository

import com.example.myapplication.feature_main.domain.entity.ChatMessageState
import kotlinx.coroutines.flow.Flow

interface ChatRepository {

    suspend fun sendMessage(message: String, useRag: Boolean = false)

    fun observeMessages(): Flow<Result<ChatMessageState>>

    suspend fun connect()

    suspend fun disconnect()
}