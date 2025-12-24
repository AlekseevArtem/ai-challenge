package com.example.myapplication.feature_settings.di

import com.example.myapplication.feature_settings.data.datasource.InMemorySettingsDataSource
import com.example.myapplication.feature_settings.data.datasource.SettingsLocalDataSource
import com.example.myapplication.feature_settings.data.repository.SettingsRepositoryImpl
import com.example.myapplication.feature_settings.domain.repository.SettingsRepository
import com.example.myapplication.feature_settings.domain.usecase.GetSettingsUseCase
import com.example.myapplication.feature_settings.domain.usecase.UpdateRagSettingUseCase
import com.example.myapplication.feature_settings.presentation.SettingsStore
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val featureSettingsModule = module {
    singleOf(::InMemorySettingsDataSource) bind SettingsLocalDataSource::class
    singleOf(::SettingsRepositoryImpl) bind SettingsRepository::class
    factoryOf(::GetSettingsUseCase)
    factoryOf(::UpdateRagSettingUseCase)
    factoryOf(::SettingsStore)
}
