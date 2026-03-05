package com.aditya.automeg.notification

import android.app.Notification
import android.content.Context
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.aditya.automeg.memory.MemoryManager

class NotificationService : NotificationListenerService() {

    private var memoryManager: MemoryManager? = null

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d("NotificationListener", "Notification Service Connected")
        if (memoryManager == null) {
            memoryManager = MemoryManager(this)
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        
        val packageName = sbn?.packageName ?: return
        val sharedPrefs = getSharedPreferences("automeg_prefs", Context.MODE_PRIVATE)
        val isAgentOn = sharedPrefs.getBoolean("agent_enabled", false)
        val isAppEnabled = sharedPrefs.getBoolean(packageName, false)

        if (!isAgentOn || !isAppEnabled) return

        val extras = sbn.notification.extras
        val title = extras.getString("android.title") ?: "Unknown"

        // Handle MessagingStyle notifications (like WhatsApp/Telegram)
        // These often contain a history of messages in the notification itself
        val messages = extras.get(Notification.EXTRA_MESSAGES) as? Array<*>
        
        if (messages != null) {
            Log.d("NotificationListener", "Processing multi-message notification (${messages.size} items)")
            for (messageObj in messages) {
                if (messageObj is Bundle) {
                    val msgText = messageObj.getCharSequence("text")?.toString() ?: ""
                    val msgSender = messageObj.getCharSequence("sender")?.toString() ?: title
                    
                    if (msgText.isNotEmpty()) {
                        saveMessage(msgSender, msgText, packageName)
                    }
                }
            }
        } else {
            // Fallback for simple notifications
            val text = extras.getCharSequence("android.text")?.toString() ?: ""
            if (text.isNotEmpty()) {
                saveMessage(title, text, packageName)
            }
        }
    }

    private fun saveMessage(sender: String, text: String, packageName: String) {
        if (memoryManager == null) {
            memoryManager = MemoryManager(this)
        }
        
        // The MemoryManager should handle duplicates (e.g., if we've already saved this exact text/timestamp)
        memoryManager?.saveToMemory(
            sender = sender,
            text = text,
            packageName = packageName
        )
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
    }
}
