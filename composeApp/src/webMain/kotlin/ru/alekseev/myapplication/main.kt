package ru.alekseev.myapplication

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import kotlinx.browser.document
import ru.alekseev.myapplication.root.DefaultRootComponent
import ru.alekseev.myapplication.root.RootContent

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val lifecycle = LifecycleRegistry()
    val rootComponent = DefaultRootComponent(DefaultComponentContext(lifecycle))

    ComposeViewport(document.body!!) {
        RootContent(component = rootComponent)
    }
}