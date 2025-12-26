package ru.alekseev.myapplication.feature.main.presentation.messageinfo

import ru.alekseev.myapplication.feature.main.domain.entity.MessageInfo

interface MessageInfoComponent {
    val messageInfo: MessageInfo

    fun onDismiss()
}
