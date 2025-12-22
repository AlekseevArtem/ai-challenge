package ru.alekseev.mcp.devops

import kotlinx.serialization.json.*
import ru.alekseev.mcp.devops.models.Tool
import java.io.File
import java.nio.file.Files

/**
 * MCP Tool Provider for read-only filesystem operations.
 * Provides tools for listing directories and reading files.
 */
class FilesystemToolProvider(
    private val projectRoot: String = System.getProperty("user.dir")
) : MCPToolProvider {

    override fun getTools(): List<Tool> = listOf(
        Tool(
            name = "list_directory",
            description = "List all files and directories in the specified path. Returns file names, types (file/directory), and sizes.",
            inputSchema = buildJsonObject {
                put("type", "object")
                putJsonObject("properties") {
                    putJsonObject("path") {
                        put("type", "string")
                        put("description", "Relative or absolute path to list. If relative, resolved from project root.")
                    }
                    putJsonObject("recursive") {
                        put("type", "boolean")
                        put("description", "Whether to list recursively (default: false)")
                    }
                }
                putJsonArray("required") {
                    add("path")
                }
            }
        ),
        Tool(
            name = "read_file",
            description = "Read the contents of a text file. Returns the file content as a string.",
            inputSchema = buildJsonObject {
                put("type", "object")
                putJsonObject("properties") {
                    putJsonObject("path") {
                        put("type", "string")
                        put("description", "Relative or absolute path to the file. If relative, resolved from project root.")
                    }
                }
                putJsonArray("required") {
                    add("path")
                }
            }
        )
    )

    override fun handleToolCall(toolName: String, arguments: JsonObject?): String {
        return when (toolName) {
            "list_directory" -> handleListDirectory(arguments)
            "read_file" -> handleReadFile(arguments)
            else -> throw IllegalArgumentException("Unknown tool: $toolName")
        }
    }

    private fun handleListDirectory(arguments: JsonObject?): String {
        val pathArg = arguments?.get("path")?.jsonPrimitive?.content
            ?: throw IllegalArgumentException("path is required")

        val recursive = arguments["recursive"]?.jsonPrimitive?.booleanOrNull ?: false

        val targetPath = resolvePathSafely(pathArg)

        if (!targetPath.exists()) {
            throw IllegalArgumentException("Path does not exist: ${targetPath.absolutePath}")
        }

        if (!targetPath.isDirectory) {
            throw IllegalArgumentException("Path is not a directory: ${targetPath.absolutePath}")
        }

        return if (recursive) {
            buildRecursiveDirectoryListing(targetPath)
        } else {
            buildDirectoryListing(targetPath)
        }
    }

    private fun buildDirectoryListing(dir: File): String {
        val files = dir.listFiles()?.sortedWith(compareBy({ !it.isDirectory }, { it.name }))
            ?: return "Directory is empty or cannot be read"

        val result = StringBuilder()
        result.appendLine("Directory: ${dir.absolutePath}")
        result.appendLine("Total items: ${files.size}")
        result.appendLine()

        files.forEach { file ->
            val type = if (file.isDirectory) "DIR " else "FILE"
            val size = if (file.isFile) formatFileSize(file.length()) else "-"
            result.appendLine("$type  ${file.name.padEnd(50)}  $size")
        }

        return result.toString()
    }

    private fun buildRecursiveDirectoryListing(dir: File): String {
        val result = StringBuilder()
        result.appendLine("Recursive listing of: ${dir.absolutePath}")
        result.appendLine()

        walkDirectory(dir, "", result)

        return result.toString()
    }

    private fun walkDirectory(dir: File, indent: String, result: StringBuilder) {
        val files = dir.listFiles()?.sortedWith(compareBy({ !it.isDirectory }, { it.name })) ?: return

        files.forEach { file ->
            val type = if (file.isDirectory) "DIR " else "FILE"
            val size = if (file.isFile) formatFileSize(file.length()) else ""
            result.appendLine("$indent$type  ${file.name}  $size")

            if (file.isDirectory) {
                walkDirectory(file, "$indent  ", result)
            }
        }
    }

    private fun handleReadFile(arguments: JsonObject?): String {
        val pathArg = arguments?.get("path")?.jsonPrimitive?.content
            ?: throw IllegalArgumentException("path is required")

        val targetFile = resolvePathSafely(pathArg)

        if (!targetFile.exists()) {
            throw IllegalArgumentException("File does not exist: ${targetFile.absolutePath}")
        }

        if (!targetFile.isFile) {
            throw IllegalArgumentException("Path is not a file: ${targetFile.absolutePath}")
        }

        // Check if file is binary
        if (isBinaryFile(targetFile)) {
            throw IllegalArgumentException("Cannot read binary file: ${targetFile.name}")
        }

        // Read file content
        return try {
            targetFile.readText(Charsets.UTF_8)
        } catch (e: Exception) {
            throw IllegalArgumentException("Failed to read file: ${e.message}")
        }
    }

    /**
     * Resolve path safely, preventing directory traversal attacks
     */
    private fun resolvePathSafely(pathArg: String): File {
        val baseDir = File(projectRoot).canonicalFile

        // Strip leading slash to treat all paths as relative to project root
        // This handles cases where Claude passes "/server/..." as a path
        val normalizedPath = pathArg.trimStart('/')

        val targetFile = File(baseDir, normalizedPath).canonicalFile

        // Ensure the target is within the project root
        if (!targetFile.canonicalPath.startsWith(baseDir.canonicalPath)) {
            throw IllegalArgumentException("Access denied: path outside project root")
        }

        return targetFile
    }

    /**
     * Check if a file is binary by examining the first 512 bytes
     */
    private fun isBinaryFile(file: File): Boolean {
        if (file.length() == 0L) return false

        // Whitelist of known text file extensions
        val textExtensions = setOf(
            "kt", "kts", "java", "gradle", "md", "txt", "xml", "json", "yml", "yaml",
            "properties", "conf", "sh", "bat", "py", "js", "ts", "jsx", "tsx", "css",
            "scss", "html", "htm", "sql", "c", "cpp", "h", "hpp", "rs", "go", "rb",
            "php", "swift", "m", "mm", "pl", "r", "scala", "groovy", "toml", "ini"
        )

        val extension = file.extension.lowercase()
        if (extension in textExtensions) {
            return false // Known text file, don't check content
        }

        val sampleSize = minOf(512, file.length().toInt())
        val bytes = file.inputStream().use { it.readNBytes(sampleSize) }

        // Check for null bytes or high ratio of non-printable characters
        var nonPrintable = 0
        bytes.forEach { byte ->
            if (byte.toInt() == 0) return true // Null byte = binary
            if (byte < 0x20 && byte != 0x09.toByte() && byte != 0x0A.toByte() && byte != 0x0D.toByte()) {
                nonPrintable++
            }
        }

        // Increase threshold to 50% (less strict)
        return nonPrintable.toDouble() / bytes.size > 0.5
    }

    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            else -> "${bytes / (1024 * 1024)} MB"
        }
    }
}
