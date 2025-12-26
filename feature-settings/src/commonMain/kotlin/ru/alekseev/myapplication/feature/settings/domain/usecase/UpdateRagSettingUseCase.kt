package ru.alekseev.myapplication.feature.settings.domain.usecase

import ru.alekseev.myapplication.domain.entity.RagMode
import ru.alekseev.myapplication.feature.settings.domain.repository.SettingsRepository

class UpdateRagSettingUseCase(
    private val repository: SettingsRepository
) {
    suspend operator fun invoke(ragMode: RagMode) {
        repository.updateRagMode(ragMode)
    }
}
