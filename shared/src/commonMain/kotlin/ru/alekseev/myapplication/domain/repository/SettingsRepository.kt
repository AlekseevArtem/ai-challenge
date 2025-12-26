package ru.alekseev.myapplication.domain.repository

import ru.alekseev.myapplication.domain.entity.AppSettings
import ru.alekseev.myapplication.domain.entity.RagMode
import kotlinx.coroutines.flow.Flow

/**
 * Repository for managing application settings.
 * This is a shared domain interface that can be implemented by feature modules.
 */
interface SettingsRepository {
    /**
     * Observe settings changes as a Flow.
     * Emits the current settings immediately and then emits whenever settings change.
     */
    fun observeSettings(): Flow<AppSettings>

    /**
     * Get the current settings (suspending call).
     */
    suspend fun getSettings(): AppSettings

    /**
     * Update the RAG mode setting.
     */
    suspend fun updateRagMode(ragMode: RagMode)
}
