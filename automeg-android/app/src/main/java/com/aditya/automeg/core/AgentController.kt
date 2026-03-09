package com.aditya.automeg.core

import android.app.ActivityManager
import android.content.Context
import android.os.PowerManager
import android.util.Log

class AgentController(private val context: Context) {

    private val sharedPrefs = context.getSharedPreferences("automeg_prefs", Context.MODE_PRIVATE)
    private val COOLDOWN_MILLIS = 2_000L // 2 second cooldown.

    companion object {
        private val sentMessagesCache = mutableSetOf<Int>()
    }

    private fun initState(): AgentState {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

        // Checking the screen is on.
        val screenOn = powerManager.isInteractive

        // Checking the app is in foreground or not.
        var appInForeground = false
        val runningAppProcesses = activityManager.runningAppProcesses
        if (runningAppProcesses != null) {
            val packageName = context.packageName
            for (processInfo in runningAppProcesses) {
                if (processInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND &&
                    processInfo.processName == packageName) {
                    appInForeground = true
                    break
                }
            }
        }

        val lastResponseTime = sharedPrefs.getLong("last_agent_response_time", 0L)
        val isCooldownActive = (System.currentTimeMillis() - lastResponseTime) < COOLDOWN_MILLIS

        return AgentState(
            screenOn = screenOn,
            appInForeground = appInForeground,
            cooldownActive = isCooldownActive
        )
    }

    /**
     * Decision engine to determine if the agent should reply to a specific message.
     * @param incomingText The text of the message received.
     */
    fun shouldAgentRespond(incomingText: String): Boolean {
        // 1. Loop Prevention: Check if this message is one we just sent
        if (sentMessagesCache.contains(incomingText.trim().hashCode())) {
            Log.d("AgentController", "Loop detected: Ignoring our own echo notification.")
            return false
        }

        val state = initState()
        Log.d("AgentController", "State: ScreenOn=${state.screenOn}, Foreground=${state.appInForeground}, Cooldown=${state.cooldownActive}")

        // Logic: For testing, we allow even if foreground or screen on, but respect cooldown
        if (state.cooldownActive) return false

        // Uncomment in production:
         if (state.appInForeground) return false

        return true
    }

    /**
     * Call this whenever the Agent actually sends a reply.
     * Starts the cooldown and caches the message hash to prevent loops.
     */
    fun markResponseSent(responseText: String) {
        // Cache the hash of the sent message
        sentMessagesCache.add(responseText.trim().hashCode())
        
        // Keep cache size manageable
        if (sentMessagesCache.size > 20) {
            sentMessagesCache.remove(sentMessagesCache.first())
        }

        sharedPrefs.edit().putLong("last_agent_response_time", System.currentTimeMillis()).apply()
        Log.d("AgentController", "Agent response marked. Cooldown started.")
    }
}
