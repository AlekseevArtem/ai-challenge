package ru.alekseev.myapplication.feature.main.presentation

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.slot.ChildSlot
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.doOnDestroy
import ru.alekseev.myapplication.feature.main.domain.entity.ChatMessage
import ru.alekseev.myapplication.feature.main.domain.entity.MessageInfo
import ru.alekseev.myapplication.feature.main.presentation.messageinfo.DefaultMessageInfoComponent
import ru.alekseev.myapplication.feature.main.presentation.messageinfo.MessageInfoComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import pro.respawn.flowmvi.api.Store

class DefaultMainComponent(
    componentContext: ComponentContext,
    private val onOpenSettings: () -> Unit = {},
) : MainComponent, ComponentContext by componentContext, KoinComponent {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val chatStore: ChatStore by inject()

    override val state: Store<ChatState, ChatIntent, ChatAction> = chatStore.store

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
            state.close()
        }

        scope.launch {
            state.start(scope)
        }
    }

    @Serializable
    private data class MessageInfoConfig(
        val messageInfo: MessageInfo,
    )

    override fun onSendMessage(message: String) {
        scope.launch {
            state.emit(ChatIntent.SendMessage(message))
        }
    }

    override fun onMessageTextChanged(text: String) {
        scope.launch(Dispatchers.Unconfined) {
            state.emit(ChatIntent.UpdateMessageText(text))
        }
    }

    override fun onClearError() {
        scope.launch {
            state.emit(ChatIntent.ClearError)
        }
    }

    override fun onMessageClick(message: ChatMessage) {
        message.messageInfo?.let { info ->
            messageInfoNavigation.activate(MessageInfoConfig(info))
        }
    }

    override fun onOpenSettings() {
        onOpenSettings.invoke()
    }

    private fun dismissMessageInfo() {
        messageInfoNavigation.dismiss()
    }
}
