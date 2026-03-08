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

class NotificationService : NotificationListenerService() {

    private var memoryManager: MemoryManager? = null
    private var agentController: AgentController? = null

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d("NotificationListener", "Notification Service Connected")
        if (memoryManager == null) memoryManager = MemoryManager(this)
        if (agentController == null) agentController = AgentController(this)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        
        val packageName = sbn?.packageName ?: return
        val sharedPrefs = getSharedPreferences("automeg_prefs", Context.MODE_PRIVATE)
        val isAgentOn = sharedPrefs.getBoolean("agent_enabled", false)
        val isAppEnabled = sharedPrefs.getBoolean(packageName, false)

        if (!isAgentOn || !isAppEnabled) return

        val extras = sbn.notification.extras
        val title = extras.getString(Notification.EXTRA_TITLE) ?: 
                    extras.getString("android.title") ?: "Unknown"

        val messages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            extras.get(Notification.EXTRA_MESSAGES) as? Array<*>
        } else {
            null
        }
        
        if (messages != null) {
            for (messageObj in messages) {
                if (messageObj is Bundle) {
                    val msgText = messageObj.getCharSequence("text")?.toString() ?: ""
                    val msgSender = messageObj.getCharSequence("sender")?.toString() ?: title
                    if (msgText.isNotEmpty()) {
                        processIncomingMessage(msgSender, msgText, packageName)
                    }
                }
            }
        } else {
            val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: 
                       extras.getCharSequence("android.text")?.toString() ?: ""
            if (text.isNotEmpty()) {
                processIncomingMessage(title, text, packageName)
            }
        }
    }

    private fun processIncomingMessage(sender: String, text: String, packageName: String) {
        if (memoryManager == null) memoryManager = MemoryManager(this)
        if (agentController == null) agentController = AgentController(this)

        // 1. Save to Memory
        memoryManager?.saveToMemory(sender, text, packageName)
        
        // 2. Decide if we should trigger the agent
        if (agentController?.shouldAgentRespond() == true) {
            Log.d("AutoMeg", "AGENT TRIGGERED: Preparing to reply to $sender")
            
            // TODO: Call AI Service
            // agentController?.markResponseSent() // Call this after the AI actually sends the message
        } else {
            Log.d("AutoMeg", "AGENT SKIP: Conditions not met (User active or Cooldown)")
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
    }
}
