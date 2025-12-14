package ru.alekseev.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.arkivanov.decompose.defaultComponentContext
import ru.alekseev.myapplication.root.DefaultRootComponent
import ru.alekseev.myapplication.root.RootContent

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = DefaultRootComponent(componentContext = defaultComponentContext())

        setContent {
            RootContent(component = root)
        }
    }
}