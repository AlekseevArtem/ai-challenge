package ru.alekseev.myapplication.feature.main.domain.repository

import ru.alekseev.myapplication.feature.main.domain.entity.ChatMessageState
import ru.alekseev.myapplication.domain.entity.RagMode
import kotlinx.coroutines.flow.Flow

/**
 * Repository for managing WebSocket chat connections.
 * Renamed from ChatRepository to avoid collision with server-side ChatRepository
 * which handles database operations.
 */
interface ChatConnectionRepository {

    suspend fun sendMessage(message: String, ragMode: RagMode = RagMode.Disabled)

    fun observeMessages(): Flow<Result<ChatMessageState>>

    suspend fun connect()

    suspend fun disconnect()
}