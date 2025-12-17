package com.example.myapplication.feature_main.domain.usecase

import com.example.myapplication.feature_main.data.mapper.toDto
import com.example.myapplication.feature_main.domain.entity.AlertEntity
import ru.alekseev.myapplication.service.AlertDispatcher

/**
 * Use case for dispatching alerts to the user using platform-specific AlertDispatcher
 */
class DispatchAlertUseCase(
    private val alertDispatcher: AlertDispatcher
) {
    suspend operator fun invoke(alert: AlertEntity) {
        // Convert domain Alert to DTO UserAlert for platform dispatcher
        val userAlert = alert.toDto()

        // Dispatch the alert using platform-specific implementation
        alertDispatcher.dispatch(userAlert)
    }
}
