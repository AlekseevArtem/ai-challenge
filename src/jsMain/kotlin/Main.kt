package jsMain.kotlin

import androidx.compose.runtime.*
import org.jetbrains.compose.web.dom.*
import org.jetbrains.compose.web.renderComposable

fun main() {
    renderComposable(rootElementId = "root") {
        var counter by remember { mutableStateOf(0) }

        H1 { Text("Hello from Compose Web!") }

        Button({
            onClick { counter++ }
        }) {
            Text("Clicked $counter times")
        }
    }
}