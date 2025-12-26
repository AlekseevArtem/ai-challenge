package ru.alekseev.myapplication.feature.settings.presentation

import pro.respawn.flowmvi.api.Store

interface SettingsComponent {
    val store: Store<SettingsState, SettingsIntent, SettingsAction>
    fun onBackClicked()
}
