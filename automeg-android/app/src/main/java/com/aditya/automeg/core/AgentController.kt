package com.aditya.automeg.core

import android.content.Context
import android.os.PowerManager

// This mainly handles the core logic about is the agent has to response or not.

// Creating a data-class which has some booleans symbols.
data class AgentState(
    val userOnline: Boolean, // Right now we are not focussing on this
    val screenOn: Boolean, // We are mainly focussing on this if the screen the on dont reply.
    val appInForeground: Boolean, // No focus
    val cooldownActive: Boolean // No focus
)

class AgentController(private val context: Context) {

    private fun initState(): AgentState {
        val powerManager =
            context.getSystemService(Context.POWER_SERVICE)
            as PowerManager

        val screenOn = powerManager.isInteractive
        return AgentState(
            userOnline = false,
            screenOn = screenOn,
            appInForeground = false,
            cooldownActive = false
        )
    }

    // For every call we have to lookup at the state of the user device and return that state
    // To this function.
    fun shouldAgentRespond() : Boolean {
        val state = initState()

        if (state.screenOn) return false
        if (state.cooldownActive) return false

        return true
    }
}