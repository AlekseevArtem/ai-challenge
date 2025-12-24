package ru.alekseev.myapplication

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.decompose.extensions.compose.lifecycle.LifecycleController
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.example.myapplication.feature_main.di.featureMainModule
import com.example.myapplication.feature_settings.di.featureSettingsModule
import org.koin.core.context.startKoin
import ru.alekseev.myapplication.di.platformModule
import ru.alekseev.myapplication.root.DefaultRootComponent
import ru.alekseev.myapplication.root.RootContent

fun main() {
    // Initialize Koin
    startKoin {
        modules(
            platformModule,
            featureMainModule,
            featureSettingsModule
        )
    }

    val lifecycle = LifecycleRegistry()

    val root = runOnUiThread {
        DefaultRootComponent(
            componentContext = DefaultComponentContext(lifecycle = lifecycle),
        )
    }

    application {
        val windowState = rememberWindowState()

        LifecycleController(lifecycle, windowState)

        Window(
            onCloseRequest = ::exitApplication,
            state = windowState,
            title = "My Application"
        ) {
            RootContent(root)
        }
    }
}
