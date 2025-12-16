package ru.alekseev.myapplication.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import ru.alekseev.myapplication.data.dto.*
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.util.concurrent.atomic.AtomicInteger

/**
 * MCP Client for communicating with MCP servers via stdio.
 *
 * @param command Command to start the MCP server process
 * @param environment Environment variables for the MCP server process
 * @param workingDirectory Working directory for the MCP server process
 */
class MCPClient(
    private val name: String,
    private val command: List<String>,
    private val environment: Map<String, String> = emptyMap(),
    private val workingDirectory: String? = null
) {
    private var process: Process? = null
    private var writer: BufferedWriter? = null
    private var reader: BufferedReader? = null
    private val requestIdCounter = AtomicInteger(0)
    private val mutex = Mutex()

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    var isConnected = false
        private set

    /**
     * Start the MCP server process and initialize the connection
     */
    suspend fun connect(): MCPInitializeResult = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (isConnected) {
                throw IllegalStateException("MCP client is already connected")
            }

            // Start the process
            val processBuilder = ProcessBuilder(command).apply {
                environment().putAll(this@MCPClient.environment)
                workingDirectory?.let { directory(java.io.File(it)) }
                redirectErrorStream(false)
            }

            process = processBuilder.start()

            // Setup streams
            writer = BufferedWriter(OutputStreamWriter(process!!.outputStream))
            reader = BufferedReader(InputStreamReader(process!!.inputStream))

            // Send initialize request
            val initRequest = MCPRequest(
                id = requestIdCounter.incrementAndGet(),
                method = "initialize",
                params = null
            )

            val response = sendRequest(initRequest)

            response.error?.let { error ->
                disconnect()
                throw Exception("Failed to initialize MCP server: ${error.message}")
            }

            val result = response.result?.let {
                json.decodeFromJsonElement(MCPInitializeResult.serializer(), it)
            } ?: throw Exception("No result in initialize response")

            isConnected = true
            result
        }
    }

    /**
     * Get the list of available tools from the MCP server
     */
    suspend fun listTools(): List<MCPTool> = withContext(Dispatchers.IO) {
        ensureConnected()

        val request = MCPRequest(
            id = requestIdCounter.incrementAndGet(),
            method = "tools/list",
            params = null
        )

        val response = sendRequest(request)

        response.error?.let { error ->
            throw Exception("Failed to list tools: ${error.message}")
        }

        val result = response.result?.let {
            json.decodeFromJsonElement(MCPListToolsResult.serializer(), it)
        } ?: throw Exception("No result in tools/list response")

        result.tools
    }

    /**
     * Call a tool on the MCP server
     */
    suspend fun callTool(toolName: String, arguments: kotlinx.serialization.json.JsonObject?): MCPCallToolResult =
        withContext(Dispatchers.IO) {
            ensureConnected()

            val params = json.encodeToJsonElement(
                MCPCallToolParams.serializer(),
                MCPCallToolParams(name = toolName, arguments = arguments)
            )

            val request = MCPRequest(
                id = requestIdCounter.incrementAndGet(),
                method = "tools/call",
                params = params as kotlinx.serialization.json.JsonObject
            )

            val response = sendRequest(request)

            response.error?.let { error ->
                throw Exception("Failed to call tool '$toolName': ${error.message}")
            }

            response.result?.let {
                json.decodeFromJsonElement(MCPCallToolResult.serializer(), it)
            } ?: throw Exception("No result in tools/call response")
        }

    /**
     * Send a request to the MCP server and wait for response
     */
    private fun sendRequest(request: MCPRequest): MCPResponse {
        val requestJson = json.encodeToString(MCPRequest.serializer(), request)

        // Write request
        writer?.write(requestJson)
        writer?.newLine()
        writer?.flush()

        // Read response
        val responseLine = reader?.readLine()
            ?: throw Exception("Failed to read response from MCP server")

        // Try to parse as JSON, if it fails, it might be an error message from the server
        return try {
            json.decodeFromString(MCPResponse.serializer(), responseLine)
        } catch (e: Exception) {
            // If parsing fails, the server probably output non-JSON (e.g., OAuth URL)
            System.err.println("Failed to parse MCP response as JSON: $responseLine")
            // Return an error response
            MCPResponse(
                jsonrpc = "2.0",
                id = request.id,
                result = null,
                error = MCPError(
                    code = -1,
                    message = "MCP server returned non-JSON response: ${responseLine.take(100)}"
                )
            )
        }
    }

    /**
     * Disconnect from the MCP server and cleanup resources
     */
    suspend fun disconnect() = withContext(Dispatchers.IO) {
        mutex.withLock {
            try {
                writer?.close()
                reader?.close()
                process?.destroy()
                process?.waitFor()
            } catch (e: Exception) {
                // Log but don't throw
                System.err.println("Error during MCP client disconnect: ${e.message}")
            } finally {
                writer = null
                reader = null
                process = null
                isConnected = false
            }
        }
    }

    private fun ensureConnected() {
        if (!isConnected) {
            throw IllegalStateException("MCP client is not connected. Call connect() first.")
        }
    }

    override fun toString(): String = "MCPClient(name='$name', connected=$isConnected)"
}
