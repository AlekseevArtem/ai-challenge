#!/bin/bash

# Script to authorize Google Calendar for MCP server
# This needs to be run locally before deploying to Docker

set -e

echo "🔐 Authorizing Google Calendar for MCP Server..."
echo ""

# Check if credentials file exists
if [ ! -f "server/google-calendar-credentials.json" ]; then
    echo "❌ Error: server/google-calendar-credentials.json not found"
    echo "Please download OAuth 2.0 credentials from Google Cloud Console"
    echo "https://console.cloud.google.com/apis/credentials"
    exit 1
fi

# Build MCP server if not already built
if [ ! -f "mcp-server/build/libs/mcp-server-1.0.0.jar" ]; then
    echo "📦 Building MCP server..."
    ./gradlew :mcp-server:jar --quiet
    echo ""
fi

# Create tokens directory
mkdir -p server/tokens

# Clear any existing invalid tokens
rm -f server/tokens/StoredCredential

# Set environment variables (do NOT set MCP_HEADLESS)
export GOOGLE_CALENDAR_CREDENTIALS_PATH="$(pwd)/server/google-calendar-credentials.json"
export GOOGLE_CALENDAR_TOKENS_PATH="$(pwd)/server/tokens"

echo "🚀 Starting MCP server for OAuth authorization..."
echo "A browser window will open for you to authorize the application."
echo ""
echo "Follow these steps:"
echo "  1. A browser will open automatically"
echo "  2. Sign in with your Google account"
echo "  3. Grant calendar access permissions"
echo "  4. Wait for the 'Authorization complete' message"
echo "  5. Return here - the script will verify the tokens"
echo ""
read -p "Press ENTER to start the authorization process..."
echo ""

# Create a temporary script to run the MCP server and trigger auth
cat > /tmp/mcp-auth-trigger.sh << 'EOF'
#!/bin/bash
sleep 3
echo '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{}}'
sleep 1
echo '{"jsonrpc":"2.0","id":2,"method":"tools/list","params":null}'
sleep 1
echo '{"jsonrpc":"2.0","id":3,"method":"tools/call","params":{"name":"list_events","arguments":{"maxResults":1}}}'
sleep 2
EOF
chmod +x /tmp/mcp-auth-trigger.sh

# Run MCP server with input from the trigger script
echo "🌐 Opening browser for authorization..."
/tmp/mcp-auth-trigger.sh | java -jar mcp-server/build/libs/mcp-server-1.0.0.jar 2>&1 &
MCP_PID=$!

# Wait for tokens to be created
echo "⏳ Waiting for authorization (timeout: 2 minutes)..."
TIMEOUT=120
ELAPSED=0
LAST_SIZE=0

while [ $ELAPSED -lt $TIMEOUT ]; do
    # Check if StoredCredential exists and has content
    if [ -f "server/tokens/StoredCredential" ]; then
        FILE_SIZE=$(wc -c < "server/tokens/StoredCredential" 2>/dev/null || echo 0)
        if [ "$FILE_SIZE" -gt 100 ]; then
            echo ""
            echo "✅ Authorization successful! Tokens saved to server/tokens/"
            echo "📊 Token file size: $FILE_SIZE bytes"

            # Give it a moment to finish writing
            sleep 1

            # Kill the MCP server
            kill $MCP_PID 2>/dev/null || true
            wait $MCP_PID 2>/dev/null || true

            # Cleanup
            rm -f /tmp/mcp-auth-trigger.sh

            echo ""
            echo "✨ Next steps:"
            echo "   1. Rebuild the Docker container:"
            echo "      docker-compose build server"
            echo "   2. Restart the services:"
            echo "      docker-compose up -d"
            echo ""
            exit 0
        elif [ "$FILE_SIZE" -gt "$LAST_SIZE" ]; then
            echo -n "📝"
            LAST_SIZE=$FILE_SIZE
        fi
    fi

    # Check if process is still running
    if ! kill -0 $MCP_PID 2>/dev/null; then
        echo ""
        echo "⚠️  MCP server process ended. Checking if authorization completed..."
        if [ -f "server/tokens/StoredCredential" ]; then
            FILE_SIZE=$(wc -c < "server/tokens/StoredCredential")
            if [ "$FILE_SIZE" -gt 100 ]; then
                echo "✅ Authorization successful!"
                rm -f /tmp/mcp-auth-trigger.sh
                exit 0
            fi
        fi
        echo "❌ Authorization may have failed. Please try again."
        rm -f /tmp/mcp-auth-trigger.sh
        exit 1
    fi

    sleep 2
    ELAPSED=$((ELAPSED + 2))
    echo -n "."
done

echo ""
echo "❌ Timeout waiting for authorization"
kill $MCP_PID 2>/dev/null || true
rm -f /tmp/mcp-auth-trigger.sh
exit 1
