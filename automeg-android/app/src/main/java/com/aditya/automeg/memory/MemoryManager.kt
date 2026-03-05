package com.aditya.automeg.memory

import android.content.Context

class MemoryManager(private val context: Context) {
    private val memoryStore = MemoryStore(context)

    fun saveToMemory(sender: String, text: String, packageName: String) {
        val conversationId = "${packageName}_${sender}".replace(" ", "_")
        val conversation = memoryStore.readConversation(conversationId)
        
        // Prevent duplicates: Check if the exact same message was just saved
        // This is important because notifications often repeat old messages
        val isDuplicate = conversation.messages.any { 
            it.text == text && (System.currentTimeMillis() - it.timestamp) < 2000 
        }

        if (!isDuplicate) {
            val newMessage = Message(
                sender = sender,
                text = text,
                timestamp = System.currentTimeMillis(),
                direction = "incoming"
            )
            
            conversation.messages.add(newMessage)
            memoryStore.saveConversation(conversation)
        }
    }
}
