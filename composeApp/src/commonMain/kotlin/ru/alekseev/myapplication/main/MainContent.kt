package ru.alekseev.myapplication.main

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.example.myapplication.feature_main.domain.entity.ChatMessage
import com.example.myapplication.feature_main.presentation.MainComponent
import com.example.myapplication.feature_main.presentation.PreviewMainComponent
import kotlinx.coroutines.launch
import pro.respawn.flowmvi.compose.dsl.subscribe
import ru.alekseev.myapplication.main.messageinfo.MessageInfoContent
import ru.alekseev.myapplication.permissions.PermissionDeniedException
import ru.alekseev.myapplication.permissions.rememberPermissionsWrapper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MainContent(
    component: MainComponent,
    modifier: Modifier = Modifier,
) {
    val state by component.state.subscribe()

    val permissionsWrapper = rememberPermissionsWrapper()

    permissionsWrapper.BindEffect()

    // TODO(cделать красивую обертку)
    LaunchedEffect(Unit) {
        try {
            permissionsWrapper.requestNotificationPermission()
        } catch (e: PermissionDeniedException) {
            // Permission denied, silently ignore
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1A1A2E),
                        Color(0xFF0F3460),
                        Color(0xFF16213E)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top Bar with gradient
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.Transparent
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFF6C63FF),
                                    Color(0xFF5B4DFF),
                                    Color(0xFF4A3FFF)
                                )
                            )
                        )
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.SmartToy,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Claude AI Chat",
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Settings button on the right
                    IconButton(
                        onClick = component::onOpenSettings,
                        modifier = Modifier.align(Alignment.CenterEnd)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            // Messages List
            ChatMessagesList(
                messages = state.messages,
                isLoading = state.isLoading,
                onMessageClick = component::onMessageClick,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            )

            // Message Info Dialog (Decompose Child Slot)
            val messageInfoSlot by component.messageInfoSlot.subscribeAsState()
            messageInfoSlot.child?.instance?.let { messageInfoComponent ->
                MessageInfoContent(component = messageInfoComponent)
            }

            // Input Area
            MessageInputArea(
                currentMessage = state.currentMessage,
                onMessageChanged = component::onMessageTextChanged,
                onSendClicked = { component.onSendMessage(state.currentMessage) },
                isLoading = state.isLoading,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ChatMessagesList(
    messages: List<ChatMessage>,
    modifier: Modifier = Modifier,
    isLoading: Boolean,
    onMessageClick: (ChatMessage) -> Unit,
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            coroutineScope.launch {
                listState.animateScrollToItem(messages.size - 1)
            }
        }
    }

    LazyColumn(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        state = listState,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(messages, key = { it.id }) { message ->
            MessageBubble(
                message = message,
                onClick = onMessageClick
            )
        }

        if (isLoading) {
            item(key = "Loading") {
                LoadingIndicator()
            }
        }

    }
}

@Composable
private fun MessageBubble(
    message: ChatMessage,
    onClick: (ChatMessage) -> Unit,
) {
    val isUser = message.isFromUser

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Avatar(isUser = false)
            Spacer(modifier = Modifier.width(8.dp))
        }

        Surface(
            onClick = { if (!isUser && message.messageInfo != null) onClick(message) },
            modifier = Modifier.widthIn(max = 280.dp),
            shape = RoundedCornerShape(
                topStart = if (isUser) 20.dp else 4.dp,
                topEnd = if (isUser) 4.dp else 20.dp,
                bottomStart = 20.dp,
                bottomEnd = 20.dp
            ),
            color = if (isUser) Color(0xFF6C63FF) else Color(0xFF2C2C54),
            shadowElevation = 4.dp
        ) {
            Box(
                modifier = Modifier
                    .background(
                        if (isUser) {
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFF6C63FF),
                                    Color(0xFF5B4DFF)
                                )
                            )
                        } else {
                            // Different gradient for RAG-enabled vs RAG-disabled responses
                            if (message.usedRag == true) {
                                // RAG enabled - green gradient
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xFF1B5E20),
                                        Color(0xFF2E7D32)
                                    )
                                )
                            } else {
                                // RAG disabled - default purple gradient
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xFF2C2C54),
                                        Color(0xFF3A3A6B)
                                    )
                                )
                            }
                        }
                    )
                    .padding(12.dp)
            ) {
                Text(
                    text = message.content,
                    color = Color.White,
                    fontSize = 15.sp
                )
            }
        }

        if (isUser) {
            Spacer(modifier = Modifier.width(8.dp))
            Avatar(isUser = true)
        }
    }
}

@Composable
private fun Avatar(isUser: Boolean) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(
                if (isUser) {
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF6C63FF),
                            Color(0xFF4A3FFF)
                        )
                    )
                } else {
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFE63946),
                            Color(0xFFD62839)
                        )
                    )
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isUser) Icons.Default.Person else Icons.Default.SmartToy,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun LoadingIndicator() {
    Row(
        modifier = Modifier.padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) { index ->
            val infiniteTransition = rememberInfiniteTransition()
            val scale by infiniteTransition.animateFloat(
                initialValue = 0.5f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(600, delayMillis = index * 200),
                    repeatMode = RepeatMode.Reverse
                )
            )

            Box(
                modifier = Modifier
                    .size(12.dp)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(Color(0xFF6C63FF))
            )
        }
    }
}

@Composable
private fun MessageInputArea(
    currentMessage: String,
    onMessageChanged: (String) -> Unit,
    onSendClicked: () -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }

    var textFieldValue by remember { mutableStateOf(TextFieldValue(currentMessage)) }

    LaunchedEffect(currentMessage) {
        if (currentMessage != textFieldValue.text) {
            textFieldValue = TextFieldValue(currentMessage, selection = TextRange(currentMessage.length))
        }
    }

    Surface(
        modifier = modifier,
        color = Color(0xFF1A1A2E),
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = textFieldValue,
                onValueChange = { newValue ->
                    textFieldValue = newValue
                    onMessageChanged(newValue.text)
                },
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester),
                placeholder = {
                    Text(
                        text = "Type a message...",
                        color = Color.Gray
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF6C63FF),
                    unfocusedBorderColor = Color(0xFF3A3A6B),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = Color(0xFF6C63FF)
                ),
                shape = RoundedCornerShape(24.dp),
                enabled = isLoading.not(),
                readOnly = false,
                singleLine = false,
            )

            FloatingActionButton(
                onClick = onSendClicked,
                modifier = Modifier.size(56.dp),
                containerColor = Color.Transparent,
//                enabled = currentMessage.isNotBlank() && !isLoading
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF6C63FF),
                                    Color(0xFF4A3FFF)
                                )
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun MainPreview() {
    MainContent(PreviewMainComponent)
}
