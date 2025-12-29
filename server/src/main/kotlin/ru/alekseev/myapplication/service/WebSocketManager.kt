package ru.alekseev.myapplication.service

import io.ktor.websocket.DefaultWebSocketSession
import io.ktor.websocket.Frame
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import ru.alekseev.myapplication.data.dto.ChatResponseDto
import ru.alekseev.myapplication.data.dto.UserAlertDto
import ru.alekseev.myapplication.core.common.logTag
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages WebSocket connections and broadcasts messages to all connected clients
 */
class WebSocketManager(
    private val json: Json,
    private val reminderScheduler: ReminderSchedulerService
) {
    private val connections = ConcurrentHashMap<String, DefaultWebSocketSession>()
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var alertListenerJob: Job? = null

    /**
     * Register a new WebSocket connection
     */
    fun registerConnection(id: String, session: DefaultWebSocketSession) {
        connections[id] = session
        System.err.println("$logTag Connection registered: $id (Total: ${connections.size})")

        // Start alert listener if this is the first connection
        if (connections.size == 1 && alertListenerJob == null) {
            startAlertListener()
        }
    }

    /**
     * Unregister a WebSocket connection
     */
    fun unregisterConnection(id: String) {
        connections.remove(id)
        System.err.println("$logTag Connection unregistered: $id (Total: ${connections.size})")

        // Stop alert listener if no more connections
        if (connections.isEmpty()) {
            stopAlertListener()
        }
    }

    /**
     * Broadcast an alert to all connected clients
     */
    suspend fun broadcastAlert(alert: UserAlertDto) {
        System.err.println("$logTag Encoding alert: $alert")

        val message = json.encodeToString(
            ChatResponseDto.serializer(),
            ChatResponseDto.Alert(alert)
        )

        System.err.println("$logTag Encoded message: '$message'")
        System.err.println("$logTag Message length: ${message.length}")

        val frame = Frame.Text(message)
        val deadConnections = mutableListOf<String>()

        connections.forEach { (id, session) ->
            try {
                session.send(frame)
                System.err.println("$logTag Alert sent to connection: $id")
            } catch (e: Exception) {
                System.err.println("$logTag Failed to send alert to $id: ${e.message}")
                e.printStackTrace()
                deadConnections.add(id)
            }
        }

        // Remove dead connections
        deadConnections.forEach { unregisterConnection(it) }

        System.err.println("$logTag Alert broadcasted to ${connections.size} connections")
    }

    /**
     * Start listening to alerts from ReminderScheduler
     */
    private fun startAlertListener() {
        System.err.println("$logTag Starting alert listener...")

        alertListenerJob = scope.launch {
            reminderScheduler.alertFlow.collect { alert ->
                System.err.println("$logTag Received alert: ${alert.title}")
                broadcastAlert(alert)
            }
        }

        System.err.println("$logTag Alert listener started")
    }

    /**
     * Stop listening to alerts
     */
    private fun stopAlertListener() {
        System.err.println("$logTag Stopping alert listener...")
        alertListenerJob?.cancel()
        alertListenerJob = null
        System.err.println("$logTag Alert listener stopped")
    }

    /**
     * Get number of active connections
     */
    fun getConnectionCount(): Int = connections.size

    /**
     * Close all connections
     */
    suspend fun closeAll() {
        System.err.println("$logTag Clearing all connections...")
        connections.clear()
        stopAlertListener()
        System.err.println("$logTag All connections cleared")
    }
}
