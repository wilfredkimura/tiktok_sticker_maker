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

    fun log(message: String, level: LogLevel = LogLevel.INFO) {
        val entry = LogEntry(
            timestamp = dateFormat.format(Date()),
            message = message,
            level = level
        )
        _logs.add(0, entry) // Add to top
        if (_logs.size > 200) _logs.removeLast() // Keep last 200 logs
        println("${entry.timestamp} [${entry.level}] ${entry.message}")
    }

    fun clear() {
        _logs.clear()
    }

    fun getFullLogs(): String {
        return _logs.joinToString("\n") { "[${it.timestamp}] ${it.level}: ${it.message}" }
    }
}

data class LogEntry(
    val timestamp: String,
    val message: String,
    val level: LogLevel
)

enum class LogLevel {
    INFO, ERROR, DEBUG
}
