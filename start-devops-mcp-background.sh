#!/bin/bash

# Start DevOps MCP Server in Background
# This script starts the DevOps MCP server as a background daemon
# The server will continue running even after terminal is closed

set -e

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

PORT=${1:-8082}
LOG_FILE="devops-mcp-server.log"
PID_FILE="devops-mcp-server.pid"

echo -e "${GREEN}=== DevOps MCP Server Background Startup ===${NC}\n"

# Check if already running
if [ -f "$PID_FILE" ]; then
    PID=$(cat "$PID_FILE")
    if kill -0 "$PID" 2>/dev/null; then
        echo -e "${YELLOW}DevOps MCP Server is already running (PID: $PID)${NC}"
        echo "To stop it: kill $PID"
        echo "To view logs: tail -f $LOG_FILE"
        exit 0
    else
        echo -e "${YELLOW}Removing stale PID file${NC}"
        rm "$PID_FILE"
    fi
fi

# Check if JAR exists
JAR_PATH="mcp-server-devops/build/libs/mcp-server-devops-1.0.0.jar"
if [ ! -f "$JAR_PATH" ]; then
    echo -e "${YELLOW}DevOps MCP server JAR not found. Building...${NC}"
    ./gradlew :mcp-server-devops:jar
    echo ""
fi

# Check if port is available
if lsof -Pi :"$PORT" -sTCP:LISTEN -t >/dev/null 2>&1; then
    echo -e "${RED}ERROR: Port $PORT is already in use${NC}"
    echo "Kill the process using: kill \$(lsof -t -i:$PORT)"
    exit 1
fi

# Check if Docker is running
if ! docker ps >/dev/null 2>&1; then
    echo -e "${YELLOW}WARNING: Docker is not running${NC}"
    echo "Docker containers won't be accessible"
    echo ""
fi

echo -e "${GREEN}Starting DevOps MCP Server on port $PORT in background...${NC}"
echo "Logs will be written to: $LOG_FILE"
echo ""

# Start the server in background
nohup java -jar "$JAR_PATH" "$PORT" > "$LOG_FILE" 2>&1 &
SERVER_PID=$!

# Save PID
echo "$SERVER_PID" > "$PID_FILE"

# Wait a moment to check if it started successfully
sleep 2

if kill -0 "$SERVER_PID" 2>/dev/null; then
    echo -e "${GREEN}✓ DevOps MCP Server started successfully!${NC}"
    echo "  PID: $SERVER_PID"
    echo "  Port: $PORT"
    echo "  Logs: $LOG_FILE"
    echo ""
    echo "To view logs: tail -f $LOG_FILE"
    echo "To stop server: kill $SERVER_PID (or: kill \$(cat $PID_FILE))"
    echo ""
    echo "Server endpoint: http://localhost:$PORT/mcp"
    echo "Ktor server will connect via: http://host.docker.internal:$PORT"
else
    echo -e "${RED}✗ Failed to start server${NC}"
    echo "Check logs at: $LOG_FILE"
    rm -f "$PID_FILE"
    exit 1
fi
