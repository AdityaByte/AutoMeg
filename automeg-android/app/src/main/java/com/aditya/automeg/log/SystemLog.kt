package com.aditya.automeg.log

data class SystemLog (
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val type: LogType = LogType.INFO
)