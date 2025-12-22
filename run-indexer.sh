#!/bin/bash

# Document Indexing Script
# This script runs the document indexer

echo "======================================"
echo "Document Indexing Pipeline"
echo "======================================"
echo ""

# Check if MCP server is running
echo "Checking prerequisites..."
if ! curl -s http://localhost:8082/health > /dev/null 2>&1; then
    echo "❌ MCP DevOps server is not running!"
    echo ""
    echo "Please start it first:"
    echo "  ./start-devops-mcp.sh"
    echo ""
    exit 1
fi
echo "✓ MCP server is running"

# Check if Ollama is running
if ! curl -s http://localhost:11434/api/tags > /dev/null 2>&1; then
    echo "❌ Ollama is not running!"
    echo ""
    echo "Please start Ollama and pull the embedding model:"
    echo "  ollama pull nomic-embed-text"
    echo ""
    exit 1
fi
echo "✓ Ollama is running"

# Check if nomic-embed-text model is available
if ! ollama list | grep -q "nomic-embed-text"; then
    echo "⚠ Warning: nomic-embed-text model might not be available"
    echo "  You may want to run: ollama pull nomic-embed-text"
    echo ""
fi

echo ""
echo "Starting indexing pipeline..."
echo ""

# Run the indexer
./gradlew :server-doc-indexer:run --quiet

echo ""
echo "Done!"
