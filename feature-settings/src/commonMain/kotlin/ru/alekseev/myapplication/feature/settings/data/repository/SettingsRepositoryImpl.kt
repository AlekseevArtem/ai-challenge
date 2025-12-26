package ru.alekseev.myapplication.feature.settings.data.repository

import ru.alekseev.myapplication.feature.settings.data.datasource.SettingsLocalDataSource
import ru.alekseev.myapplication.domain.entity.AppSettings
import ru.alekseev.myapplication.domain.entity.RagMode
import ru.alekseev.myapplication.domain.repository.SettingsRepository
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

    override suspend fun updateRagMode(ragMode: RagMode) {
        val currentSettings = localDataSource.getSettings()
        val updatedSettings = currentSettings.copy(ragMode = ragMode)
        localDataSource.saveSettings(updatedSettings)
    }
}
