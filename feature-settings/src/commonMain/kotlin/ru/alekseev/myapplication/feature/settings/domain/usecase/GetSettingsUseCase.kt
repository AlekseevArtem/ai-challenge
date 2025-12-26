package ru.alekseev.myapplication.feature.settings.domain.usecase

import ru.alekseev.myapplication.feature.settings.domain.entity.AppSettings
import ru.alekseev.myapplication.feature.settings.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow

class GetSettingsUseCase(
    private val repository: SettingsRepository
) {
    operator fun invoke(): Flow<AppSettings> {
        return repository.observeSettings()
    }
}
