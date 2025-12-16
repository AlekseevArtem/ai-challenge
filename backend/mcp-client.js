import { Client } from "@modelcontextprotocol/sdk/client/index.js";
import { StdioClientTransport } from "@modelcontextprotocol/sdk/client/stdio.js";
import { spawn } from "child_process";

class MCPDemoClient {
  constructor() {
    this.client = null;
  }

  async connect() {
    console.log("🚀 Starting MCP Client...\n");

    const serverProcess = spawn("node", ["mcp-server.js"], {
      cwd: process.cwd(),
      stdio: ["pipe", "pipe", "pipe"],
    });

    serverProcess.stderr.on("data", (data) => {
      console.log(`[Server Log] ${data.toString().trim()}`);
    });

    const transport = new StdioClientTransport({
      command: "node",
      args: ["mcp-server.js"],
    });

    this.client = new Client(
      {
        name: "ai-challenge-mcp-client",
        version: "1.0.0",
      },
      {
        capabilities: {},
      }
    );

    await this.client.connect(transport);
    console.log("✅ Successfully connected to MCP server\n");
  }

  async listTools() {
    if (!this.client) {
      throw new Error("Client not connected. Call connect() first.");
    }

    console.log("📋 Fetching available tools from MCP server...\n");

    const response = await this.client.listTools();

    console.log("=" .repeat(60));
    console.log("AVAILABLE MCP TOOLS");
    console.log("=" .repeat(60));
    console.log();

    if (response.tools && response.tools.length > 0) {
      response.tools.forEach((tool, index) => {
        console.log(`${index + 1}. ${tool.name}`);
        console.log(`   Description: ${tool.description}`);
        console.log(`   Input Schema:`);
        console.log(`   ${JSON.stringify(tool.inputSchema, null, 6).split('\n').join('\n   ')}`);
        console.log();
      });

      console.log("=" .repeat(60));
      console.log(`Total tools available: ${response.tools.length}`);
      console.log("=" .repeat(60));
    } else {
      console.log("No tools available.");
    }

    return response.tools;
  }

  async callTool(name, args) {
    if (!this.client) {
      throw new Error("Client not connected. Call connect() first.");
    }

    console.log(`\n🔧 Calling tool: ${name}`);
    console.log(`   Arguments: ${JSON.stringify(args)}`);

    const result = await this.client.callTool({ name, arguments: args });

    console.log(`   Result: ${JSON.stringify(result, null, 2)}`);

    return result;
  }

  async demonstrateTools() {
    console.log("\n" + "=".repeat(60));
    console.log("DEMONSTRATING TOOL CALLS");
    console.log("=".repeat(60));

    await this.callTool("get_current_time", {});

    await this.callTool("calculate", {
      operation: "add",
      a: 15,
      b: 27,
    });

    await this.callTool("echo", {
      message: "Hello from AI Challenge MCP Client!",
    });

    await this.callTool("get_weather", {
      city: "Moscow",
    });

    console.log("\n" + "=".repeat(60));
  }

  async close() {
    if (this.client) {
      await this.client.close();
      console.log("\n👋 Client disconnected");
    }
  }
}

async function main() {
  const client = new MCPDemoClient();

  try {
    await client.connect();

    const tools = await client.listTools();

    await client.demonstrateTools();

    await client.close();

    process.exit(0);
  } catch (error) {
    console.error("❌ Error:", error.message);
    console.error(error.stack);
    process.exit(1);
  }
}

main();
