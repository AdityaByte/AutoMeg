package com.aditya.automeg.ai

import android.content.Context
import android.util.Log
import com.aditya.automeg.BuildConfig
import com.aditya.automeg.memory.MemoryStore
import com.aditya.automeg.memory.Message
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class ReplyEngine(private val context: Context) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private val gson = Gson()
    private val memoryStore = MemoryStore(context)
    
    // API key is now securely pulled from BuildConfig (loaded from local.properties)
    private val apiKey = BuildConfig.GROQ_API_KEY

    /**
     * Prepares a reply by calling Groq API with conversation context.
     */
    fun prepareReply(sender: String, text: String, packageName: String): String? {
        if (apiKey.isNullOrEmpty()) {
            Log.e("ReplyEngine", "GROQ_API_KEY is missing in local.properties")
            return null
        }

        val conversationId = "${packageName}_${sender}".replace(" ", "_")
        val conversation = memoryStore.readConversation(conversationId)
        
        // Take last 10 messages for context to maintain conversation flow
        val history = conversation.messages.takeLast(10)
        
        val messagesArray = JsonArray()
        
        // 1. System Prompt to define behavior
        messagesArray.add(JsonObject().apply {
            addProperty("role", "system")
            addProperty("content", "You are AutoMeg, a helpful AI assistant. Keep replies concise and natural. Do not use excessive emojis.")
        })

        // 2. Add Conversation History
        history.forEach { msg ->
            messagesArray.add(JsonObject().apply {
                addProperty("role", if (msg.direction == "incoming") "user" else "assistant")
                addProperty("content", msg.text)
            })
        }

        // 3. Add Current Message (if not already in history)
        if (history.none { it.text == text }) {
            messagesArray.add(JsonObject().apply {
                addProperty("role", "user")
                addProperty("content", text)
            })
        }

        val requestBody = JsonObject().apply {
            addProperty("model", "llama-3.3-70b-versatile")
            add("messages", messagesArray)
            addProperty("temperature", 0.7)
        }

        val body = requestBody.toString().toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("https://api.groq.com/openai/v1/chat/completions")
            .post(body)
            .addHeader("Authorization", "Bearer $apiKey")
            .build()

        return try {
            val response = client.newCall(request).execute()
            val responseData = response.body?.string()
            
            if (response.isSuccessful && responseData != null) {
                val jsonResponse = gson.fromJson(responseData, JsonObject::class.java)
                val reply = jsonResponse.getAsJsonArray("choices")
                    .get(0).asJsonObject
                    .getAsJsonObject("message")
                    .get("content").asString
                
                val cleanedReply = reply.trim().removeSurrounding("\"")
                if (cleanedReply.isNotEmpty()) {
                    saveOutgoingToMemory(sender, cleanedReply, packageName)
                    cleanedReply
                } else null
            } else {
                Log.e("ReplyEngine", "API Error: ${response.code}")
                null
            }
        } catch (e: Exception) {
            Log.e("ReplyEngine", "Network Error", e)
            null
        }
    }

    private fun saveOutgoingToMemory(sender: String, text: String, packageName: String) {
        val conversationId = "${packageName}_${sender}".replace(" ", "_")
        val conversation = memoryStore.readConversation(conversationId)
        
        val newMessage = Message(
            sender = "AutoMeg",
            text = text,
            timestamp = System.currentTimeMillis(),
            direction = "outgoing"
        )
        
        conversation.messages.add(newMessage)
        memoryStore.saveConversation(conversation)
    }
}
