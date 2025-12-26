package ru.alekseev.myapplication.feature.settings.di

import ru.alekseev.myapplication.feature.settings.data.datasource.InMemorySettingsDataSource
import ru.alekseev.myapplication.feature.settings.data.datasource.SettingsLocalDataSource
import ru.alekseev.myapplication.feature.settings.data.repository.SettingsRepositoryImpl
import ru.alekseev.myapplication.feature.settings.domain.repository.SettingsRepository
import ru.alekseev.myapplication.feature.settings.domain.usecase.GetSettingsUseCase
import ru.alekseev.myapplication.feature.settings.domain.usecase.UpdateRagSettingUseCase
import ru.alekseev.myapplication.feature.settings.presentation.SettingsStore
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
