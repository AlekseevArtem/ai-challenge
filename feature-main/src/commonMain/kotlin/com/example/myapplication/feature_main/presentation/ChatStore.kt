@file:OptIn(ExperimentalUuidApi::class, ExperimentalTime::class)

package com.example.myapplication.feature_main.presentation

import com.example.myapplication.feature_main.domain.entity.ChatMessage
import com.example.myapplication.feature_main.domain.entity.ChatMessageState
import com.example.myapplication.feature_main.domain.usecase.ConnectToChatUseCase
import com.example.myapplication.feature_main.domain.usecase.ObserveMessagesUseCase
import com.example.myapplication.feature_main.domain.usecase.SendMessageUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import pro.respawn.flowmvi.api.Container
import pro.respawn.flowmvi.api.MVIAction
import pro.respawn.flowmvi.api.MVIIntent
import pro.respawn.flowmvi.api.MVIState
import pro.respawn.flowmvi.api.Store
import pro.respawn.flowmvi.dsl.store
import pro.respawn.flowmvi.plugins.init
import pro.respawn.flowmvi.plugins.recover
import pro.respawn.flowmvi.plugins.reduce
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

data class ChatState(
    val messages: List<ChatMessage> = emptyList(),
    val currentMessage: String = "",
    val error: String? = null,
    val isLoading: Boolean = false
) : MVIState

sealed interface ChatIntent : MVIIntent {
    data class SendMessage(val message: String) : ChatIntent
    data class UpdateMessageText(val text: String) : ChatIntent
    data object ClearError : ChatIntent
    data object Connect : ChatIntent
}

sealed interface ChatAction : MVIAction {
    data class ShowError(val message: String) : ChatAction
}

class ChatStore(
    private val sendMessageUseCase: SendMessageUseCase,
    private val observeMessagesUseCase: ObserveMessagesUseCase,
    private val connectToChatUseCase: ConnectToChatUseCase,
) : Container<ChatState, ChatIntent, ChatAction> {

    override val store: Store<ChatState, ChatIntent, ChatAction> = store(ChatState()) {
        configure {
            debuggable = true
            parallelIntents = true

            this@store.recover { exception ->
                updateState { copy(error = exception.message) }
                null
            }
        }

        init {
            // Connect to chat on start
            connectToChatUseCase()

            // Observe messages
            observeMessagesUseCase().onEach { result ->
                result.onSuccess { messageState ->
                    updateState {
                        when (messageState) {
                            is ChatMessageState.Loading -> {
                                copy(
                                    isLoading = true,
                                )
                            }
                            is ChatMessageState.Data -> {
                                copy(
                                    messages = messages + messageState.message,
                                    isLoading = false
                                )
                            }
                            is ChatMessageState.History -> {
                                copy(
                                    messages = messageState.messages
                                )
                            }
                        }
                    }
                }.onFailure { error ->
                    updateState {
                        copy(
                            error = error.message
                        )
                    }
                    action(ChatAction.ShowError(error.message ?: "Unknown error"))
                }
            }.launchIn(CoroutineScope(coroutineContext))
        }

        reduce { intent ->
            when (intent) {
                is ChatIntent.UpdateMessageText -> updateState {
                    copy(currentMessage = intent.text, error = null)
                }

                is ChatIntent.SendMessage -> {
                    val messageText = intent.message.trim()
                    if (messageText.isEmpty()) return@reduce

                    // Add user message immediately
                    val userMessage = ChatMessage(
                        id = Uuid.random().toString(),
                        content = messageText,
                        isFromUser = true,
                        timestamp = Clock.System.now().toEpochMilliseconds(),
                    )

                    updateState {
                        copy(
                            messages = messages,
                            currentMessage = "",
                            error = null
                        )
                    }

                    // Send message to server
                    sendMessageUseCase(messageText)
                }

                is ChatIntent.ClearError -> updateState {
                    copy(error = null)
                }

                is ChatIntent.Connect -> {
                    connectToChatUseCase()
                }
            }
        }
    }
}