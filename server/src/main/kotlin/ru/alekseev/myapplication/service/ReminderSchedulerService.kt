package ru.alekseev.myapplication.service

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import ru.alekseev.myapplication.data.dto.AlertSeverityDto
import ru.alekseev.myapplication.data.dto.UserAlertDto
import java.util.UUID
import kotlin.time.Duration.Companion.minutes
import ru.alekseev.myapplication.data.dto.ClaudeRequest
import ru.alekseev.myapplication.data.dto.ClaudeMessage
import ru.alekseev.myapplication.data.dto.ClaudeMessageContent

/**
 * Service that periodically generates reminder summaries
 * and broadcasts them to all connected clients
 */
class ReminderSchedulerService(
    private val claudeApiService: ClaudeApiService
) {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var schedulerJob: Job? = null

    private val _alertFlow = MutableSharedFlow<UserAlertDto>(replay = 0)
    val alertFlow = _alertFlow.asSharedFlow()

    companion object {
        private val SCHEDULE_INTERVAL = 2.minutes
    }

    /**
     * Start the scheduler
     */
    fun start() {
        if (schedulerJob?.isActive == true) {
            System.err.println("[ReminderScheduler] Scheduler is already running")
            return
        }

        System.err.println("[ReminderScheduler] Starting scheduler with interval: $SCHEDULE_INTERVAL")

        schedulerJob = scope.launch {
            // Wait a bit before first execution to allow system to fully initialize
            delay(30_000) // 30 seconds

            while (isActive) {
                try {
                    System.err.println("[ReminderScheduler] Generating reminder summary...")
                    generateAndBroadcastSummary()
                } catch (e: Exception) {
                    System.err.println("[ReminderScheduler] Error generating summary: ${e.message}")
                    e.printStackTrace()
                }

                delay(SCHEDULE_INTERVAL)
            }
        }

        System.err.println("[ReminderScheduler] Scheduler started successfully")
    }

    /**
     * Stop the scheduler
     */
    fun stop() {
        System.err.println("[ReminderScheduler] Stopping scheduler...")
        schedulerJob?.cancel()
        schedulerJob = null
        System.err.println("[ReminderScheduler] Scheduler stopped")
    }

    /**
     * Generate summary and broadcast to all clients
     */
    private suspend fun generateAndBroadcastSummary() {
        try {
            // Initialize MCP if needed
            claudeApiService.initializeMCP()

            // Ask Claude to get reminders summary
            // Claude will use the MCP tool "get_reminders_summary" automatically
            System.err.println("[ReminderScheduler] Asking Claude to get reminders summary...")

            val request = ClaudeRequest(
                maxTokens = 1024,
                messages = listOf(
                    ClaudeMessage(
                        role = "user",
                        content = ClaudeMessageContent.Text(
                            "Пожалуйста, получи сводку по всем напоминаниям. " +
                            "Используй инструмент get_reminders_summary для получения информации. " +
                            "Предоставь краткую сводку на русском языке."
                        )
                    )
                )
            )

            val response = claudeApiService.sendMessage(request)

            // Extract text from response
            val summaryText = response.content
                ?.firstOrNull { it.type == "text" }
                ?.text
                ?: "Не удалось получить сводку"

            System.err.println("[ReminderScheduler] Summary result: ${summaryText.take(100)}")

            // Create alert
            val alert = UserAlertDto(
                id = UUID.randomUUID().toString(),
                title = "📋 Сводка по напоминаниям",
                message = summaryText,
                severity = AlertSeverityDto.INFO,
                category = "reminder_summary"
            )

            // Broadcast alert to all clients
            _alertFlow.emit(alert)

            System.err.println("[ReminderScheduler] Alert broadcasted successfully")

        } catch (e: Exception) {
            System.err.println("[ReminderScheduler] Error in generateAndBroadcastSummary: ${e.message}")
            e.printStackTrace()

            // Send error alert
            val errorAlert = UserAlertDto(
                id = UUID.randomUUID().toString(),
                title = "⚠️ Ошибка получения сводки",
                message = "Не удалось получить сводку по напоминаниям: ${e.message}",
                severity = AlertSeverityDto.ERROR,
                category = "reminder_error"
            )
            _alertFlow.emit(errorAlert)
        }
    }

    /**
     * Manually trigger summary generation
     */
    suspend fun triggerManualSummary() {
        System.err.println("[ReminderScheduler] Manual summary triggered")
        generateAndBroadcastSummary()
    }
}
