package ru.alekseev.myapplication.main.messageinfo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.alekseev.myapplication.feature.main.presentation.messageinfo.MessageInfoComponent
import ru.alekseev.myapplication.utils.formatUsd

@Composable
fun MessageInfoContent(
    component: MessageInfoComponent,
    modifier: Modifier = Modifier
) {
    val messageInfo = component.messageInfo

    AlertDialog(
        onDismissRequest = component::onDismiss,
        title = {
            Text(
                text = "Message Information",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = Color.White
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                InfoRow(label = "Input Tokens", value = messageInfo.inputTokens.toString())
                InfoRow(label = "Output Tokens", value = messageInfo.outputTokens.toString())
                InfoRow(
                    label = "Total Tokens",
                    value = (messageInfo.inputTokens + messageInfo.outputTokens).toString()
                )
                InfoRow(label = "Response Time", value = "${messageInfo.responseTimeMs}ms")
                InfoRow(label = "Model", value = messageInfo.model)
                InfoRow(label = "Cost", value = formatUsd(messageInfo.cost))
            }
        },
        confirmButton = {
            TextButton(onClick = component::onDismiss) {
                Text("Close", color = Color(0xFF6C63FF))
            }
        },
        containerColor = Color(0xFF1A1A2E),
        textContentColor = Color.White,
        modifier = modifier
    )
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = Color.Gray,
            fontSize = 14.sp
        )
        Text(
            text = value,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
