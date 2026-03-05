package com.aditya.automeg.memory

import android.content.Context
import com.google.gson.Gson
import java.io.File

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
}
