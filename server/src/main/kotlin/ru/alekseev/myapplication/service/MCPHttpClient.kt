package ru.alekseev.myapplication.service

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import ru.alekseev.myapplication.data.dto.*
import java.util.concurrent.atomic.AtomicInteger

/**
 * HTTP-based MCP Client for communicating with MCP servers over HTTP.
 * Used for connecting to MCP servers running on the host machine from Docker.
 *
 * @param name Client name for logging
 * @param baseUrl Base URL of the MCP server (e.g., "http://host.docker.internal:8082")
 */
class MCPHttpClient(
    private val name: String,
    private val baseUrl: String
) {
    private val requestIdCounter = AtomicInteger(0)
    private val mutex = Mutex()

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(this@MCPHttpClient.json)
        }

        // Configure timeouts for long-running operations like builds
        install(HttpTimeout) {
            requestTimeoutMillis = 600_000 // 10 minutes for builds
            connectTimeoutMillis = 10_000  // 10 seconds to connect
            socketTimeoutMillis = 600_000  // 10 minutes for socket read/write
        }

        // Configure engine timeouts
        engine {
            requestTimeout = 600_000 // 10 minutes
        }
    }

    var isConnected = false
        private set

    /**
     * Initialize the connection to the HTTP MCP server
     */
    suspend fun connect(): MCPInitializeResult = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (isConnected) {
                throw IllegalStateException("MCP HTTP client is already connected")
            }

            // Check health endpoint first
            try {
                val healthResponse = httpClient.get("$baseUrl/health")
                if (healthResponse.status != HttpStatusCode.OK) {
                    throw Exception("Health check failed with status: ${healthResponse.status}")
                }
            } catch (e: Exception) {
                throw Exception("Failed to connect to MCP server at $baseUrl: ${e.message}")
            }

            // Send initialize request
            val initParams = MCPInitializeParams(
                protocolVersion = "2024-11-05",
                capabilities = MCPClientCapabilities(),
                clientInfo = MCPClientInfo(
                    name = name,
                    version = "1.0.0"
                )
            )

            val initRequest = MCPRequest(
                id = requestIdCounter.incrementAndGet(),
                method = "initialize",
                params = json.encodeToJsonElement(MCPInitializeParams.serializer(), initParams) as kotlinx.serialization.json.JsonObject
            )

            val response = sendRequestUnsafe(initRequest)

            response.error?.let { error ->
                throw Exception("Failed to initialize MCP server: ${error.message}")
            }

            val result = response.result?.let {
                json.decodeFromJsonElement(MCPInitializeResult.serializer(), it)
            } ?: throw Exception("No result in initialize response")

            isConnected = true
            println("[$name] Successfully connected to HTTP MCP server at $baseUrl")
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
     * Send a request to the MCP server via HTTP and wait for response
     */
    private suspend fun sendRequest(request: MCPRequest): MCPResponse = mutex.withLock {
        sendRequestUnsafe(request)
    }

    /**
     * Internal version of sendRequest without mutex protection
     */
    private suspend fun sendRequestUnsafe(request: MCPRequest): MCPResponse {
        return try {
            val response = httpClient.post("$baseUrl/mcp") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }

            if (response.status != HttpStatusCode.OK) {
                MCPResponse(
                    jsonrpc = "2.0",
                    id = request.id,
                    result = null,
                    error = MCPError(
                        code = response.status.value,
                        message = "HTTP error: ${response.status.description}"
                    )
                )
            } else {
                response.body<MCPResponse>()
            }
        } catch (e: Exception) {
            System.err.println("[$name] Error sending HTTP request: ${e.message}")
            MCPResponse(
                jsonrpc = "2.0",
                id = request.id,
                result = null,
                error = MCPError(
                    code = -1,
                    message = "HTTP request failed: ${e.message}"
                )
            )
        }
    }

    /**
     * Disconnect from the MCP server
     */
    suspend fun disconnect() = withContext(Dispatchers.IO) {
        mutex.withLock {
            try {
                httpClient.close()
            } catch (e: Exception) {
                System.err.println("[$name] Error during disconnect: ${e.message}")
            } finally {
                isConnected = false
            }
        }
    }

    private fun ensureConnected() {
        if (!isConnected) {
            throw IllegalStateException("MCP HTTP client is not connected. Call connect() first.")
        }
    }

    override fun toString(): String = "MCPHttpClient(name='$name', baseUrl='$baseUrl', connected=$isConnected)"
}
