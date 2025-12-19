package ru.alekseev.mcp.devops

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json
import ru.alekseev.mcp.devops.models.JSONRPCRequest
import ru.alekseev.mcp.devops.models.JSONRPCResponse
import ru.alekseev.mcp.devops.models.JSONRPCError

/**
 * HTTP server wrapper for MCP Server.
 * Provides HTTP POST endpoint for MCP JSON-RPC requests.
 */
class MCPHttpServer(
    private val mcpServer: MCPServer,
    private val port: Int = 8082
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun start() {
        embeddedServer(Netty, port = port) {
            install(ContentNegotiation) {
                json(json)
            }

            routing {
                // Health check endpoint
                get("/health") {
                    call.respondText("OK")
                }

                // MCP JSON-RPC endpoint
                post("/mcp") {
                    try {
                        val request = call.receive<JSONRPCRequest>()
                        System.err.println("[MCPHttpServer] Received request: method=${request.method}, id=${request.id}")

                        val response = mcpServer.handleRequest(request)

                        call.respond(response)
                    } catch (e: Exception) {
                        System.err.println("[MCPHttpServer] Error handling request: ${e.message}")
                        e.printStackTrace(System.err)
                        call.respond(
                            JSONRPCResponse(
                                error = JSONRPCError(
                                    code = -32603,
                                    message = "Internal error: ${e.message}"
                                )
                            )
                        )
                    }
                }
            }
        }.start(wait = true)
    }
}
