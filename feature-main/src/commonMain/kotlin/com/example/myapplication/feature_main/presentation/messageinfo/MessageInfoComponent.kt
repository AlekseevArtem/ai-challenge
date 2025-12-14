package com.example.myapplication.feature_main.presentation.messageinfo

import ru.alekseev.myapplication.dto.MessageInfoDto

interface MessageInfoComponent {
    val messageInfo: MessageInfoDto

    fun onDismiss()
}
