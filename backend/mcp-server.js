import { Server } from "@modelcontextprotocol/sdk/server/index.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import {
  CallToolRequestSchema,
  ListToolsRequestSchema,
} from "@modelcontextprotocol/sdk/types.js";

class MCPDemoServer {
  constructor() {
    this.server = new Server(
      {
        name: "ai-challenge-mcp-server",
        version: "1.0.0",
      },
      {
        capabilities: {
          tools: {},
        },
      }
    );

    this.setupToolHandlers();

    this.server.onerror = (error) => console.error("[MCP Error]", error);
    process.on("SIGINT", async () => {
      await this.server.close();
      process.exit(0);
    });
  }

  setupToolHandlers() {
    this.server.setRequestHandler(ListToolsRequestSchema, async () => ({
      tools: [
        {
          name: "get_current_time",
          description: "Returns the current time in ISO format",
          inputSchema: {
            type: "object",
            properties: {},
          },
        },
        {
          name: "calculate",
          description: "Performs basic arithmetic operations (add, subtract, multiply, divide)",
          inputSchema: {
            type: "object",
            properties: {
              operation: {
                type: "string",
                enum: ["add", "subtract", "multiply", "divide"],
                description: "The arithmetic operation to perform",
              },
              a: {
                type: "number",
                description: "First number",
              },
              b: {
                type: "number",
                description: "Second number",
              },
            },
            required: ["operation", "a", "b"],
          },
        },
        {
          name: "get_weather",
          description: "Gets mock weather data for a city",
          inputSchema: {
            type: "object",
            properties: {
              city: {
                type: "string",
                description: "Name of the city",
              },
            },
            required: ["city"],
          },
        },
        {
          name: "echo",
          description: "Echoes back the provided message",
          inputSchema: {
            type: "object",
            properties: {
              message: {
                type: "string",
                description: "Message to echo",
              },
            },
            required: ["message"],
          },
        },
      ],
    }));

    this.server.setRequestHandler(CallToolRequestSchema, async (request) => {
      const { name, arguments: args } = request.params;

      switch (name) {
        case "get_current_time":
          return {
            content: [
              {
                type: "text",
                text: new Date().toISOString(),
              },
            ],
          };

        case "calculate": {
          const { operation, a, b } = args;
          let result;

          switch (operation) {
            case "add":
              result = a + b;
              break;
            case "subtract":
              result = a - b;
              break;
            case "multiply":
              result = a * b;
              break;
            case "divide":
              if (b === 0) {
                throw new Error("Division by zero");
              }
              result = a / b;
              break;
            default:
              throw new Error(`Unknown operation: ${operation}`);
          }

          return {
            content: [
              {
                type: "text",
                text: `Result: ${result}`,
              },
            ],
          };
        }

        case "get_weather": {
          const { city } = args;
          const mockWeather = {
            temperature: Math.floor(Math.random() * 30) + 10,
            condition: ["sunny", "cloudy", "rainy", "windy"][Math.floor(Math.random() * 4)],
            humidity: Math.floor(Math.random() * 50) + 30,
          };

          return {
            content: [
              {
                type: "text",
                text: JSON.stringify({
                  city,
                  ...mockWeather,
                }, null, 2),
              },
            ],
          };
        }

        case "echo": {
          const { message } = args;
          return {
            content: [
              {
                type: "text",
                text: `Echo: ${message}`,
              },
            ],
          };
        }

        default:
          throw new Error(`Unknown tool: ${name}`);
      }
    });
  }

  async run() {
    const transport = new StdioServerTransport();
    await this.server.connect(transport);
    console.error("AI Challenge MCP Server running on stdio");
  }
}

const server = new MCPDemoServer();
server.run().catch(console.error);
