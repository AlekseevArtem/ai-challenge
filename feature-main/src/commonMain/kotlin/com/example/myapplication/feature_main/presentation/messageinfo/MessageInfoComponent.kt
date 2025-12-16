package com.example.myapplication.feature_main.presentation.messageinfo

import com.example.myapplication.feature_main.domain.entity.MessageInfo

interface MessageInfoComponent {
    val messageInfo: MessageInfo

    fun onDismiss()
}
