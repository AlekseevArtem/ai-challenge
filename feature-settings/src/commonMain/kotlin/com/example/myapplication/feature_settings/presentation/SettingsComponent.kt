package com.example.myapplication.feature_settings.presentation

import pro.respawn.flowmvi.api.Store

interface SettingsComponent {
    val store: Store<SettingsState, SettingsIntent, SettingsAction>
    fun onBackClicked()
}
