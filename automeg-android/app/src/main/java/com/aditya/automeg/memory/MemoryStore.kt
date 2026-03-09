package com.aditya.automeg.memory

import android.content.Context
import com.google.gson.Gson
import java.io.File
import com.aditya.automeg.log.LogType
import com.aditya.automeg.log.SystemLog

class MemoryStore(private val context: Context) {
    private val gson = Gson()

    fun saveConversation(conversation: ConversationMemory) {
        try {
            val fileName = "conv_${conversation.conversationId.replace(" ", "_")}.json"
            val file = File(context.filesDir, fileName)
            val json = gson.toJson(conversation)
            file.writeText(json)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun readConversation(conversationId: String): ConversationMemory {
        try {
            val fileName = "conv_${conversationId.replace(" ", "_")}.json"
            val file = File(context.filesDir, fileName)
            return if (file.exists()) {
                val json = file.readText()
                gson.fromJson(json, ConversationMemory::class.java)
            } else {
                ConversationMemory(conversationId, mutableListOf())
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return ConversationMemory(conversationId, mutableListOf())
        }
    }

    fun getAllConversations(): List<ConversationMemory> {
        val files = context.filesDir.listFiles { _, name -> name.startsWith("conv_") && name.endsWith(".json") }
        return files?.mapNotNull { file ->
            try {
                val json = file.readText()
                gson.fromJson(json, ConversationMemory::class.java)
            } catch (e: Exception) {
                null
            }
        } ?: emptyList()
    }

    fun clearAllConversations() {
        val files = context.filesDir.listFiles { _, name -> name.startsWith("conv_") && name.endsWith(".json") }
        files?.forEach { it.delete() }
    }

    // --- System Log Methods ---

    fun addSystemLog(message: String, type: LogType = LogType.INFO) {
        val logs = getSystemLogs().toMutableList()
        logs.add(0, SystemLog(message, System.currentTimeMillis(), type))
        if (logs.size > 100) logs.removeAt(logs.size - 1) // Keep last 100 logs
        
        try {
            val file = File(context.filesDir, "system_logs.json")
            val json = gson.toJson(logs)
            file.writeText(json)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getSystemLogs(): List<SystemLog> {
        return try {
            val file = File(context.filesDir, "system_logs.json")
            if (file.exists()) {
                val json = file.readText()
                gson.fromJson(json, Array<SystemLog>::class.java).toList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun clearSystemLogs() {
        val file = File(context.filesDir, "system_logs.json")
        if (file.exists()) file.delete()
    }
}
