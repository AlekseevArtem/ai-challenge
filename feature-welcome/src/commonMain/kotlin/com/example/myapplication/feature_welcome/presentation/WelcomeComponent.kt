package com.example.myapplication.feature_welcome.presentation

import com.arkivanov.decompose.value.Value

interface WelcomeComponent {

    val model: Value<Model>

    fun onUpdateGreetingText()
    fun onBackClicked()

    data class Model(
        val greetingText: String = "Welcome from Decompose!"
    )
}
