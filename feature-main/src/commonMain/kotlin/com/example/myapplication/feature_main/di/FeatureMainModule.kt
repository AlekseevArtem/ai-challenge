package com.example.myapplication.feature_main.di

import com.example.myapplication.feature_main.data.datasource.ChatWebSocketDataSource
import com.example.myapplication.feature_main.data.datasource.ChatWebSocketDataSourceImpl
import com.example.myapplication.feature_main.data.repository.ChatRepositoryImpl
import com.example.myapplication.feature_main.domain.repository.ChatRepository
import com.example.myapplication.feature_main.domain.usecase.ConnectToChatUseCase
import com.example.myapplication.feature_main.domain.usecase.ObserveMessagesUseCase
import com.example.myapplication.feature_main.domain.usecase.SendMessageUseCase
import com.example.myapplication.feature_main.presentation.ChatStore
import kotlinx.serialization.json.Json
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import ru.alekseev.myapplication.SERVER_WS_URL

val featureMainModule = module {
    // Json serializer
    single {
        Json {
            classDiscriminator = "type"
            ignoreUnknownKeys = true
            prettyPrint = true
            isLenient = true
            encodeDefaults = true
        }
    }

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