package com.aditya.automeg.memory

// Conversion Memory for keeping all the chats isolated.
data class ConversationMemory (
    val conversationId: String,
    val messages: MutableList<Message>
)