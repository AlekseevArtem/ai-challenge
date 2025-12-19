# Day 15: MCP DevOps Integration

This document describes how to set up and use the DevOps MCP server that allows Claude (via your Ktor server) to control Docker, build Android apps, and deploy to an emulator.

## Overview

The DevOps MCP server is a **separate module** (`mcp-server-devops`) that runs on your Mac host machine. This is different from the main MCP server (`mcp-server`) which handles Calendar and Reminders via stdio.

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│  Mac Host                                                    │
│                                                              │
│  ┌──────────────────────┐      ┌─────────────────────────┐ │
│  │ DevOps MCP Server    │◄─────┤ Android SDK/Emulator    │ │
│  │ (Port 8082)          │      │ Docker Engine           │ │
│  │                      │      │ Gradle Build            │ │
│  └──────────┬───────────┘      └─────────────────────────┘ │
│             │ HTTP                                          │
│             │                                               │
│  ┌──────────▼───────────────────────────────────────────┐  │
│  │ Docker Container: myapplication-server               │  │
│  │                                                       │  │
│  │  ┌─────────────────────────────────────────────┐    │  │
│  │  │ Ktor Server (Port 8080)                     │    │  │
│  │  │  - MCPManager with MCPHttpClient            │    │  │
│  │  │  - Connects to host.docker.internal:8082    │    │  │
│  │  └─────────────────────────────────────────────┘    │  │
│  └──────────────────────────────────────────────────────┘  │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

## Prerequisites

1. **Android SDK**: Installed on Mac with `ANDROID_HOME` set
2. **Android Emulator**: An AVD created and accessible via `adb`
3. **Docker**: Running on Mac with Docker socket accessible
4. **MCP Server JAR**: Built with `./gradlew :mcp-server:jar`

## Setup Instructions

### Step 1: Build the DevOps MCP Server

```bash
./gradlew :mcp-server-devops:jar
```

This creates: `mcp-server-devops/build/libs/mcp-server-devops-1.0.0.jar`

### Step 2: Start the DevOps MCP Server on Mac

You have two options:

#### Option A: Foreground (recommended for testing)

```bash
./start-devops-mcp.sh
```

Or manually:
```bash
java -jar mcp-server-devops/build/libs/mcp-server-devops-1.0.0.jar 8082
```

#### Option B: Background (recommended for daily use)

```bash
./start-devops-mcp-background.sh
```

This starts the server as a daemon. To stop it later:
```bash
./stop-devops-mcp.sh
```

You should see:
```
DevOps MCP Server running on http://localhost:8082/mcp
Available tools: docker_ps, build_android_app, deploy_to_emulator
Health check: http://localhost:8082/health
```

### Step 3: Start an Android Emulator

In another terminal:

```bash
# List available AVDs
emulator -list-avds

# Start an emulator (replace with your AVD name)
emulator -avd Pixel_5_API_33 &

# Verify it's running
adb devices
```

### Step 4: Start Your Ktor Server (in Docker)

```bash
docker-compose up server
```

Check the logs for:
```
Registering DevOps MCP HTTP client at http://host.docker.internal:8082
Registered DevOps MCP HTTP client (will connect when server starts)
```

### Step 5: Connect to Your Application

Open your web application at http://localhost:8080 and start chatting with Claude.

## Available MCP Tools

### 1. `docker_ps`
Lists all running Docker containers.

**Example prompt to Claude:**
```
"Show me which Docker containers are currently running"
```

**Tool Input:** None

**Tool Output:**
```
Docker Containers:
CONTAINER ID   NAMES                   STATUS         PORTS
abc123def456   myapplication-server   Up 2 hours     0.0.0.0:8081->8080/tcp
```

### 2. `build_android_app`
Builds the Android application using Gradle assembleDebug.

**Example prompt to Claude:**
```
"Build the Android app for me"
"Build the app with a clean build"
```

**Tool Input:**
- `clean` (optional, boolean): Whether to run clean before build (default: false)

**Tool Output:**
```
Build successful!
APK location: /Users/artemalekseev/AndroidStudioProjects/MyApplication4/composeApp/build/outputs/apk/debug/composeApp-debug.apk

Build output:
BUILD SUCCESSFUL in 1m 23s
42 actionable tasks: 42 executed
```

### 3. `deploy_to_emulator`
Installs the APK on a running emulator and launches the app.

**Example prompt to Claude:**
```
"Deploy the app to the emulator"
"Install the APK at /path/to/app.apk on the emulator"
```

**Tool Input:**
- `apk_path` (required, string): Path to the APK file to install
- `package_name` (optional, string): Package name to launch (default: ru.alekseev.myapplication)

**Tool Output:**
```
Successfully deployed to emulator!

Install output:
Performing Streamed Install
Success

Launch output:
Starting: Intent { cmp=ru.alekseev.myapplication/.MainActivity }
```

## Complete Workflow Example

Here's a complete conversation you can have with Claude:

```
User: "I want to verify my app is working on a real device. Can you help me?"

Claude: [Uses docker_ps to check containers are running]

User: "Build the app and deploy it to my emulator"

Claude: [Uses build_android_app to build the APK]
Claude: [Uses deploy_to_emulator with the APK path from the build]

User: "Great! The app is running on my emulator"
```

## Troubleshooting

### DevOps MCP Server won't start
- Check port 8082 is not already in use: `lsof -i :8082`
- Verify JAR exists: `ls -la mcp-server/build/libs/mcp-server-1.0.0.jar`

### Ktor server can't connect to DevOps MCP
- Verify DevOps MCP server is running on Mac
- Check Docker can reach host: `docker exec myapplication-server curl http://host.docker.internal:8082/health`
- Check logs in Ktor server for connection errors

### Build fails
- Verify you're in the correct directory (project root)
- Check `./gradlew` is executable: `chmod +x gradlew`
- Try building manually: `./gradlew :composeApp:assembleDebug`

### Emulator not found
- Check emulator is running: `adb devices`
- Verify ADB is in PATH: `which adb`
- Start ADB server: `adb start-server`

### Deploy fails with "MainActivity not found"
- Check package name in build.gradle.kts: `namespace = "ru.alekseev.myapplication"`
- Verify MainActivity exists in composeApp/src/androidMain
- Try launching manually: `adb shell am start -n ru.alekseev.myapplication/.MainActivity`

## Testing the Integration

### Manual Test
1. Start DevOps MCP server
2. Start emulator
3. Start Ktor server in Docker
4. In your web UI, ask Claude: "List running Docker containers"
5. Ask: "Build the Android app"
6. Ask: "Deploy it to the emulator"

### Verification
- Docker containers listed correctly
- APK builds successfully
- APK installs on emulator
- App launches on emulator

## Technical Details

### HTTP Communication
The Ktor server (in Docker) connects to the DevOps MCP server (on Mac host) via HTTP:
- Endpoint: `http://host.docker.internal:8082/mcp`
- Protocol: JSON-RPC 2.0 over HTTP POST
- Health check: `http://host.docker.internal:8082/health`

### Process Execution
The DevOps MCP server executes commands on the Mac host:
- Docker: `docker ps`
- Gradle: `./gradlew :composeApp:assembleDebug`
- ADB: `adb install -r <apk>` and `adb shell am start`

### Security Considerations
- DevOps MCP server runs with your user permissions
- Can execute arbitrary commands (docker, gradle, adb)
- Only expose on localhost (127.0.0.1:8082)
- Use authentication if exposing to network

## Next Steps

1. **Add more tools**: Screenshots, logcat, app restart
2. **CI/CD Integration**: Automate testing after deployment
3. **Multiple devices**: Support deploying to multiple emulators
4. **Real device support**: Deploy to physical devices via USB
5. **Test execution**: Run instrumented tests after deployment

## Module Structure

This project now has **two separate MCP server modules**:

### 1. `mcp-server` (Calendar & Reminders)
- **Purpose**: Google Calendar and Reminder tools
- **Connection**: stdio (launched by Ktor server via ProcessBuilder)
- **Location**: Runs inside Docker container
- **Tools**: calendar_*, reminder_*

### 2. `mcp-server-devops` (DevOps Tools)
- **Purpose**: Docker, Gradle builds, Android deployment
- **Connection**: HTTP (connects to host via `host.docker.internal:8082`)
- **Location**: Runs on Mac host machine
- **Tools**: docker_ps, build_android_app, deploy_to_emulator

## File References

### DevOps Module (`mcp-server-devops/`)
- Main entry: `src/main/kotlin/ru/alekseev/mcp/devops/Main.kt`
- DevOps Tools: `src/main/kotlin/ru/alekseev/mcp/devops/DevOpsToolProvider.kt`
- HTTP Server: `src/main/kotlin/ru/alekseev/mcp/devops/MCPHttpServer.kt`
- MCP Server: `src/main/kotlin/ru/alekseev/mcp/devops/MCPServer.kt`

### Server Integration
- HTTP Client: `server/src/main/kotlin/ru/alekseev/myapplication/service/MCPHttpClient.kt`
- Registration: `server/src/main/kotlin/ru/alekseev/myapplication/di/AppModule.kt:147-160`

### Startup Scripts
- Foreground: `start-devops-mcp.sh`
- Background: `start-devops-mcp-background.sh`
- Stop daemon: `stop-devops-mcp.sh`
