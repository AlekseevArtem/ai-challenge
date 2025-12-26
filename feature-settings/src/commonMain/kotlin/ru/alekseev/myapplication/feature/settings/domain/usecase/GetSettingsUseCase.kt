package ru.alekseev.myapplication.feature.settings.domain.usecase

import ru.alekseev.myapplication.domain.entity.AppSettings
import ru.alekseev.myapplication.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow

class GetSettingsUseCase(
    private val repository: SettingsRepository
) {
    operator fun invoke(): Flow<AppSettings> {
        return repository.observeSettings()
    }
}
