package com.aditya.automeg.executor

import android.app.Notification
import android.app.RemoteInput
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.service.notification.StatusBarNotification
import android.util.Log

class ExecutorEngine(private val context: Context) {

    /**
     * Sends a reply to a specific notification using its RemoteInput actions.
     */
    fun sendReply(sbn: StatusBarNotification, replyText: String): Boolean {
        val notification = sbn.notification
        val actions = notification.actions ?: return false

        // Look for an action that has a RemoteInput (usually the "Reply" action)
        for (action in actions) {
            val remoteInputs = action.remoteInputs ?: continue
            
            for (remoteInput in remoteInputs) {
                // We found the reply action!
                Log.d("ExecutorEngine", "Found Reply action for: ${sbn.packageName}")
                
                try {
                    val intent = Intent()
                    val bundle = Bundle()
                    bundle.putCharSequence(remoteInput.resultKey, replyText)
                    RemoteInput.addResultsToIntent(arrayOf(remoteInput), intent, bundle)
                    
                    // Trigger the pending intent to send the message
                    action.actionIntent.send(context, 0, intent)
                    Log.d("ExecutorEngine", "Reply sent successfully to ${sbn.packageName}")
                    return true
                } catch (e: Exception) {
                    Log.e("ExecutorEngine", "Failed to send reply", e)
                }
            }
        }
        
        Log.w("ExecutorEngine", "No Reply action found for notification from ${sbn.packageName}")
        return false
    }
}
