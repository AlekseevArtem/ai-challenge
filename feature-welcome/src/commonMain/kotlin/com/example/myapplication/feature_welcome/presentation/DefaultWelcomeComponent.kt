package com.example.myapplication.feature_welcome.presentation

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.update
import com.example.myapplication.feature_welcome.presentation.WelcomeComponent.Model
import ru.alekseev.myapplication.getPlatform

class DefaultWelcomeComponent(
    private val componentContext: ComponentContext,
    private val onFinished: () -> Unit,
) : WelcomeComponent, ComponentContext by componentContext {

    // Consider preserving and managing the state via a store
    private val state = MutableValue(Model())
    override val model: Value<Model> = state

    override fun onUpdateGreetingText() {
        state.update { it.copy(greetingText = "Welcome from ${getPlatform().name}") }
    }

    override fun onBackClicked() {
        onFinished()
    }
}
