package ru.alekseev.myapplication

import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import ru.alekseev.myapplication.root.RootContent
import ru.alekseev.myapplication.root.RootComponent

fun rootViewController(root: RootComponent): UIViewController {
    // Ensure Koin is initialized
    initKoin()

    return ComposeUIViewController {
        RootContent(component = root, modifier = Modifier.fillMaxSize())
    }
}