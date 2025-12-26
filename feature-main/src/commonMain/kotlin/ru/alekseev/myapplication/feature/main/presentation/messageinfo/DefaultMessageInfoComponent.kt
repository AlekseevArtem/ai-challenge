package ru.alekseev.myapplication.feature.main.presentation.messageinfo

import com.arkivanov.decompose.ComponentContext
import ru.alekseev.myapplication.feature.main.domain.entity.MessageInfo
import ru.alekseev.myapplication.data.dto.MessageInfoDto

class DefaultMessageInfoComponent(
    componentContext: ComponentContext,
    override val messageInfo: MessageInfo,
    private val onDismissRequested: () -> Unit
) : MessageInfoComponent, ComponentContext by componentContext {

    override fun onDismiss() {
        onDismissRequested()
    }
}
