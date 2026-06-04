package com.bicy.novel.util

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.File
import java.io.FileWriter
import java.io.InputStreamReader
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppLogger @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "NovelEditor"
        private const val LOG_DIR = "log"
        private const val APP_LOG_FILE = "app.log"
        private const val CRASH_LOG_FILE = "crash.log"
        private const val MAX_LOG_SIZE_MB = 5
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var logcatJob: Job? = null
    private var logcatProcess: Process? = null

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())

    @Volatile
    var isLoggingEnabled: Boolean = true

    private val logDir: File
        get() = File(context.getExternalFilesDir(null), LOG_DIR).also { dir ->
            if (!dir.exists()) dir.mkdirs()
        }

    private val appLogFile: File
        get() = File(logDir, APP_LOG_FILE)

    private val crashLogFile: File
        get() = File(logDir, CRASH_LOG_FILE)

    fun startLogcatCapture() {
        if (logcatJob?.isActive == true) return
        if (!isLoggingEnabled) return

        logcatJob = scope.launch {
            try {
                val process = Runtime.getRuntime().exec(
                    arrayOf("logcat", "-v", "threadtime")
                )
                logcatProcess = process
                val reader = BufferedReader(InputStreamReader(process.inputStream))

                reader.useLines { lines ->
                    lines.forEach { line ->
                        if (!isLoggingEnabled) return@forEach

                        appLogFile.also { file ->
                            try {
                                rotateFileIfNeeded(file)
                                FileWriter(file, true).use { it.append(line).append("\n") }
                            } catch (e: Exception) {
                                Log.e(TAG, "Failed to write logcat line", e)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Logcat capture failed", e)
            }
        }
    }

    fun stopLogcatCapture() {
        logcatJob?.cancel()
        logcatJob = null
        logcatProcess?.destroy()
        logcatProcess = null
    }

    fun restartLogcatCapture() {
        stopLogcatCapture()
        if (isLoggingEnabled) {
            startLogcatCapture()
        }
    }

    fun crash(throwable: Throwable) {
        val now = dateFormat.format(Date())
        val entry = buildString {
            append("$now [CRASH] App Crash\n")
            StringWriter().use { sw ->
                PrintWriter(sw).use { pw ->
                    throwable.printStackTrace(pw)
                }
                append(sw.toString())
            }
            append("\n")
        }

        Log.e(TAG, "App Crash", throwable)

        try {
            rotateFileIfNeeded(crashLogFile)
            FileWriter(crashLogFile, true).use { it.append(entry) }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write crash log", e)
        }
    }

    suspend fun getAppLogContent(): String = withContext(Dispatchers.IO) {
        readFileContent(appLogFile)
    }

    suspend fun getCrashLogContent(): String = withContext(Dispatchers.IO) {
        readFileContent(crashLogFile)
    }

    suspend fun getAllLogFiles(): List<File> = withContext(Dispatchers.IO) {
        logDir.listFiles()?.filter { it.isFile && it.name.endsWith(".log") }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
    }

    suspend fun readLogFile(file: File): String = withContext(Dispatchers.IO) {
        readFileContent(file)
    }

    suspend fun clearAppLog() = withContext(Dispatchers.IO) {
        stopLogcatCapture()
        if (appLogFile.exists()) appLogFile.delete()
        if (isLoggingEnabled) startLogcatCapture()
    }

    suspend fun clearCrashLog() = withContext(Dispatchers.IO) {
        if (crashLogFile.exists()) crashLogFile.delete()
    }

    suspend fun clearAllLogs() = withContext(Dispatchers.IO) {
        stopLogcatCapture()
        logDir.listFiles()?.forEach { it.delete() }
    }
    
    suspend fun getTotalLogSizeMb(): Double = withContext(Dispatchers.IO) {
        val files = logDir.listFiles() ?: return@withContext 0.0
        val totalBytes = files.filter { it.isFile && it.name.endsWith(".log") }.sumOf { it.length() }
        totalBytes / (1024.0 * 1024.0)
    }

    private fun readFileContent(file: File): String {
        if (!file.exists()) return ""
        return try {
            file.readText()
        } catch (e: Exception) {
            "无法读取日志文件: ${e.message}"
        }
    }

    private fun rotateFileIfNeeded(file: File) {
        if (file.exists() && file.length() > MAX_LOG_SIZE_MB * 1024 * 1024) {
            val rotated = File(file.parent, "${file.name}.1")
            if (rotated.exists()) rotated.delete()
            file.renameTo(rotated)
        }
    }
}
