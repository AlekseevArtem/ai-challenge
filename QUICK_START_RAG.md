# Quick Start: RAG-Powered Chat

Ask questions about your codebase and get accurate answers from Claude with automatic context retrieval.

## Prerequisites

```bash
# 1. Install Ollama (if not installed)
# Visit: https://ollama.ai

# 2. Pull the embedding model
ollama pull nomic-embed-text

# 3. Start Ollama (if not running)
# Ollama runs automatically on macOS after installation
# Check: curl http://localhost:11434/api/tags
```

## One-Time Setup: Create Document Index

```bash
# 1. Start MCP DevOps server (provides filesystem access for indexing)
./start-devops-mcp.sh

# 2. Run the indexer (in a new terminal)
./gradlew :server-doc-indexer:run
```

**Expected Output:**
```
=== Starting Document Indexing Pipeline ===

Step 1: Crawling project files...
Found 42 files to process

Step 2: Reading and chunking files...
  ./server/build.gradle.kts: 12 chunks
  ./server/src/main/kotlin/ru/alekseev/myapplication/Application.kt: 8 chunks
  ...

Total chunks created: 156
Files processed: 42

Step 3: Generating embeddings using Ollama...
  Progress: 10 / 156
  Progress: 20 / 156
  ...

Step 4: Building vector index...
Index size: 156 vectors

Step 5: Saving index to disk...
Index saved to: /Users/.../faiss_index/project.index
Metadata saved to: /Users/.../faiss_index/metadata.json

=== Indexing Complete ===
```

**Note:** This creates `./faiss_index/` with your project's knowledge base.

## Running the Server

```bash
# Start the server (RAG loads automatically)
./gradlew :server:run
```

**Expected Output:**
```
[DocumentRAGService] Loading index from ./faiss_index/project.index
[DocumentRAGService] Index loaded successfully with 156 vectors
[DocumentRAGService] Metadata loaded: 156 chunks from 42 files
Document RAG Service initialized successfully
  - Total vectors: 156
  - Total files: 42
  - Indexed at: 2025-12-22T18:00:00Z
```

## Usage: Ask Questions

Connect to the server via WebSocket at `ws://localhost:8080/chat` and ask questions about your project!

### Example Questions

**About the code:**
- "Which Claude LLM model is used in this project?"
- "How does the MCP integration work?"
- "Show me the database schema"
- "What is the chat message flow?"
- "How are websockets configured?"

**About configuration:**
- "What dependencies does the server use?"
- "How is Koin DI configured?"
- "What ports does the server use?"

**About architecture:**
- "Explain the project structure"
- "How is Clean Architecture implemented?"
- "What are the main modules?"

### How It Works

1. **You ask:** "Which Claude model is used?"
2. **RAG searches:** Finds relevant code chunks from `ClaudeApiService.kt`
3. **Context added:** Code snippets added to Claude prompt
4. **Claude answers:** With accurate information from your codebase
5. **You get:** Correct answer based on actual code!

## Re-indexing

Update the index after making code changes:

```bash
# Make sure MCP server is running
./start-devops-mcp.sh

# Re-run the indexer
./gradlew :server-doc-indexer:run
```

**Tip:** Re-index after:
- Adding new files
- Major code changes
- Updating documentation

## Troubleshooting

### "Index not found"
```bash
# Run the indexer first
./gradlew :server-doc-indexer:run
```

### "Ollama connection failed"
```bash
# Check if Ollama is running
curl http://localhost:11434/api/tags

# If not, start it (it usually auto-starts on macOS)
# Or pull the model again
ollama pull nomic-embed-text
```

### "MCP server not running"
```bash
# Start the MCP server
./start-devops-mcp.sh

# Verify it's running
curl http://localhost:8082/health
```

## Architecture

```
┌─────────────────────────────────────────────────┐
│                  User Query                     │
│        "Which Claude model is used?"            │
└────────────────┬────────────────────────────────┘
                 │
                 v
┌─────────────────────────────────────────────────┐
│         ProcessUserMessageUseCase               │
│   (orchestrates message processing)             │
└────────────────┬────────────────────────────────┘
                 │
                 v
┌─────────────────────────────────────────────────┐
│          DocumentRAGService                     │
│   - Generate query embedding (Ollama)           │
│   - Search vector index (cosine similarity)     │
│   - Retrieve top 3 matching chunks              │
└────────────────┬────────────────────────────────┘
                 │
                 v
┌─────────────────────────────────────────────────┐
│              Context String                     │
│  "Here is relevant context:                     │
│   --- Document 1 (ClaudeApiService.kt) ---      │
│   class ClaudeApiService {                      │
│     val apiKey: String                          │
│     ...                                         │
│   }"                                            │
└────────────────┬────────────────────────────────┘
                 │
                 v
┌─────────────────────────────────────────────────┐
│            Claude API Call                      │
│   Prompt: [context + conversation + query]     │
└────────────────┬────────────────────────────────┘
                 │
                 v
┌─────────────────────────────────────────────────┐
│             Claude Response                     │
│  "This project uses Claude Sonnet 3.5           │
│   via the Anthropic API. The model is           │
│   configured in ClaudeApiService..."            │
└─────────────────────────────────────────────────┘
```

## Files Created

After indexing:

```
./faiss_index/
├── project.index      # Vector embeddings (JSON format)
│                      # Contains 768-dim vectors for each chunk
│
└── metadata.json      # Chunk content and mappings
                       # Maps chunk IDs to file paths and content
```

## Performance

**Small project (42 files, 156 chunks):**
- Indexing time: ~3-5 minutes
- Search time: < 100ms
- Index size: ~2 MB

**Large project (1000+ files, 5000+ chunks):**
- Indexing time: ~20-30 minutes
- Search time: < 500ms
- Index size: ~50 MB

## Summary

✅ **One-time setup:** Create index with `./gradlew :server-doc-indexer:run`
✅ **Auto-loading:** Server loads index on startup
✅ **Automatic RAG:** Context retrieved for every query
✅ **Accurate answers:** Based on actual codebase
✅ **No manual work:** Everything happens automatically

**You're ready to chat with your codebase!** 🎉
