package com.example.myapplication.feature_main.presentation

import com.arkivanov.decompose.router.slot.ChildSlot
import com.arkivanov.decompose.value.Value
import com.example.myapplication.feature_main.domain.entity.ChatMessage
import com.example.myapplication.feature_main.presentation.messageinfo.MessageInfoComponent
import pro.respawn.flowmvi.api.Store

interface MainComponent {

    val state: Store<ChatState, ChatIntent, ChatAction>

    val messageInfoSlot: Value<ChildSlot<*, MessageInfoComponent>>

    fun onSendMessage(message: String)

    fun onMessageTextChanged(text: String)

    fun onClearError()

    fun onMessageClick(message: ChatMessage)

    fun onOpenSettings()
}
