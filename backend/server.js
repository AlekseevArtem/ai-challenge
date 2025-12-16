import express from "express";
import cors from "cors";
import { Client } from "@modelcontextprotocol/sdk/client/index.js";
import { StdioClientTransport } from "@modelcontextprotocol/sdk/client/stdio.js";

const app = express();
const PORT = process.env.PORT || 3000;

app.use(cors());
app.use(express.json());

let mcpClient = null;
let mcpTools = [];

async function initializeMCP() {
  try {
    console.log("🚀 Initializing MCP Client...");

    const transport = new StdioClientTransport({
      command: "node",
      args: ["mcp-server.js"],
    });

    mcpClient = new Client(
      {
        name: "ai-challenge-http-client",
        version: "1.0.0",
      },
      {
        capabilities: {},
      }
    );

    await mcpClient.connect(transport);
    console.log("✅ MCP Client connected successfully");

    const response = await mcpClient.listTools();
    mcpTools = response.tools || [];

    console.log(`📋 Loaded ${mcpTools.length} MCP tools:`);
    mcpTools.forEach((tool) => {
      console.log(`   - ${tool.name}: ${tool.description}`);
    });
  } catch (error) {
    console.error("❌ Failed to initialize MCP:", error.message);
    throw error;
  }
}

app.get("/", (req, res) => {
  res.json({
    message: "AI Challenge - MCP Integration Server",
    status: "running",
    endpoints: {
      tools: "GET /api/mcp/tools - List all available MCP tools",
      call: "POST /api/mcp/call - Call an MCP tool",
    },
  });
});

app.get("/api/mcp/tools", async (req, res) => {
  try {
    if (!mcpClient) {
      return res.status(503).json({
        error: "MCP client not initialized",
      });
    }

    const response = await mcpClient.listTools();

    res.json({
      success: true,
      tools: response.tools,
      count: response.tools?.length || 0,
    });
  } catch (error) {
    console.error("Error listing tools:", error);
    res.status(500).json({
      success: false,
      error: error.message,
    });
  }
});

app.post("/api/mcp/call", async (req, res) => {
  try {
    if (!mcpClient) {
      return res.status(503).json({
        error: "MCP client not initialized",
      });
    }

    const { name, arguments: args } = req.body;

    if (!name) {
      return res.status(400).json({
        success: false,
        error: "Tool name is required",
      });
    }

    const result = await mcpClient.callTool({
      name,
      arguments: args || {},
    });

    res.json({
      success: true,
      tool: name,
      result,
    });
  } catch (error) {
    console.error("Error calling tool:", error);
    res.status(500).json({
      success: false,
      error: error.message,
    });
  }
});

app.get("/health", (req, res) => {
  res.json({
    status: "healthy",
    mcp: mcpClient ? "connected" : "disconnected",
    toolsCount: mcpTools.length,
  });
});

async function startServer() {
  try {
    await initializeMCP();

    app.listen(PORT, () => {
      console.log();
      console.log("=".repeat(60));
      console.log(`🌐 Server running on http://localhost:${PORT}`);
      console.log("=".repeat(60));
      console.log();
      console.log("Available endpoints:");
      console.log(`  GET  http://localhost:${PORT}/`);
      console.log(`  GET  http://localhost:${PORT}/api/mcp/tools`);
      console.log(`  POST http://localhost:${PORT}/api/mcp/call`);
      console.log(`  GET  http://localhost:${PORT}/health`);
      console.log();
      console.log("=".repeat(60));
    });
  } catch (error) {
    console.error("Failed to start server:", error);
    process.exit(1);
  }
}

process.on("SIGINT", async () => {
  console.log("\n🛑 Shutting down...");
  if (mcpClient) {
    await mcpClient.close();
  }
  process.exit(0);
});

startServer();
