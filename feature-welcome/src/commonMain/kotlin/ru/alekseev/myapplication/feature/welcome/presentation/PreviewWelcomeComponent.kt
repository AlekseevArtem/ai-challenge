package ru.alekseev.myapplication.feature.welcome.presentation

import com.arkivanov.decompose.value.MutableValue
import ru.alekseev.myapplication.feature.welcome.presentation.WelcomeComponent.Model

object PreviewWelcomeComponent : WelcomeComponent {
  override val model = MutableValue(Model())

  override fun onUpdateGreetingText() {}

  override fun onBackClicked() {}
}
