# Document Indexing

A complete document indexing pipeline for extracting, chunking, embedding, and indexing project files.

## Overview

This implementation provides a full document indexing pipeline that:

1. **Crawls** the project structure via MCP server
2. **Extracts** text content from supported file types
3. **Chunks** documents with 1024-token chunks and 100-token overlap
4. **Generates** embeddings using Ollama (nomic-embed-text)
5. **Indexes** vectors in a FAISS-like index for semantic search
6. **Persists** index and metadata to disk

## Architecture

Following **Clean Architecture** and **DRY** principles, the solution is organized into layers:

```
server-doc-indexer/
├── domain/
│   ├── models/          # Domain models (DocumentChunk, IndexMetadata)
│   ├── chunking/        # Text chunking logic
│   ├── crawler/         # File crawler
│   └── pipeline/        # Main orchestrator
└── data/
    ├── mcp/             # MCP client for filesystem access
    ├── ollama/          # Ollama embedding client
    └── index/           # Vector index implementation
```

### Key Components

#### 1. **FilesystemToolProvider** (`mcp-server-devops`)
- Provides MCP tools: `list_directory`, `read_file`
- Read-only filesystem access
- Security: prevents directory traversal
- Binary file detection

#### 2. **MCPClient** (`data/mcp/MCPClient.kt`)
- Communicates with MCP server via JSON-RPC
- Abstracts filesystem operations
- HTTP-based (Ktor client)

#### 3. **TextChunker** (`domain/chunking/TextChunker.kt`)
- Whitespace tokenization
- Configurable chunk size (1024 tokens)
- Exactly 100-token overlap between chunks
- Returns chunks with token boundaries

#### 4. **OllamaClient** (`data/ollama/OllamaClient.kt`)
- Generates embeddings via Ollama API
- Model: `nomic-embed-text` (768-dimensional)
- Supports batch processing

#### 5. **VectorIndex** (`data/index/VectorIndex.kt`)
- In-memory vector store
- Cosine similarity search
- JSON-based persistence
- Stores embeddings + metadata

#### 6. **FileCrawler** (`domain/crawler/FileCrawler.kt`)
- Recursive directory traversal via MCP
- Filters by extension (`.kt`, `.kts`, `.gradle`, `.md`, `.txt`, `.java`, `.xml`)
- Excludes: `build/`, `.gradle/`, `.git/`, `.idea/`, `node_modules/`
- Size limit: 1 MB per file

#### 7. **IndexingPipeline** (`domain/pipeline/IndexingPipeline.kt`)
- Orchestrates entire process
- Progress reporting
- Error handling
- Saves index + metadata

## Prerequisites

### 1. Start MCP DevOps Server

```bash
./start-devops-mcp.sh
```

The server provides filesystem tools on port 8082.

### 2. Install and Start Ollama

```bash
# Install Ollama (if not already installed)
# https://ollama.ai

# Pull the embedding model
ollama pull nomic-embed-text

# Verify Ollama is running
curl http://localhost:11434/api/tags
```

## Usage

### Quick Start

```bash
./run-indexer.sh
```

### Manual Execution

```bash
./gradlew :server-doc-indexer:run
```

### With Custom Parameters

```bash
./gradlew :server-doc-indexer:run --args="http://localhost:8082 http://localhost:11434 ./my-index"
```

**Arguments:**
1. MCP server URL (default: `http://localhost:8082`)
2. Ollama server URL (default: `http://localhost:11434`)
3. Output directory (default: `./faiss_index`)

## Output

The indexer creates:

### 1. **project.index**
JSON file containing:
- Vector embeddings (768-dimensional)
- Chunk IDs
- Metadata (file path, token range, file type)

### 2. **metadata.json**
JSON file containing:
- All chunk metadata
- Chunk content (for retrieval)
- Indexing statistics
- Configuration (chunk size, overlap, model)

## Example Metadata Structure

```json
{
  "chunks": [
    {
      "chunkId": "123e4567-e89b-12d3-a456-426614174000",
      "filePath": "./server/build.gradle.kts",
      "content": "plugins { alias(libs.plugins.kotlinJvm) ...",
      "startTokenIndex": 0,
      "endTokenIndex": 1024,
      "fileType": "kts"
    }
  ],
  "totalFiles": 42,
  "totalChunks": 156,
  "indexedAt": "2025-12-22T18:00:00Z",
  "chunkSize": 1024,
  "overlapSize": 100,
  "embeddingModel": "nomic-embed-text"
}
```

## How It Works

### 1. File Discovery
```kotlin
val files = fileCrawler.crawl(".")
// Uses MCP list_directory recursively
// Filters by extension and excludes build dirs
```

### 2. Content Extraction
```kotlin
val content = fileCrawler.readFile(fileItem)
// Uses MCP read_file
// Skips binary files and files > 1MB
```

### 3. Text Chunking
```kotlin
val chunks = textChunker.chunk(content)
// Splits into 1024-token chunks
// 100-token overlap between adjacent chunks
// Whitespace tokenization
```

Example:
```
Chunk 1: tokens 0-1024
Chunk 2: tokens 924-1948  (100 token overlap with chunk 1)
Chunk 3: tokens 1848-2872 (100 token overlap with chunk 2)
```

### 4. Embedding Generation
```kotlin
val embedding = ollamaClient.embed(chunk.content)
// Returns 768-dimensional vector
// Model: nomic-embed-text
```

### 5. Indexing
```kotlin
vectorIndex.add(chunk.copy(embedding = embedding))
// Stores vector + metadata
// Enables cosine similarity search
```

### 6. Persistence
```kotlin
vectorIndex.save(File("./faiss_index/project.index"))
// Saves as JSON to disk
```

## Code Example: Using the Index

```kotlin
import ru.alekseev.indexer.data.index.VectorIndex
import ru.alekseev.indexer.data.ollama.OllamaClient
import java.io.File

suspend fun searchIndex(query: String) {
    // Load the index
    val index = VectorIndex()
    index.load(File("./faiss_index/project.index"))

    // Generate query embedding
    val ollamaClient = OllamaClient()
    val queryEmbedding = ollamaClient.embed(query)

    // Search
    val results = index.search(queryEmbedding, topK = 5)

    results.forEach { result ->
        println("Similarity: ${result.similarity}")
        println("File: ${result.metadata.filePath}")
        println("Tokens: ${result.metadata.startToken}-${result.metadata.endToken}")
        println()
    }
}
```

## Implementation Details

### Chunking Algorithm

```kotlin
fun chunk(text: String): List<TextChunk> {
    val tokens = text.split(Regex("\\s+")).filter { it.isNotBlank() }

    val chunks = mutableListOf<TextChunk>()
    var startIndex = 0

    while (startIndex < tokens.size) {
        val endIndex = minOf(startIndex + chunkSize, tokens.size)

        chunks.add(TextChunk(
            content = tokens.subList(startIndex, endIndex).joinToString(" "),
            startTokenIndex = startIndex,
            endTokenIndex = endIndex
        ))

        // Move forward by (chunkSize - overlapSize)
        // This ensures exactly overlapSize tokens overlap
        startIndex += (chunkSize - overlapSize)

        if (endIndex >= tokens.size) break
    }

    return chunks
}
```

### Security Features

1. **Path Traversal Protection**
   - All paths validated against project root
   - Canonical path checking

2. **Binary File Detection**
   - Checks for null bytes
   - Analyzes non-printable character ratio
   - Skips unreadable files gracefully

3. **Resource Limits**
   - 1 MB file size limit
   - Prevents OOM on large files

## Performance Considerations

- **Chunking**: O(n) where n = number of tokens
- **Embedding**: Rate-limited by Ollama (CPU/GPU bound)
- **Search**: O(m) where m = number of indexed chunks (brute-force cosine similarity)

### Optimization Tips

1. **Batch Embeddings**: Process multiple chunks in parallel
2. **Incremental Indexing**: Skip already-indexed files
3. **FAISS Native**: For large-scale (10K+ chunks), use native FAISS with HNSW index

## Troubleshooting

### MCP Server Not Running
```bash
./start-devops-mcp.sh
# Verify: curl http://localhost:8082/health
```

### Ollama Not Running
```bash
ollama serve
# In another terminal: ollama pull nomic-embed-text
```

### Out of Memory
- Reduce batch size in `IndexingPipeline`
- Process files incrementally
- Increase JVM heap: `./gradlew -Xmx2g :server-doc-indexer:run`

### Slow Embedding Generation
- Use GPU-accelerated Ollama
- Reduce chunk size (e.g., 512 tokens)
- Skip large documentation files

## Future Enhancements

1. **Incremental Updates**: Track file modifications, re-index only changed files
2. **FAISS Native Bindings**: Use `faiss-java` for production-scale indexing
3. **Multi-Model Support**: Allow different embedding models
4. **Metadata Enrichment**: Add code language detection, AST parsing
5. **Distributed Indexing**: Process files in parallel across workers
6. **Semantic Deduplication**: Detect and merge similar chunks

## Summary

✅ **Clean Architecture**: Domain/Data separation
✅ **DRY Principles**: Reusable components
✅ **MCP Integration**: Filesystem access via tools
✅ **Configurable**: Chunk size, overlap, model
✅ **Persistent**: Saves index + metadata to disk
✅ **Production-Ready**: Error handling, logging, security

**Complete!** 🎉
