package ru.alekseev.myapplication

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.example.myapplication.feature_main.di.featureMainModule
import kotlinx.browser.document
import org.koin.core.context.startKoin
import ru.alekseev.myapplication.di.platformModule
import ru.alekseev.myapplication.root.DefaultRootComponent
import ru.alekseev.myapplication.root.RootContent

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    // Initialize Koin
    startKoin {
        modules(
            platformModule,
            featureMainModule
        )
    }

    val lifecycle = LifecycleRegistry()
    val rootComponent = DefaultRootComponent(DefaultComponentContext(lifecycle))

    ComposeViewport(document.body!!) {
        RootContent(component = rootComponent)
    }
}