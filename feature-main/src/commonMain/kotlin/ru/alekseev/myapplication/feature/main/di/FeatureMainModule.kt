package ru.alekseev.myapplication.feature.main.di

import ru.alekseev.myapplication.feature.main.data.repository.ChatConnectionRepositoryImpl
import ru.alekseev.myapplication.feature.main.domain.repository.ChatConnectionRepository
import ru.alekseev.myapplication.feature.main.domain.usecase.ConnectToChatUseCase
import ru.alekseev.myapplication.feature.main.domain.usecase.DispatchAlertUseCase
import ru.alekseev.myapplication.feature.main.domain.usecase.ObserveMessagesUseCase
import ru.alekseev.myapplication.feature.main.domain.usecase.SendMessageUseCase
import ru.alekseev.myapplication.feature.main.presentation.ChatStore
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module
import ru.alekseev.myapplication.core.common.JsonFactory
import ru.alekseev.myapplication.data.datasource.ChatWebSocketDataSource
import ru.alekseev.myapplication.data.datasource.ChatWebSocketDataSourceImpl

val featureMainModule = module {
    // Json serializer
    single {
        JsonFactory.create()
    }

    // Data Source from shared
    singleOf(::ChatWebSocketDataSourceImpl) bind ChatWebSocketDataSource::class

    // Repository (WebSocket connection management)
    singleOf(::ChatConnectionRepositoryImpl) bind ChatConnectionRepository::class

    // Use Cases
    factoryOf(::SendMessageUseCase)
    factoryOf(::ObserveMessagesUseCase)
    factoryOf(::ConnectToChatUseCase)
    factoryOf(::DispatchAlertUseCase)

    // Store
    factoryOf(::ChatStore)
}