package ru.alekseev.mcp.devops

fun main(args: Array<String>) {
    val port = args.firstOrNull()?.toIntOrNull() ?: 8082

    System.err.println("DevOps MCP Server starting on port $port...")

    val toolProviders = listOf(
        DevOpsToolProvider()
    )

    val server = MCPServer(toolProviders)
    val httpServer = MCPHttpServer(server, port)

    System.err.println("DevOps MCP Server running on http://localhost:$port/mcp")
    System.err.println("Available tools: docker_ps, build_android_app, deploy_to_emulator")
    System.err.println("Health check: http://localhost:$port/health")

    httpServer.start()
}
