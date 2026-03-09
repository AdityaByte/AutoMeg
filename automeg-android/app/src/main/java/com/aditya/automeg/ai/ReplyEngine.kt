package com.aditya.automeg.ai

import android.content.Context

class ReplyEngine(private val context: Context) {

    /**
     * Prepares a reply based on the input text.
     * Currently implemented with simple testing logic.
     * Returns the reply string if a reply is warranted, null otherwise.
     */
    fun prepareReply(text: String): String? {
        val normalizedText = text.lowercase().trim()
        
        return if (normalizedText in arrayOf<String>("hi", "hello", "hey")) {
            "AutoMeg: Hello! How can I assist you today?"
        } else {
            // For now, only reply to greetings during testing
            null
        }
    }
}
