package ru.alekseev.indexer.data.mcp

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.*
import ru.alekseev.myapplication.core.common.JsonFactory
import ru.alekseev.myapplication.core.common.MCPDefaults

/**
 * Client for communicating with the MCP server
 */
class MCPClient(
    private val baseUrl: String = MCPDefaults.DEFAULT_BASE_URL
) {
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(JsonFactory.create())
        }
    }

    private var requestId = 0L

    /**
     * List files and directories at the given path
     */
    suspend fun listDirectory(path: String, recursive: Boolean = false): List<FileEntry> {
        val response = callTool(
            toolName = "list_directory",
            arguments = buildJsonObject {
                put("path", path)
                put("recursive", recursive)
            }
        )

        // Parse the response text to extract file entries
        return parseDirectoryListing(response)
    }

    /**
     * Read the contents of a file
     */
    suspend fun readFile(path: String): String {
        return callTool(
            toolName = "read_file",
            arguments = buildJsonObject {
                put("path", path)
            }
        )
    }

    /**
     * Call an MCP tool
     */
    private suspend fun callTool(toolName: String, arguments: JsonObject): String {
        val requestBody = buildJsonObject {
            put("jsonrpc", "2.0")
            put("method", "tools/call")
            put("id", ++requestId)
            putJsonObject("params") {
                put("name", toolName)
                put("arguments", arguments)
            }
        }

        val response: JsonObject = client.post("$baseUrl${ru.alekseev.myapplication.core.common.ApiEndpoints.MCP}") {
            contentType(ContentType.Application.Json)
            setBody(requestBody)
        }.body()

        // Extract result from JSON-RPC response
        val result = response["result"]?.jsonObject
            ?: throw Exception("No result in response: $response")

        val content = result["content"]?.jsonArray?.firstOrNull()?.jsonObject
            ?: throw Exception("No content in result: $result")

        return content["text"]?.jsonPrimitive?.content
            ?: throw Exception("No text in content: $content")
    }

    /**
     * Parse directory listing text into FileEntry objects
     */
    private fun parseDirectoryListing(listing: String): List<FileEntry> {
        val entries = mutableListOf<FileEntry>()
        val lines = listing.lines()

        for (line in lines) {
            // Parse lines like: "FILE  filename.kt                     1 KB"
            // or: "DIR   foldername                     -"
            if (line.startsWith("FILE") || line.startsWith("DIR")) {
                val parts = line.split(Regex("\\s+"))
                if (parts.size >= 2) {
                    val type = parts[0]
                    val name = parts[1]
                    val isDirectory = type == "DIR"

                    entries.add(FileEntry(name, isDirectory))
                }
            }
        }

        return entries
    }

    fun close() {
        client.close()
    }
}

/**
 * Represents a file or directory entry
 */
data class FileEntry(
    val name: String,
    val isDirectory: Boolean
)
