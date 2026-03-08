package com.aditya.automeg.core

import android.app.ActivityManager
import android.content.Context
import android.os.PowerManager
import android.util.Log

class AgentController(private val context: Context) {

    private val sharedPrefs = context.getSharedPreferences("automeg_prefs", Context.MODE_PRIVATE)
    private val COOLDOWN_MILLIS = 30_000L // 30 seconds cooldown

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

    fun shouldAgentRespond(): Boolean {
        val state = initState()

        Log.d("AgentController", "State: ScreenOn=${state.screenOn}, Foreground=${state.appInForeground}, Cooldown=${state.cooldownActive}")

        // Logic: Only Respond when the user is not using the phone and not in cooldown
        // TODO: In Production uncomment this
        // if (state.screenOn) return false // Enabled for production, disabled for testing if needed
        // if (state.appInForeground) return false
        // if (state.cooldownActive) return false

        return true // So in each and every case it will return true for dev.
    }

    /**
     * Call this whenever the Agent actually sends a reply to start the cooldown timer.
     */
    fun markResponseSent() {
        sharedPrefs.edit().putLong("last_agent_response_time", System.currentTimeMillis()).apply()
        Log.d("AgentController", "Agent response marked. Cooldown started.")
    }
}
