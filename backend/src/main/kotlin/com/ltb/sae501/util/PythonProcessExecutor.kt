package com.ltb.sae501.util

import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

data class ProcessResult(
    val exitCode: Int,
    val stdout: String,
    val stderr: String,
    val timedOut: Boolean
)

class PythonProcessExecutor(
    private val pythonExecutable: String,
    private val workingDirectory: String
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun execute(
        scriptName: String,
        args: List<String>,
        timeoutMs: Long,
        progressCallback: ((String) -> Unit)? = null
    ): ProcessResult {
        val workingDir = File(workingDirectory).absoluteFile

        if (!workingDir.exists()) {
            throw IllegalStateException("Working directory does not exist: ${workingDir.absolutePath}")
        }

        val command = mutableListOf(pythonExecutable, scriptName)
        command.addAll(args)

        logger.info("Executing command: ${command.joinToString(" ")}")
        logger.info("Working directory: ${workingDir.absolutePath}")

        val processBuilder = ProcessBuilder(command)
        processBuilder.directory(workingDir)
        processBuilder.redirectErrorStream(false)

        val process = processBuilder.start()

        val stdoutBuilder = StringBuilder()
        val stderrBuilder = StringBuilder()

        val stdoutThread = Thread {
            try {
                BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                    reader.forEachLine { line ->
                        stdoutBuilder.appendLine(line)
                        progressCallback?.invoke(line)
                        logger.debug("Python stdout: $line")
                    }
                }
            } catch (e: Exception) {
                logger.error("Error reading stdout", e)
            }
        }

        val stderrThread = Thread {
            try {
                BufferedReader(InputStreamReader(process.errorStream)).use { reader ->
                    reader.forEachLine { line ->
                        stderrBuilder.appendLine(line)
                        logger.warn("Python stderr: $line")
                    }
                }
            } catch (e: Exception) {
                logger.error("Error reading stderr", e)
            }
        }

        stdoutThread.start()
        stderrThread.start()

        val completed = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS)

        if (!completed) {
            logger.error("Process timed out after ${timeoutMs}ms, destroying forcibly")
            process.destroy()
            if (!process.waitFor(5, TimeUnit.SECONDS)) {
                process.destroyForcibly()
            }

            stdoutThread.join(1000)
            stderrThread.join(1000)

            return ProcessResult(
                exitCode = -1,
                stdout = stdoutBuilder.toString(),
                stderr = stderrBuilder.toString(),
                timedOut = true
            )
        }

        stdoutThread.join()
        stderrThread.join()

        val exitCode = process.exitValue()
        logger.info("Process exited with code: $exitCode")

        return ProcessResult(
            exitCode = exitCode,
            stdout = stdoutBuilder.toString(),
            stderr = stderrBuilder.toString(),
            timedOut = false
        )
    }

    fun checkPythonAvailability(): Pair<Boolean, String> {
        return try {
            val processBuilder = ProcessBuilder(pythonExecutable, "--version")
            val process = processBuilder.start()

            val output = BufferedReader(InputStreamReader(process.inputStream)).use { it.readText() }
            val errorOutput = BufferedReader(InputStreamReader(process.errorStream)).use { it.readText() }

            val completed = process.waitFor(5, TimeUnit.SECONDS)

            if (!completed) {
                process.destroyForcibly()
                return Pair(false, "Timeout checking Python version")
            }

            val version = output.ifBlank { errorOutput }.trim()

            if (process.exitValue() == 0) {
                logger.info("Python available: $version")
                Pair(true, version)
            } else {
                logger.error("Python check failed: $version")
                Pair(false, version)
            }
        } catch (e: Exception) {
            logger.error("Error checking Python availability", e)
            Pair(false, e.message ?: "Unknown error")
        }
    }
}
