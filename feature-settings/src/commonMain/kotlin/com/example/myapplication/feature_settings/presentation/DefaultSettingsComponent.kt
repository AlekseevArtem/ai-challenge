package com.example.myapplication.feature_settings.presentation

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.doOnDestroy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import pro.respawn.flowmvi.api.Store

class DefaultSettingsComponent(
    private val componentContext: ComponentContext,
    private val settingsStore: SettingsStore,
    private val onFinished: () -> Unit,
) : SettingsComponent, ComponentContext by componentContext {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override val store: Store<SettingsState, SettingsIntent, SettingsAction> = settingsStore.store

    init {
        lifecycle.doOnDestroy {
            scope.cancel()
            store.close()
        }

        scope.launch {
            store.start(scope)
        }
    }

    override fun onBackClicked() {
        onFinished()
    }
}
