package ru.alekseev.myapplication.root

import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.Value
import ru.alekseev.myapplication.feature.main.presentation.MainComponent
import ru.alekseev.myapplication.feature.settings.presentation.SettingsComponent
import ru.alekseev.myapplication.feature.welcome.presentation.WelcomeComponent

interface RootComponent {

    val stack: Value<ChildStack<*, Child>>

    fun onBackClicked(toIndex: Int)

    sealed class Child {
        class Main(val component: MainComponent) : Child()
        class Welcome(val component: WelcomeComponent) : Child()
        class Settings(val component: SettingsComponent) : Child()
    }
}
