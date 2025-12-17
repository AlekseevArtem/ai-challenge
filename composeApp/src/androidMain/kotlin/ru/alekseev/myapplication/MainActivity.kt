package ru.alekseev.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.arkivanov.decompose.defaultComponentContext
import org.koin.android.ext.android.inject
import ru.alekseev.myapplication.root.DefaultRootComponent
import ru.alekseev.myapplication.root.RootContent

class MainActivity : ComponentActivity() {

    private val activityProvider: ActivityProvider by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        activityProvider.set(this)

        val root = DefaultRootComponent(componentContext = defaultComponentContext())

        setContent {
            RootContent(component = root)
        }
    }


    override fun onDestroy() {
        activityProvider.clear(this)
        super.onDestroy()
    }
}