#!/bin/bash

# Stop DevOps MCP Server
# This script stops the background DevOps MCP server

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

PID_FILE="devops-mcp-server.pid"

echo -e "${GREEN}=== Stopping DevOps MCP Server ===${NC}\n"

if [ ! -f "$PID_FILE" ]; then
    echo -e "${YELLOW}No PID file found. Server may not be running.${NC}"

    # Check if process is running on port 8082
    if lsof -Pi :8082 -sTCP:LISTEN -t >/dev/null 2>&1; then
        PID=$(lsof -t -i:8082)
        echo -e "${YELLOW}Found process on port 8082 (PID: $PID)${NC}"
        echo "Kill it with: kill $PID"
    fi
    exit 1
fi

PID=$(cat "$PID_FILE")

if kill -0 "$PID" 2>/dev/null; then
    echo "Stopping server (PID: $PID)..."
    kill "$PID"

    # Wait up to 5 seconds for graceful shutdown
    for i in {1..5}; do
        if ! kill -0 "$PID" 2>/dev/null; then
            break
        fi
        sleep 1
    done

    # Force kill if still running
    if kill -0 "$PID" 2>/dev/null; then
        echo "Forcing shutdown..."
        kill -9 "$PID"
    fi

    rm "$PID_FILE"
    echo -e "${GREEN}✓ Server stopped${NC}"
else
    echo -e "${YELLOW}Process $PID is not running${NC}"
    rm "$PID_FILE"
fi
