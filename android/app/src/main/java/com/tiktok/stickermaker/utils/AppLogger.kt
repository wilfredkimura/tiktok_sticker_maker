package com.tiktok.stickermaker.utils

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AppLogger {
    private val _logs = mutableStateListOf<LogEntry>()
    val logs: List<LogEntry> get() = _logs

    private val dateFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())

    fun log(message: String, level: LogLevel = LogLevel.INFO, tag: String? = null) {
        val stackTrace = Thread.currentThread().stackTrace
        // Index 3 or 4 usually contains the caller info
        val caller = stackTrace.getOrNull(4)
        val source = if (caller != null) {
            "${caller.fileName}:${caller.lineNumber}"
        } else null

        val entry = LogEntry(
            timestamp = dateFormat.format(Date()),
            message = message,
            level = level,
            source = source,
            tag = tag
        )
        _logs.add(0, entry)
        if (_logs.size > 500) _logs.removeLast()
        println("${entry.timestamp} [${entry.level}] ${entry.tag ?: ""}: ${entry.message} (${entry.source})")
    }

    fun clear() {
        _logs.clear()
    }

    fun getFullLogs(): String {
        return _logs.joinToString("\n") { "[${it.timestamp}] ${it.level} ${it.tag ?: ""}: ${it.message} ${it.source?.let { "($it)" } ?: ""}" }
    }
}

data class LogEntry(
    val timestamp: String,
    val message: String,
    val level: LogLevel,
    val source: String? = null,
    val tag: String? = null
)

enum class LogLevel {
    INFO, ERROR, DEBUG
}
