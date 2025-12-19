package ru.alekseev.mcp.devops

import kotlinx.serialization.json.*
import ru.alekseev.mcp.devops.models.Tool
import java.io.File

/**
 * MCP Tool Provider for DevOps operations:
 * - Docker container management
 * - Android app building
 * - Emulator deployment
 */
class DevOpsToolProvider(
    private val projectRoot: String = System.getProperty("user.dir")
) : MCPToolProvider {

    override fun getTools(): List<Tool> = listOf(
        Tool(
            name = "docker_ps",
            description = "List all running Docker containers with their status, names, and ports",
            inputSchema = buildJsonObject {
                put("type", "object")
                putJsonObject("properties") { }
                putJsonArray("required") { }
            }
        ),
        Tool(
            name = "build_android_app",
            description = "Build the Android application using Gradle assembleDebug task. Returns the path to the generated APK file.",
            inputSchema = buildJsonObject {
                put("type", "object")
                putJsonObject("properties") {
                    putJsonObject("clean") {
                        put("type", "boolean")
                        put("description", "Whether to run clean before build (default: false)")
                    }
                }
                putJsonArray("required") { }
            }
        ),
        Tool(
            name = "deploy_to_emulator",
            description = "Install and launch the Android app on a running emulator. Requires an APK file path and checks that an emulator is running.",
            inputSchema = buildJsonObject {
                put("type", "object")
                putJsonObject("properties") {
                    putJsonObject("apk_path") {
                        put("type", "string")
                        put("description", "Path to the APK file to install")
                    }
                    putJsonObject("package_name") {
                        put("type", "string")
                        put("description", "Package name to launch (default: ru.alekseev.myapplication)")
                    }
                }
                putJsonArray("required") {
                    add("apk_path")
                }
            }
        )
    )

    override fun handleToolCall(toolName: String, arguments: JsonObject?): String {
        return when (toolName) {
            "docker_ps" -> handleDockerPs()
            "build_android_app" -> handleBuildAndroidApp(arguments)
            "deploy_to_emulator" -> handleDeployToEmulator(arguments)
            else -> throw IllegalArgumentException("Unknown tool: $toolName")
        }
    }

    private fun handleDockerPs(): String {
        val result = executeCommand(
            listOf("docker", "ps", "--format", "table {{.ID}}\\t{{.Names}}\\t{{.Status}}\\t{{.Ports}}"),
            workingDir = File(projectRoot)
        )

        return if (result.exitCode == 0) {
            "Docker Containers:\n${result.output}"
        } else {
            throw Exception("Failed to list Docker containers: ${result.error}")
        }
    }

    private fun handleBuildAndroidApp(arguments: JsonObject?): String {
        val clean = arguments?.get("clean")?.jsonPrimitive?.content?.toBoolean() ?: false

        val tasks = mutableListOf<String>()
        if (clean) {
            tasks.add("clean")
        }
        tasks.add(":composeApp:assembleDebug")

        val command = mutableListOf("./gradlew")
        command.addAll(tasks)

        System.err.println("[DevOpsToolProvider] Running: ${command.joinToString(" ")}")

        val result = executeCommand(
            command,
            workingDir = File(projectRoot),
            timeout = 300_000 // 5 minutes for build
        )

        if (result.exitCode != 0) {
            throw Exception("Build failed with exit code ${result.exitCode}:\n${result.error}")
        }

        // Find the generated APK
        val apkPath = File(projectRoot, "composeApp/build/outputs/apk/debug").let { dir ->
            dir.listFiles()?.firstOrNull { it.extension == "apk" }?.absolutePath
        }

        return if (apkPath != null) {
            "Build successful!\nAPK location: $apkPath\n\nBuild output:\n${result.output.takeLast(500)}"
        } else {
            "Build completed but APK not found at expected location.\n\nBuild output:\n${result.output.takeLast(500)}"
        }
    }

    private fun handleDeployToEmulator(arguments: JsonObject?): String {
        val apkPath = arguments?.get("apk_path")?.jsonPrimitive?.content
            ?: throw IllegalArgumentException("apk_path is required")

        val packageName = arguments["package_name"]?.jsonPrimitive?.content
            ?: "ru.alekseev.myapplication"

        val apkFile = File(apkPath)
        if (!apkFile.exists()) {
            throw IllegalArgumentException("APK file not found: $apkPath")
        }

        // Check if emulator is running
        val devicesResult = executeCommand(listOf("adb", "devices"), File(projectRoot))
        if (devicesResult.exitCode != 0) {
            throw Exception("Failed to check for running emulators: ${devicesResult.error}")
        }

        val runningDevices = devicesResult.output.lines()
            .drop(1) // Skip "List of devices attached"
            .filter { it.contains("device") && !it.contains("offline") }

        if (runningDevices.isEmpty()) {
            throw Exception("No running emulator found. Please start an Android emulator first.\n\nADB output:\n${devicesResult.output}")
        }

        System.err.println("[DevOpsToolProvider] Found ${runningDevices.size} device(s)")

        // Install APK
        val installResult = executeCommand(
            listOf("adb", "install", "-r", apkFile.absolutePath),
            workingDir = File(projectRoot),
            timeout = 60_000 // 1 minute for install
        )

        if (installResult.exitCode != 0) {
            throw Exception("Failed to install APK: ${installResult.error}")
        }

        // Launch the app
        val launchResult = executeCommand(
            listOf("adb", "shell", "am", "start", "-n", "$packageName/.MainActivity"),
            workingDir = File(projectRoot)
        )

        if (launchResult.exitCode != 0) {
            return "APK installed successfully, but failed to launch app: ${launchResult.error}\n\nYou may need to manually launch the app or adjust the package/activity name."
        }

        return "Successfully deployed to emulator!\n\nInstall output:\n${installResult.output}\n\nLaunch output:\n${launchResult.output}"
    }

    /**
     * Execute a shell command and return the result
     */
    private fun executeCommand(
        command: List<String>,
        workingDir: File,
        timeout: Long = 30_000
    ): CommandResult {
        val processBuilder = ProcessBuilder(command)
            .directory(workingDir)
            .redirectErrorStream(false)

        val process = processBuilder.start()

        val output = process.inputStream.bufferedReader().readText()
        val error = process.errorStream.bufferedReader().readText()

        val completed = process.waitFor(timeout, java.util.concurrent.TimeUnit.MILLISECONDS)

        if (!completed) {
            process.destroyForcibly()
            throw Exception("Command timed out after ${timeout}ms: ${command.joinToString(" ")}")
        }

        return CommandResult(
            exitCode = process.exitValue(),
            output = output,
            error = error
        )
    }

    private data class CommandResult(
        val exitCode: Int,
        val output: String,
        val error: String
    )
}
