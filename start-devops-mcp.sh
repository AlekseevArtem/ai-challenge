#!/bin/bash

# Start DevOps MCP Server
# This script starts the DevOps MCP server on your Mac to enable
# Claude to control Docker, build Android apps, and deploy to emulator

set -e

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${GREEN}=== DevOps MCP Server Startup ===${NC}\n"

# Check if JAR exists
JAR_PATH="mcp-server-devops/build/libs/mcp-server-devops-1.0.0.jar"
if [ ! -f "$JAR_PATH" ]; then
    echo -e "${YELLOW}DevOps MCP server JAR not found. Building...${NC}"
    ./gradlew :mcp-server-devops:jar
    echo ""
fi

# Check if port 8082 is available
if lsof -Pi :8082 -sTCP:LISTEN -t >/dev/null 2>&1; then
    echo -e "${RED}ERROR: Port 8082 is already in use${NC}"
    echo "Kill the process using: kill \$(lsof -t -i:8082)"
    exit 1
fi

# Check if Docker is running
if ! docker ps >/dev/null 2>&1; then
    echo -e "${RED}ERROR: Docker is not running${NC}"
    echo "Please start Docker Desktop"
    exit 1
fi

# Check if adb is available
if ! command -v adb &> /dev/null; then
    echo -e "${YELLOW}WARNING: adb not found in PATH${NC}"
    echo "Make sure Android SDK is installed and ANDROID_HOME is set"
    echo ""
fi

echo -e "${GREEN}Starting DevOps MCP Server on port 8082...${NC}"
echo -e "${YELLOW}Press Ctrl+C to stop${NC}\n"
echo "Available tools:"
echo "  - docker_ps: List running Docker containers"
echo "  - build_android_app: Build the Android app"
echo "  - deploy_to_emulator: Deploy and launch on emulator"
echo "  - list_directory: List files and directories"
echo "  - read_file: Read text file contents"
echo ""
echo "Ktor server will connect via: http://host.docker.internal:8082"
echo ""

# Start the server (port can be overridden with first argument)
PORT=${1:-8082}
java -jar "$JAR_PATH" $PORT
