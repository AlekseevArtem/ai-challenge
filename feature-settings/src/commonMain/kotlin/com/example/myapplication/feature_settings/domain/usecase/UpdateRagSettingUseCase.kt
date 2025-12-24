package com.example.myapplication.feature_settings.domain.usecase

import ru.alekseev.myapplication.domain.entity.RagMode
import com.example.myapplication.feature_settings.domain.repository.SettingsRepository

class UpdateRagSettingUseCase(
    private val repository: SettingsRepository
) {
    suspend operator fun invoke(ragMode: RagMode) {
        repository.updateRagMode(ragMode)
    }
}
