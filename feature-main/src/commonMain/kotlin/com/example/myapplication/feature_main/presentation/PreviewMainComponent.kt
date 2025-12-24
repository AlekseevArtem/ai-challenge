package com.example.myapplication.feature_main.presentation

import com.arkivanov.decompose.router.slot.ChildSlot
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.example.myapplication.feature_main.domain.entity.ChatMessage
import com.example.myapplication.feature_main.presentation.messageinfo.MessageInfoComponent
import pro.respawn.flowmvi.api.Store
import pro.respawn.flowmvi.dsl.store

object PreviewMainComponent : MainComponent {

    override val state: Store<ChatState, ChatIntent, ChatAction> = store(ChatState()) {  }

    override val messageInfoSlot: Value<ChildSlot<*, MessageInfoComponent>> = MutableValue(
        ChildSlot<Unit, MessageInfoComponent>(child = null)
    )

    override fun onSendMessage(message: String) {}

    override fun onMessageTextChanged(text: String) {}

    override fun onClearError() {}

    override fun onMessageClick(message: ChatMessage) {}

    override fun onOpenSettings() {}
}
