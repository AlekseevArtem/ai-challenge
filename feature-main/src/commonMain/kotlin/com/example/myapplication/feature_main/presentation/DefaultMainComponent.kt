package com.example.myapplication.feature_main.presentation

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.slot.ChildSlot
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.example.myapplication.feature_main.data.datasource.ChatWebSocketDataSource
import com.example.myapplication.feature_main.data.repository.ChatRepositoryImpl
import com.example.myapplication.feature_main.domain.entity.ChatMessage
import com.example.myapplication.feature_main.domain.usecase.ConnectToChatUseCase
import com.example.myapplication.feature_main.domain.usecase.ObserveMessagesUseCase
import com.example.myapplication.feature_main.domain.usecase.SendMessageUseCase
import com.example.myapplication.feature_main.presentation.messageinfo.DefaultMessageInfoComponent
import com.example.myapplication.feature_main.presentation.messageinfo.MessageInfoComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import pro.respawn.flowmvi.api.Store
import ru.alekseev.myapplication.dto.MessageInfoDto

class DefaultMainComponent(
    componentContext: ComponentContext,
    webSocketDataSource: ChatWebSocketDataSource,
) : MainComponent, ComponentContext by componentContext {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val repository = ChatRepositoryImpl(webSocketDataSource)

    private val store = ChatStore(
        sendMessageUseCase = SendMessageUseCase(repository),
        observeMessagesUseCase = ObserveMessagesUseCase(repository),
        connectToChatUseCase = ConnectToChatUseCase(repository)
    )

    override val state: Store<ChatState, ChatIntent, ChatAction> = store.store

    private val messageInfoNavigation = SlotNavigation<MessageInfoConfig>()

    override val messageInfoSlot: Value<ChildSlot<*, MessageInfoComponent>> = childSlot(
        source = messageInfoNavigation,
        serializer = MessageInfoConfig.serializer(),
        handleBackButton = true
    ) { config, childComponentContext ->
        DefaultMessageInfoComponent(
            componentContext = childComponentContext,
            messageInfo = config.messageInfo,
            onDismissRequested = ::dismissMessageInfo
        )
    }

    init {
        lifecycle.doOnDestroy {
            scope.cancel()
            store.store.close()
        }

        scope.launch {
            store.store.start(scope)
        }
    }

    @Serializable
    private data class MessageInfoConfig(
        val messageInfo: MessageInfoDto
    )

    override fun onSendMessage(message: String) {
        scope.launch {
            store.store.emit(ChatIntent.SendMessage(message))
        }
    }

    override fun onMessageTextChanged(text: String) {
        scope.launch(Dispatchers.Unconfined) {
            store.store.emit(ChatIntent.UpdateMessageText(text))
        }
    }

    override fun onClearError() {
        scope.launch {
            store.store.emit(ChatIntent.ClearError)
        }
    }

    override fun onMessageClick(message: ChatMessage) {
        message.messageInfo?.let { info ->
            messageInfoNavigation.activate(MessageInfoConfig(info))
        }
    }

    private fun dismissMessageInfo() {
        messageInfoNavigation.dismiss()
    }
}
