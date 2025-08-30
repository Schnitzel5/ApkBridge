package me.schnitzel.apkbridge

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

class AppViewModel : ViewModel() {
    var addressText by mutableStateOf("No info")
    var apkName = ""
    var updateResponse by mutableStateOf("")
    var updateText by mutableStateOf("")
    var showNewUpdate by mutableStateOf(false)
    var logLines = mutableStateListOf("[LOGGER]")

    fun log(message: String?, level: LogLevel = LogLevel.INFO) {
        val temp = when (level) {
            LogLevel.DEBUG -> "[DEBUG]"
            LogLevel.INFO -> "[INFO]"
            LogLevel.WARNING -> "[WARNING]"
            LogLevel.ERROR -> "[ERROR]"
        }
        logLines.add("$temp $message")
    }

    fun logs(): String {
        return logLines.toList().joinToString(separator = "\n")
    }
}

enum class LogLevel {
    DEBUG,
    INFO,
    WARNING,
    ERROR
}
