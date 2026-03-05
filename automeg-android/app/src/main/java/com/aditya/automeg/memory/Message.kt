package com.aditya.automeg.memory

// Creating a model for messages.
data class Message(
    val sender: String,
    val text: String,
    val timestamp: Long,
    val direction: String // Incoming or outgoing.
)