package ru.alekseev.myapplication.feature.settings.presentation

import ru.alekseev.myapplication.domain.entity.RagMode
import ru.alekseev.myapplication.feature.settings.domain.usecase.GetSettingsUseCase
import ru.alekseev.myapplication.feature.settings.domain.usecase.UpdateRagSettingUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import pro.respawn.flowmvi.api.Container
import pro.respawn.flowmvi.api.MVIAction
import pro.respawn.flowmvi.api.MVIIntent
import pro.respawn.flowmvi.api.MVIState
import pro.respawn.flowmvi.api.Store
import pro.respawn.flowmvi.dsl.store
import pro.respawn.flowmvi.plugins.init
import pro.respawn.flowmvi.plugins.recover
import pro.respawn.flowmvi.plugins.reduce

data class SettingsState(
    val ragMode: RagMode = RagMode.Disabled,
    val error: String? = null
) : MVIState

sealed interface SettingsIntent : MVIIntent {
    data class UpdateRagMode(val ragMode: RagMode) : SettingsIntent
    data object ClearError : SettingsIntent
}

sealed interface SettingsAction : MVIAction {
    data class ShowError(val message: String) : SettingsAction
}

class SettingsStore(
    private val getSettingsUseCase: GetSettingsUseCase,
    private val updateRagSettingUseCase: UpdateRagSettingUseCase
) : Container<SettingsState, SettingsIntent, SettingsAction> {

    override val store: Store<SettingsState, SettingsIntent, SettingsAction> = store(SettingsState()) {
        configure {
            debuggable = true
            parallelIntents = false

            this@store.recover { exception ->
                updateState { copy(error = exception.message) }
                action(SettingsAction.ShowError(exception.message ?: "Unknown error"))
                null
            }
        }

        init {
            // Observe settings changes
            getSettingsUseCase().onEach { settings ->
                updateState {
                    copy(ragMode = settings.ragMode)
                }
            }.launchIn(CoroutineScope(coroutineContext))
        }

        reduce { intent ->
            when (intent) {
                is SettingsIntent.UpdateRagMode -> {
                    CoroutineScope(coroutineContext).launch {
                        try {
                            updateRagSettingUseCase(intent.ragMode)
                            updateState { copy(error = null) }
                        } catch (e: Exception) {
                            updateState { copy(error = e.message) }
                            action(SettingsAction.ShowError(e.message ?: "Failed to update setting"))
                        }
                    }
                }

                is SettingsIntent.ClearError -> updateState {
                    copy(error = null)
                }
            }
        }
    }
}
