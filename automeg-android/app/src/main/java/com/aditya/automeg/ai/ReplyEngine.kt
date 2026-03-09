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
    private val sharedPrefs = context.getSharedPreferences("automeg_prefs", Context.MODE_PRIVATE)
    
    // API key is pulled from BuildConfig (loaded from local.properties)
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
        
        // Retrieve User Identity provided in the UI
        val userIdentity = sharedPrefs.getString("user_identity", "No specific identity provided.") ?: ""
        
        // Take last 10 messages for context
        val history = conversation.messages.takeLast(10)
        
        val messagesArray = JsonArray()
        
        // System Prompt with User Identity Integration
        val systemPrompt = """
        You are AutoMeg, an autonomous messaging agent replying on behalf of the user.
        
        Platform: $packageName
        Recipient: $sender
        
        USER IDENTITY (this describes the person you are impersonating):
        $userIdentity
        
        Your task is to respond exactly like the user would in a normal chat conversation.
        
        Behavior Rules:
        1. Write replies as if the user personally typed them.
        2. Use a casual, natural texting style unless the conversation clearly requires formality.
        3. Keep responses short (1–2 sentences maximum).
        4. Avoid sounding like an AI assistant, bot, or customer support agent.
        5. Do not introduce yourself as AutoMeg or mention automation.
        6. Use the user's personality, tone, and preferences from the User Identity when replying.
        7. If the message is simple (greeting, acknowledgement, etc.), reply briefly like a real person would.
        8. Avoid overly structured sentences or long explanations.
        9. Only use emojis if they fit the user's personality and the context.
        10. If unsure what the user would say, give a neutral but polite reply.
        
        Conversation Context:
        Respond only to the latest message while considering the tone of the conversation.
        
        Output only the reply text. Do not include explanations.
        """.trimIndent()
        messagesArray.add(JsonObject().apply {
            addProperty("role", "system")
            addProperty("content", systemPrompt)
        })

        // Add Conversation History
        history.forEach { msg ->
            messagesArray.add(JsonObject().apply {
                addProperty("role", if (msg.direction == "incoming") "user" else "assistant")
                addProperty("content", msg.text)
            })
        }

        // Current message
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
