package com.aditya.automeg.notification

import android.app.Notification
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.aditya.automeg.memory.MemoryManager
import com.aditya.automeg.core.AgentController
import com.aditya.automeg.executor.ExecutorEngine
import com.aditya.automeg.ai.ReplyEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class NotificationService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    private var memoryManager: MemoryManager? = null
    private var agentController: AgentController? = null
    private var executorEngine: ExecutorEngine? = null
    private var replyEngine: ReplyEngine? = null

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d("NotificationListener", "Notification Service Connected")
        initializeEngines()
    }

    private fun initializeEngines() {
        if (memoryManager == null) memoryManager = MemoryManager(this)
        if (agentController == null) agentController = AgentController(this)
        if (executorEngine == null) executorEngine = ExecutorEngine(this)
        if (replyEngine == null) replyEngine = ReplyEngine(this)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        
        val packageName = sbn?.packageName ?: return
        if (packageName == applicationContext.packageName) return

        val sharedPrefs = getSharedPreferences("automeg_prefs", Context.MODE_PRIVATE)
        val isAgentOn = sharedPrefs.getBoolean("agent_enabled", false)
        val isAppEnabled = sharedPrefs.getBoolean(packageName, false)

        if (!isAgentOn || !isAppEnabled) return

        val extras = sbn.notification.extras
        
        val isSummary = (sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0
        if (isSummary) return

        val title = extras.getString(Notification.EXTRA_TITLE) ?: 
                    extras.getString("android.title") ?: "Unknown"

        val messages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            extras.get(Notification.EXTRA_MESSAGES) as? Array<*>
        } else {
            null
        }
        
        if (messages != null) {
            val lastMessageObj = messages.lastOrNull()
            if (lastMessageObj is Bundle) {
                val msgText = lastMessageObj.getCharSequence("text")?.toString() ?: ""
                val msgSender = lastMessageObj.getCharSequence("sender")?.toString() ?: title
                if (msgText.isNotEmpty()) {
                    processIncomingMessage(sbn, msgSender, msgText, packageName)
                }
            }
        } else {
            val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: 
                       extras.getCharSequence("android.text")?.toString() ?: ""
            if (text.isNotEmpty()) {
                processIncomingMessage(sbn, title, text, packageName)
            }
        }
    }

    private fun processIncomingMessage(sbn: StatusBarNotification, sender: String, text: String, packageName: String) {
        initializeEngines()

        // 1. MEMORY MANAGER: Save to Memory
        memoryManager?.saveToMemory(sender, text, packageName)
        
        // 2. AGENT CONTROLLER: Decide if we should trigger the agent
        if (agentController?.shouldAgentRespond(text) == true) {
            
            // 3. Start a coroutine for the API call to Groq
            serviceScope.launch {
                Log.d("AutoMeg", "Requesting Groq reply for $sender: $text")
                
                // 4. REPLY ENGINE: Prepare the reply (now with API call and history)
                val generatedReply = replyEngine?.prepareReply(sender, text, packageName)
                
                if (generatedReply != null) {
                    // 5. EXECUTOR ENGINE: Send the reply
                    val success = executorEngine?.sendReply(sbn, generatedReply) ?: false
                    
                    if (success) {
                        agentController?.markResponseSent(generatedReply)
                        Log.d("AutoMeg", "SUCCESS: Groq reply sent to $sender")
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Cancel coroutines if service is destroyed
    }
}
