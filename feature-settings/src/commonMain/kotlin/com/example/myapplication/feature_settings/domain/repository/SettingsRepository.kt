package com.example.myapplication.feature_settings.domain.repository

import com.example.myapplication.feature_settings.domain.entity.AppSettings
import ru.alekseev.myapplication.domain.entity.RagMode
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun observeSettings(): Flow<AppSettings>
    suspend fun getSettings(): AppSettings
    suspend fun updateRagMode(ragMode: RagMode)
}
