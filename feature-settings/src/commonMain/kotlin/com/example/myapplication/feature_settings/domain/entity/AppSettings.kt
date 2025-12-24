package com.example.myapplication.feature_settings.domain.entity

import ru.alekseev.myapplication.domain.entity.RagMode

data class AppSettings(
    val ragMode: RagMode = RagMode.Disabled
)
