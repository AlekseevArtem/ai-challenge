package com.example.myapplication.feature_main.presentation.messageinfo

import com.arkivanov.decompose.ComponentContext
import ru.alekseev.myapplication.dto.MessageInfoDto

class DefaultMessageInfoComponent(
    componentContext: ComponentContext,
    override val messageInfo: MessageInfoDto,
    private val onDismissRequested: () -> Unit
) : MessageInfoComponent, ComponentContext by componentContext {

    override fun onDismiss() {
        onDismissRequested()
    }
}
