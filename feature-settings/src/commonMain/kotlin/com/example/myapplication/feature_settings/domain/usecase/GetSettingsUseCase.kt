package com.example.myapplication.feature_settings.domain.usecase

import com.example.myapplication.feature_settings.domain.entity.AppSettings
import com.example.myapplication.feature_settings.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow

class GetSettingsUseCase(
    private val repository: SettingsRepository
) {
    operator fun invoke(): Flow<AppSettings> {
        return repository.observeSettings()
    }
}
