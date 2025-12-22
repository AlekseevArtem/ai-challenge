package ru.alekseev.indexer.domain.crawler

import ru.alekseev.indexer.data.mcp.MCPClient
import ru.alekseev.indexer.domain.models.FileItem

/**
 * Crawls the project directory structure via MCP server
 * to find all files matching the specified criteria.
 */
class FileCrawler(
    private val mcpClient: MCPClient
) {
    companion object {
        // Supported file extensions
        private val SUPPORTED_EXTENSIONS = setOf(
            "kt", "kts", "gradle", "md", "txt", "java", "xml"
        )

        // Directories to exclude
        private val EXCLUDED_DIRS = setOf(
            "build", ".gradle", ".git", ".idea", "node_modules"
        )

        // Max file size (1 MB)
        private const val MAX_FILE_SIZE = 1 * 1024 * 1024L
    }

    /**
     * Crawl the entire project starting from the root
     */
    suspend fun crawl(rootPath: String = "."): List<FileItem> {
        val files = mutableListOf<FileItem>()
        crawlDirectory(rootPath, files)
        return files
    }

    /**
     * Recursively crawl a directory
     */
    private suspend fun crawlDirectory(path: String, accumulator: MutableList<FileItem>) {
        try {
            val entries = mcpClient.listDirectory(path, recursive = false)

            for (entry in entries) {
                val fullPath = if (path == ".") entry.name else "$path/${entry.name}"

                if (entry.isDirectory) {
                    // Skip excluded directories
                    if (entry.name !in EXCLUDED_DIRS && !entry.name.startsWith(".")) {
                        crawlDirectory(fullPath, accumulator)
                    }
                } else {
                    // Process file
                    val extension = entry.name.substringAfterLast('.', "")
                    if (extension in SUPPORTED_EXTENSIONS) {
                        // We don't have size info from list_directory, so we'll check it during read
                        accumulator.add(
                            FileItem(
                                path = fullPath,
                                name = entry.name,
                                sizeBytes = 0, // Will be checked during read
                                extension = extension
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            System.err.println("Warning: Failed to crawl directory $path: ${e.message}")
        }
    }

    /**
     * Read and validate a file
     */
    suspend fun readFile(fileItem: FileItem): String? {
        return try {
            val content = mcpClient.readFile(fileItem.path)

            // Check size after reading
            if (content.length > MAX_FILE_SIZE) {
                System.err.println("Skipping large file: ${fileItem.path} (${content.length} bytes)")
                return null
            }

            content
        } catch (e: Exception) {
            System.err.println("Warning: Failed to read file ${fileItem.path}: ${e.message}")
            null
        }
    }
}
