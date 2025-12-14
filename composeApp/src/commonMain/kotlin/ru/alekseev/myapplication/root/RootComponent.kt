package ru.alekseev.myapplication.root

import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.Value
import com.example.myapplication.feature_main.presentation.MainComponent
import com.example.myapplication.feature_welcome.presentation.WelcomeComponent

interface RootComponent {

    val stack: Value<ChildStack<*, Child>>

    fun onBackClicked(toIndex: Int)

    sealed class Child {
        class Main(val component: MainComponent) : Child()
        class Welcome(val component: WelcomeComponent) : Child()
    }
}
