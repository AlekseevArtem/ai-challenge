package ru.alekseev.myapplication.feature.settings.data.datasource

import ru.alekseev.myapplication.feature.settings.domain.entity.AppSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

interface SettingsLocalDataSource {
    fun observeSettings(): Flow<AppSettings>
    suspend fun getSettings(): AppSettings
    suspend fun saveSettings(settings: AppSettings)
}

class InMemorySettingsDataSource : SettingsLocalDataSource {
    private val settingsFlow = MutableStateFlow(AppSettings())

    override fun observeSettings(): Flow<AppSettings> {
        return settingsFlow.asStateFlow()
    }

    override suspend fun getSettings(): AppSettings {
        return settingsFlow.value
    }

    override suspend fun saveSettings(settings: AppSettings) {
        settingsFlow.value = settings
    }
}
