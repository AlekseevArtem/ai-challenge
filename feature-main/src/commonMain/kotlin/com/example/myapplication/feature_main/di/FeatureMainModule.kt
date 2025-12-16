package com.example.myapplication.feature_main.di

import com.example.myapplication.feature_main.data.repository.ChatRepositoryImpl
import com.example.myapplication.feature_main.domain.repository.ChatRepository
import com.example.myapplication.feature_main.domain.usecase.ConnectToChatUseCase
import com.example.myapplication.feature_main.domain.usecase.ObserveMessagesUseCase
import com.example.myapplication.feature_main.domain.usecase.SendMessageUseCase
import com.example.myapplication.feature_main.presentation.ChatStore
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

    // Repository
    singleOf(::ChatRepositoryImpl) bind ChatRepository::class

    // Use Cases
    factoryOf(::SendMessageUseCase)
    factoryOf(::ObserveMessagesUseCase)
    factoryOf(::ConnectToChatUseCase)

    // Store
    factoryOf(::ChatStore)
}