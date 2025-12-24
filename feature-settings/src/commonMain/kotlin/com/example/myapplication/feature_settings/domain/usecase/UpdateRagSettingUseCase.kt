package com.example.myapplication.feature_settings.domain.usecase

import com.example.myapplication.feature_settings.domain.repository.SettingsRepository

class UpdateRagSettingUseCase(
    private val repository: SettingsRepository
) {
    suspend operator fun invoke(enabled: Boolean) {
        repository.updateRagEnabled(enabled)
    }
}
