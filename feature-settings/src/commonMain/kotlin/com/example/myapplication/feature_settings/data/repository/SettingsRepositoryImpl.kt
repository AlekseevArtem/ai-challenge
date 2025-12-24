package com.example.myapplication.feature_settings.data.repository

import com.example.myapplication.feature_settings.data.datasource.SettingsLocalDataSource
import com.example.myapplication.feature_settings.domain.entity.AppSettings
import com.example.myapplication.feature_settings.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow

class SettingsRepositoryImpl(
    private val localDataSource: SettingsLocalDataSource
) : SettingsRepository {

    override fun observeSettings(): Flow<AppSettings> {
        return localDataSource.observeSettings()
    }

    override suspend fun getSettings(): AppSettings {
        return localDataSource.getSettings()
    }

    override suspend fun updateRagEnabled(enabled: Boolean) {
        val currentSettings = localDataSource.getSettings()
        val updatedSettings = currentSettings.copy(ragEnabled = enabled)
        localDataSource.saveSettings(updatedSettings)
    }
}
